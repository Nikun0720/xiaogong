package com.nikun.xiaogong.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nikun.xiaogong.common.R;
import com.nikun.xiaogong.dto.DishDto;
import com.nikun.xiaogong.entity.Dish;
import com.nikun.xiaogong.entity.DishFlavor;
import com.nikun.xiaogong.entity.Setmeal;
import com.nikun.xiaogong.entity.SetmealDish;
import com.nikun.xiaogong.exception.CustomException;
import com.nikun.xiaogong.mapper.DishFlavorMapper;
import com.nikun.xiaogong.mapper.DishMapper;
import com.nikun.xiaogong.service.DishFlavorService;
import com.nikun.xiaogong.service.DishService;
import com.nikun.xiaogong.service.SetmealDishService;
import com.nikun.xiaogong.service.SetmealService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DishServiceImpl extends ServiceImpl<DishMapper, Dish> implements DishService {

    @Autowired
    private DishFlavorService dishFlavorService;

    @Autowired
    private DishFlavorMapper dishFlavorMapper;

    @Autowired
    private SetmealDishService setmealDishService;

    @Autowired
    private SetmealService setmealService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Value("${reggie.path}")
    private String basePath;

    /**
     * 新增菜品，同时保存对应的口味数据
     * @param dishDto
     */
    @Override
    @Transactional  //SpringBoot中默认自动开启
    public void saveWithFlavor(DishDto dishDto) {

        // 保存菜品的基本信息到菜品表dish
        this.save(dishDto);

        // 为DishFlavor中的dishId赋值
        Long dishId = dishDto.getId();
        List<DishFlavor> dishFlavors = dishDto.getFlavors();
        dishFlavors.forEach(item -> item.setDishId(dishId));

        // 保存菜品口味数据到菜品口味表dish_flavor
        dishFlavorService.saveBatch(dishFlavors);

        // 清理某个分类下面的菜品缓存数据
        String dishKey = "dish_" + dishDto.getCategoryId() + "_1";
        redisTemplate.delete(dishKey);

    }

    /**
     * 删除菜品，同时删除菜品对应的图片和口味数据
     * 如果菜品关联套餐，则不可删除
     * @param ids
     */
    @Override
    @Transactional
    public void remove(List<String> ids) {

        // 查询当前菜品是否关联套餐
        LambdaQueryWrapper<SetmealDish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(SetmealDish::getDishId, ids);
        int setmealCount = setmealDishService.count(queryWrapper);
        // 如果存在关联套餐则不可删除
        if(setmealCount > 0) {
            // 已经关联套餐，抛出一个业务异常
            throw new CustomException("当前选中的菜品关联了套餐，不能删除");
        }

        // 根据id对菜品对应的文件名进行查询
        LambdaQueryWrapper<Dish> queryWrapper2 = new LambdaQueryWrapper<>();
        queryWrapper2.select(Dish::getImage).in(Dish::getId, ids);
        // 需要通过菜品状态判断是否可以删除，起售中的菜品不可删除
        queryWrapper2.eq(Dish::getStatus, 0);
        List<Dish> list = this.list(queryWrapper2);

        // 判断是否均为停售菜品，如果存在非停售菜品，则不能删除，并抛出一个异常
        if(list.size() != ids.size()) {
            throw new CustomException("菜品正在售卖中，不能删除");
        }

        // 删除菜品对应的图片
        list.forEach(item -> {
            String fileName = item.getImage();
            File file = new File(basePath + fileName);
            if(file.exists()) {
                file.delete();
            }
        });

        // 删除dish表中的菜品
        this.removeByIds(ids);

        // 删除菜品口味表中对应菜品id的数据（用到了新写Mapper方法和Mapper对应的映射文件）
        // 别用这个方法，调用了好几次数据库
        //ids.forEach(item -> dishFlavorMapper.deleteByDishId(Long.parseLong(item)));

        // 如下方式也可以进行删除口味表，更简洁一些
        LambdaQueryWrapper<DishFlavor> queryWrapper3 = new LambdaQueryWrapper<>();
        queryWrapper3.in(DishFlavor::getDishId, ids);
        dishFlavorService.remove(queryWrapper3);
    }

    /**
     * 根据id查询菜品信息和对应的口味信息
     * @param dish
     * @return
     */
    @Override
    public DishDto getByDishWithFlavor(Dish dish) {

        // 查询当前菜品对应的口味信息，从dish_flavor表查询
        LambdaQueryWrapper<DishFlavor> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DishFlavor::getDishId, dish.getId());
        List<DishFlavor> flavors = dishFlavorService.list(queryWrapper);

        // 将查询出来的结果拷贝到dishDto上
        DishDto dishDto = new DishDto();
        BeanUtils.copyProperties(dish, dishDto);
        dishDto.setFlavors(flavors);

        return dishDto;
    }

    /**
     * 更新菜品信息，同时更新对应的口味信息
     * @param dishDto
     */
    @Override
    @Transactional
    public void updateWithFlavor(DishDto dishDto) {

        // 根据id对菜品对应的文件名进行查询
        Dish dish = this.getById(dishDto.getId());
        // 删除菜品对应的图片
        if(! dish.getImage().equals(dishDto.getImage())) {
            File file = new File(basePath + dish.getImage());
            if(file.exists()) {
                file.delete();
            }
        }

        // 更新dish表的基本信息
        this.updateById(dishDto);

        // 清理当前菜品对应的口味数据 -> dish_flavor表的delete操作
        LambdaQueryWrapper<DishFlavor> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DishFlavor::getDishId, dishDto.getId());
        // 执行删除
        dishFlavorService.remove(queryWrapper);

        // 添加当前提交过来的口味数据 -> dish_flavor表的insert操作
        List<DishFlavor> flavors = dishDto.getFlavors();
        // 将dishId添加进flavors中
        flavors.forEach(item -> item.setDishId(dishDto.getId()));
        // 执行插入
        dishFlavorService.saveBatch(flavors);

        // 清理某个分类下面的菜品缓存数据
        String dishKey = "dish_" + dishDto.getCategoryId() + "_1";
        redisTemplate.delete(dishKey);

    }

    /**
     * 菜品批量启用与停用（有对应的套餐就别禁用）
     * @param status
     * @param id
     * @return
     */
    @Override
    @Transactional
    public R<String> updateStatus(int status, List<String> id) {

        // 如果是停售，先查询有无对应的套餐仍在起售，如果有，则连带着套餐一起停售
        if(status == 0) {
            // 查询setmeal_dish表，得到setmealId
            LambdaQueryWrapper<SetmealDish> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.select(SetmealDish::getSetmealId).in(SetmealDish::getDishId, id);
            List<SetmealDish> setmealDishList = setmealDishService.list(queryWrapper);

            List<Long> setmealIdList = setmealDishList.stream().map(item -> {
                Long setmealId = item.getSetmealId();

                return setmealId;
            }).collect(Collectors.toList());

            // 如果存在对应套餐，根据setmealId查询setmeal表
            if(setmealDishList.size() > 0) {
                LambdaQueryWrapper<Setmeal> queryWrapper2 = new LambdaQueryWrapper<>();
                queryWrapper2.in(Setmeal::getId, setmealIdList).eq(Setmeal::getStatus, 1);
                List<Setmeal> setmealList = setmealService.list(queryWrapper2);

                // 如果有对应的套餐就把套餐停售
                if(setmealList.size() > 0) {

                    setmealList.forEach(item ->{
                        item.setStatus(0);
                    });

                    setmealService.updateBatchById(setmealList);
                }
            }
        }

        List<Dish> list = new ArrayList<>();
        id.forEach(item -> {
            Dish dish = new Dish();
            dish.setId(Long.parseLong(item));
            dish.setStatus(status);

            list.add(dish);
        });

        this.updateBatchById(list);

        // 清理所有菜品的缓存数据
        // 这里清楚所有菜品缓存是因为没有categoryId，想获得还得从数据库查，不如直接清理缓存
        Set keys = redisTemplate.keys("dish_*");
        Long delete = redisTemplate.delete(keys);

        String result = (status == 0) ? "菜品已停售" : "菜品已起售";

        return R.success(result);
    }
}
