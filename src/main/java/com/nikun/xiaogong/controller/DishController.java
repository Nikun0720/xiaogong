package com.nikun.xiaogong.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nikun.xiaogong.common.R;
import com.nikun.xiaogong.dto.DishDto;
import com.nikun.xiaogong.entity.Category;
import com.nikun.xiaogong.entity.Dish;
import com.nikun.xiaogong.service.CategoryService;
import com.nikun.xiaogong.service.DishService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 菜品管理
 */
@RestController
@RequestMapping("/dish")
@Slf4j
public class DishController {

    @Autowired
    private DishService dishService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 新增菜品
     * @param dishDto
     * @return
     */
    @PostMapping
    public R<String> save(@RequestBody DishDto dishDto) {

        log.info("dishDto:{}", dishDto.toString());

        dishService.saveWithFlavor(dishDto);

        return R.success("新增菜品成功");
    }

    /**
     * 菜品信息分页查询
     * @param page
     * @param pageSize
     * @param name
     * @return
     */
    @GetMapping("/page")
    public R<Page> page(int page, int pageSize, String name) {

        // 构造分页构造器
        Page<Dish> pageInfo = new Page<>(page, pageSize);
        Page<DishDto> dishDtoPage = new Page<>(page, pageSize);

        // 构造条件构造器
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        // 添加过滤条件
        queryWrapper.like(StringUtils.isNotEmpty(name), Dish::getName, name);
        // 添加排序条件
        queryWrapper.orderByDesc(Dish::getStatus).orderByAsc(Dish::getSort);

        // 执行分页查询
        dishService.page(pageInfo, queryWrapper);

        // 对象拷贝
        BeanUtils.copyProperties(pageInfo, dishDtoPage, "records");

        List<Dish> records = pageInfo.getRecords();
        List<DishDto> list = records.stream().map(item -> {
            DishDto dishDto = new DishDto();
            BeanUtils.copyProperties(item, dishDto);

            Long categoryId = item.getCategoryId();
            // 根据id查询分类对象
            Category category = categoryService.getById(categoryId);

            // 设置dishDto的categoryName
            if(category != null) {
                String categoryName = category.getName();
                dishDto.setCategoryName(categoryName);
            }


            return dishDto;
        }).collect(Collectors.toList());

        dishDtoPage.setRecords(list);

        return R.success(dishDtoPage);
    }

    /**
     * 根据id批量停售或起售菜品
     * @param status
     * @param id
     * @return
     */
    @PostMapping("/status/{status}")
    public R<String> updateStatus(@PathVariable("status") int status, @RequestParam("ids") List<String> id) {

        return dishService.updateStatus(status, id);
    }

    /**
     * 根据id批量删除菜品（如果菜品关联套餐，则不可删除）
     * @param ids
     * @return
     */
    @DeleteMapping
    public R<String> delete(String[] ids) {     // 也可以用 @RequestParam("ids") List<String> ids
        // 参数注意，如果要用List接收参数的话，则必须使用@RequestParam注解
        // 如果使用数组接收的话，则在名字对应上的情况下，不需要写这个注解

        // log.info("ids = {}", ids);

        dishService.remove(Arrays.asList(ids));

        return R.success("菜品已删除");
    }

    /**
     * 根据id查询菜品信息和对应的口味信息
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public R<DishDto> get(@PathVariable("id") Long id) {

        // 查询菜品基本信息，从dish表查询
        Dish dish = dishService.getById(id);

        DishDto dishDto = dishService.getByDishWithFlavor(dish);

        return R.success(dishDto);
    }

    /**
     * 修改菜品
     * @param dishDto
     * @return
     */
    @PutMapping
    public R<String> update(@RequestBody DishDto dishDto) {

        //log.info("dishDto:{}", dishDto.toString());

        dishService.updateWithFlavor(dishDto);

        return R.success("修改菜品成功");
    }

    /**
     * 根据条件查询对应的菜品数据
     * @param dish
     * @return
     */
    /*@GetMapping("/list")
    public R<List<Dish>> list(Dish dish) {
        // 这里传过来的实体类参数，只要里面属性里有对应请求参数的属性，就可以传过来这个实体类参数

        // 构造查询条件
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        // 根据菜品种类查询
        queryWrapper.eq(dish.getCategoryId() != null, Dish::getCategoryId, dish.getCategoryId());
        // 只查询状态为起售的菜品
        queryWrapper.eq(dish.getCategoryId() != null, Dish::getStatus, 1);
        // 添加排序条件
        queryWrapper.orderByAsc(Dish::getSort).orderByDesc(Dish::getUpdateTime);
        List<Dish> list = dishService.list(queryWrapper);

        return R.success(list);
    }*/
    @GetMapping("/list")
    public R<List<DishDto>> list(Dish dish) {
        // 这里传过来的实体类参数，只要里面属性里有对应请求参数的属性，就可以传过来这个实体类参数

        List<DishDto> dishDtoList = null;

        // 动态构造key
        String key = "dish_" + dish.getCategoryId() + "_" + dish.getStatus();    // 例如：dish_1397844263642378242_1

        // 先从redis中获取缓存数据
        dishDtoList  = (List<DishDto>) redisTemplate.opsForValue().get(key);

        // 如果存在，直接返回，无需查询数据库
        if(dishDtoList != null) {
            return R.success(dishDtoList);
        }

        // 如果不存在，需要查询数据库
        // 构造查询条件
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        // 根据菜品种类查询
        queryWrapper.eq(dish.getCategoryId() != null, Dish::getCategoryId, dish.getCategoryId());
        // 只查询状态为起售的菜品
        queryWrapper.eq(dish.getCategoryId() != null, Dish::getStatus, 1);
        // 添加排序条件
        queryWrapper.orderByAsc(Dish::getSort).orderByDesc(Dish::getUpdateTime);
        List<Dish> list = dishService.list(queryWrapper);

        dishDtoList = list.stream().map(item -> {
            DishDto dishDto = dishService.getByDishWithFlavor(item);

            // 根据分类id查询分类对象
            Long categoryId = item.getCategoryId();
            Category category = categoryService.getById(categoryId);

            // 设置dishDto的categoryName
            if(category != null) {
                String categoryName = category.getName();
                dishDto.setCategoryName(categoryName);
            }

            return dishDto;
        }).collect(Collectors.toList());

        // 将查询到的菜品数据缓存到redis
        redisTemplate.opsForValue().set(key, dishDtoList, 60, TimeUnit.MINUTES);

        return R.success(dishDtoList);
    }
}
