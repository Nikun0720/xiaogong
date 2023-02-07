package com.nikun.xiaogong.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nikun.xiaogong.common.R;
import com.nikun.xiaogong.dto.DishDto;
import com.nikun.xiaogong.dto.SetmealDto;
import com.nikun.xiaogong.entity.Category;
import com.nikun.xiaogong.entity.Dish;
import com.nikun.xiaogong.entity.Setmeal;
import com.nikun.xiaogong.entity.SetmealDish;
import com.nikun.xiaogong.service.CategoryService;
import com.nikun.xiaogong.service.DishService;
import com.nikun.xiaogong.service.SetmealDishService;
import com.nikun.xiaogong.service.SetmealService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 套餐管理
 */
@RestController
@RequestMapping("/setmeal")
@Slf4j
public class SetmealController {

    @Autowired
    private SetmealService setmealService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private SetmealDishService setmealDishService;

    @Autowired
    private DishService dishService;

    /**
     * 新增套餐
     * @param setmealDto
     * @return
     */
    @PostMapping
    public R<String> save(@RequestBody SetmealDto setmealDto) {

        //log.info("setmealDto:{}", setmealDto.toString());

        setmealService.saveWithDish(setmealDto);

        return R.success("新增套餐成功");
    }

    /**
     * 套餐信息分页查询
     * @param page
     * @param pageSize
     * @param name
     * @return
     */
    @GetMapping("/page")
    public R<Page> page(int page, int pageSize, String name) {

        // 构造分页构造器
        Page<Setmeal> pageInfo = new Page<>(page, pageSize);
        Page<SetmealDto> setmealDtoPage = new Page<>(page, pageSize);

        // 构造条件构造器
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        // 添加过滤条件
        queryWrapper.eq(StringUtils.isNotEmpty(name), Setmeal::getName, name);
        // 添加排序条件
        queryWrapper.orderByDesc(Setmeal::getStatus).orderByAsc(Setmeal::getUpdateTime);

        // 执行分页查询
        setmealService.page(pageInfo, queryWrapper);

        // 对象拷贝
        BeanUtils.copyProperties(pageInfo, setmealDtoPage, "records");

        List<Setmeal> records = pageInfo.getRecords();
        List<SetmealDto> list = records.stream().map(item -> {
            SetmealDto setmealDto = new SetmealDto();
            BeanUtils.copyProperties(item, setmealDto);

            Long categoryId = item.getCategoryId();
            // 根据categoryId查询分类对象
            Category category = categoryService.getById(categoryId);

            // 设置setmealDto的categoryName
            if(category != null) {
                String categoryName = category.getName();
                setmealDto.setCategoryName(categoryName);
            }

            return setmealDto;
        }).collect(Collectors.toList());

        setmealDtoPage.setRecords(list);

        return R.success(setmealDtoPage);

    }

    /**
     * 根据id批量停售或起售套餐（如果有对应菜品在停售，则套餐不能起售）
     * @param status
     * @param ids
     * @return
     */
    @PostMapping("/status/{status}")
    public R<String> updateStauts(@PathVariable("status") int status, Long[] ids) {

        return setmealService.updateStatus(status, ids);

    }

    /**
     * 删除套餐
     * @param ids
     * @return
     */
    @DeleteMapping
    public R<String> delete(Long[] ids) {
        setmealService.removeWithDish(Arrays.asList(ids));

        return R.success("套餐已删除");
    }

    /**
     * 根据id查询套餐信息和对应的菜品信息
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public R<SetmealDto> get(@PathVariable("id") Long id) {

        SetmealDto setmealDto = setmealService.getByIdWithDish(id);

        return R.success(setmealDto);

    }

    /**
     * 修改套餐
     * @param setmealDto
     * @return
     */
    @PutMapping
    public R<String> update(@RequestBody SetmealDto setmealDto) {

        setmealService.updateWithDish(setmealDto);

        return R.success("修改套餐成功");
    }

    /**
     * 根据categoryId查询对应的套餐信息
     * @param setmeal
     * @return
     */
    @GetMapping("/list")
    public R<List<Setmeal>> list(Setmeal setmeal) {

        // 创建条件构造器
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        // 添加查询条件
        queryWrapper.eq(setmeal.getCategoryId() != null, Setmeal::getCategoryId, setmeal.getCategoryId())
                .eq(Setmeal::getStatus, 1);
        // 添加排序条件
        queryWrapper.orderByDesc(Setmeal::getUpdateTime);
        // 执行查询
        List<Setmeal> list = setmealService.list(queryWrapper);

        return R.success(list);
    }

    @GetMapping("/dish/{id}")
    public R<List<DishDto>> dish(@PathVariable("id") Long setmealId) {

        // 根据套餐id查出套餐中的菜品集合（setmeal_dish表）
        LambdaQueryWrapper<SetmealDish> queryWrapper = new LambdaQueryWrapper<SetmealDish>();
        queryWrapper.eq(SetmealDish::getSetmealId, setmealId);
        // 执行查询
        List<SetmealDish> setmealDishList = setmealDishService.list(queryWrapper);

        List<DishDto> dishDtoList = setmealDishList.stream().map(item -> {
            DishDto dishDto = new DishDto();
            // 主要是为了将setmealDish中的份数拷贝过来
            BeanUtils.copyProperties(item, dishDto);

            // 根据dishId查询dish表中的对象
            Long dishId = item.getDishId();
            Dish dish = dishService.getById(dishId);

            // 将dish中的数据拷贝过来
            BeanUtils.copyProperties(dish, dishDto);

            return dishDto;

        }).collect(Collectors.toList());

        return R.success(dishDtoList);
    }

}
