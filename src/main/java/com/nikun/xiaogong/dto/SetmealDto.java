package com.nikun.xiaogong.dto;

import com.nikun.xiaogong.entity.Setmeal;
import com.nikun.xiaogong.entity.SetmealDish;
import lombok.Data;
import java.util.List;

@Data
public class SetmealDto extends Setmeal {

    private List<SetmealDish> setmealDishes;

    private String categoryName;
}
