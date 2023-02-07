package com.nikun.xiaogong.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nikun.xiaogong.common.R;
import com.nikun.xiaogong.dto.DishDto;
import com.nikun.xiaogong.entity.Dish;

import java.util.List;

public interface DishService extends IService<Dish> {

    /**
     * 新增菜品，同时插入菜品对应的口味数据，需要操作两张表：dish，dish_flavor
     * @param dishDto
     */
    void saveWithFlavor(DishDto dishDto);

    /**
     * 删除菜品，同时删除菜品对应的口味数据，需要操作两张表：dish，dish_flavor
     * 如果菜品关联套餐，则不可删除
     * @param ids
     */
    void remove(List<String> ids);

    /**
     * 根据id查询菜品信息和对应的口味信息
     * @param dish
     * @return
     */
    DishDto getByDishWithFlavor(Dish dish);

    /**
     * 更新菜品信息，同时更新对应的口味信息
     * @param dishDto
     */
    void updateWithFlavor(DishDto dishDto);

    /**
     * 菜品批量启用与停用（有对应的套餐在起售时，就连带着套餐一起停售）
     * @param status
     * @param id
     * @return
     */
    R<String> updateStatus(int status, List<String> id);

}
