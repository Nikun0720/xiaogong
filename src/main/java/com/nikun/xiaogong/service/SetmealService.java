package com.nikun.xiaogong.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nikun.xiaogong.common.R;
import com.nikun.xiaogong.dto.SetmealDto;
import com.nikun.xiaogong.entity.Setmeal;

import java.util.List;

public interface SetmealService extends IService<Setmeal> {

    /**
     * 新增套餐，同时保存套餐和菜品的关联关系
     * @param setmealDto
     */
    void saveWithDish(SetmealDto setmealDto);

    /**
     * 删除套餐，同时删除套餐和菜品的关联关系
     * @param ids
     */
    void removeWithDish(List<Long> ids);

    /**
     * 根据id查询套餐信息和对应的菜品信息
     * @param id
     * @return
     */
    SetmealDto getByIdWithDish(Long id);

    /**
     * 更新套餐信息，同时更新对应的菜品信息
     * @param setmealDto
     */
    void updateWithDish(SetmealDto setmealDto);

    /**
     * 根据id批量停售或起售套餐（如果有对应菜品在停售，则套餐不能起售）
     * @param status
     * @param ids
     */
    R<String> updateStatus(int status, Long[] ids);
}
