package com.nikun.xiaogong.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nikun.xiaogong.common.R;
import com.nikun.xiaogong.dto.SetmealDto;
import com.nikun.xiaogong.entity.Dish;
import com.nikun.xiaogong.entity.Setmeal;
import com.nikun.xiaogong.entity.SetmealDish;
import com.nikun.xiaogong.exception.CustomException;
import com.nikun.xiaogong.mapper.SetmealMapper;
import com.nikun.xiaogong.service.DishService;
import com.nikun.xiaogong.service.SetmealDishService;
import com.nikun.xiaogong.service.SetmealService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SetmealServiceImpl extends ServiceImpl<SetmealMapper, Setmeal> implements SetmealService {

    @Autowired
    private SetmealDishService setmealDishService;

    @Autowired
    private DishService dishService;

    @Value("reggie.path")
    private String basePath;

    /**
     * 新增套餐，同时保存套餐和菜品的关联关系
     * @param setmealDto
     */
    @Override
    @Transactional
    public void saveWithDish(SetmealDto setmealDto) {

        // 保存套餐的基本信息，操作setmeal表，执行insert操作
        this.save(setmealDto);

        // 为SetmealDish中的套餐id（setmealId）赋值
        List<SetmealDish> setmealDishes = setmealDto.getSetmealDishes();
        setmealDishes.forEach(item -> item.setSetmealId(setmealDto.getId()));

        // 保存套餐和菜品的关联信息，操作setmeal_dish表，执行insert操作
        setmealDishService.saveBatch(setmealDishes);

    }

    /**
     * 删除套餐，同时删除套餐和菜品的关联关系
     * @param ids
     */
    @Override
    @Transactional
    public void removeWithDish(List<Long> ids) {

        // 根据id对套餐对应的文件名进行查询
        LambdaQueryWrapper<Setmeal> queryWrapper1 = new LambdaQueryWrapper<>();
        queryWrapper1.select(Setmeal::getImage).in(Setmeal::getId, ids);
        // 需要通过套餐状态判断是否可以删除，起售中的套餐不可删除
        queryWrapper1.eq(Setmeal::getStatus, 0);
        List<Setmeal> list = this.list(queryWrapper1);

        // 判断是否均为停售套餐，如果存在非停售套餐，则不能删除，并抛出一个异常
        if(list.size() != ids.size()) {
            throw new CustomException("套餐正在售卖中，不能删除");
        }

        // 删除套餐对应的图片
        list.forEach(item -> {
            String fileName = item.getImage();
            File file = new File(basePath + fileName);
            if(file.exists()) {
                file.delete();
            }
        });

        // 删除setmeal表中的套餐
        this.removeByIds(ids);

        // 删除setmeal_dish表中对应套餐id的数据
        LambdaQueryWrapper<SetmealDish> queryWrapper2 = new LambdaQueryWrapper<>();
        queryWrapper2.in(SetmealDish::getSetmealId, ids);
        setmealDishService.remove(queryWrapper2);

    }

    /**
     * 根据id查询套餐信息和对应的菜品信息
     * @param id
     * @return
     */
    @Override
    public SetmealDto getByIdWithDish(Long id) {

        // 查
        Setmeal setmeal = this.getById(id);

        // 查
        LambdaQueryWrapper<SetmealDish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SetmealDish::getSetmealId, id);
        List<SetmealDish> list = setmealDishService.list(queryWrapper);

        // 拷
        SetmealDto setmealDto = new SetmealDto();
        BeanUtils.copyProperties(setmeal, setmealDto);
        setmealDto.setSetmealDishes(list);

        return setmealDto;
    }

    /**
     * 更新套餐信息，同时更新对应的菜品信息
     * @param setmealDto
     */
    @Override
    @Transactional
    public void updateWithDish(SetmealDto setmealDto) {

        // 根据id对套餐对应的文件名进行查询
        Setmeal setmeal = this.getById(setmealDto);

        // 删除套餐对应的图片
        if(! setmeal.getImage().equals(setmealDto.getImage())) {
            File file = new File(basePath + setmeal.getImage());
            if(file.exists()) {
                file.delete();
            }
        }

        // 更新setmeal表的基本信息
        this.updateById(setmealDto);

        // 清理当前套餐对应的菜品数据 -> setmeal_dish表的delete操作
        LambdaQueryWrapper<SetmealDish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SetmealDish::getSetmealId, setmealDto.getId());
        // 执行删除
        setmealDishService.remove(queryWrapper);

        // 添加当前提交过来的菜品数据 -> setmeal_dish表的insert操作
        List<SetmealDish> dishes = setmealDto.getSetmealDishes();
        // 将setmealId添加进flavors中
        dishes.forEach(item -> item.setSetmealId(setmealDto.getId()));
        // 执行插入
        setmealDishService.saveBatch(dishes);
    }

    /**
     * 根据id批量停售或起售套餐（如果有对应菜品在停售，则套餐不能起售）
     * @param status
     * @param ids
     */
    @Override
    @Transactional
    public R<String> updateStatus(int status, Long[] ids) {

        // 如果是起售，先查询有无对应的菜品仍在停售，如果有，则套餐不能起售
        if(status == 1) {
            // 查询setmeal_dish表，得到dishId
            LambdaQueryWrapper<SetmealDish> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.select(SetmealDish::getDishId).in(SetmealDish::getSetmealId, ids);
            List<SetmealDish> setmealDishList = setmealDishService.list(queryWrapper);

            List<Long> dishIdList = setmealDishList.stream().map(item -> {
                Long dishId = item.getDishId();

                return dishId;
            }).collect(Collectors.toList());

            // 根据dishId查询dish表中是否存在仍在停售的对应菜品
            LambdaQueryWrapper<Dish> queryWrapper2 = new LambdaQueryWrapper<>();
            queryWrapper2.in(Dish::getId, dishIdList).eq(Dish::getStatus, 0);
            int count = dishService.count(queryWrapper2);

            // 如果有对应的菜品仍在停售，就返回错误信息
            if(count > 0) {
                return R.error("有对应菜品仍在停售，套餐起售失败");
            }

        }

        List<Setmeal> list = new ArrayList<>();
        for(Long id : ids) {
            Setmeal setmeal = new Setmeal();
            setmeal.setId(id);
            setmeal.setStatus(status);

            list.add(setmeal);
        }

        this.updateBatchById(list);

        String result = (status == 0) ? "套餐已停售" : "套餐已起售";
        return R.success(result);
    }
}
