package com.nikun.xiaogong.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nikun.xiaogong.common.BaseContent;
import com.nikun.xiaogong.common.R;
import com.nikun.xiaogong.entity.ShoppingCart;
import com.nikun.xiaogong.service.ShoppingCartService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/shoppingCart")
@Slf4j
public class ShoppingCartController {

    @Autowired
    private ShoppingCartService shoppingCartService;

    /**
     * 添加购物车
     * @param shoppingCart
     * @return
     */
    @PostMapping("/add")
    public R<ShoppingCart> add(@RequestBody ShoppingCart shoppingCart) {
        //log.info("购物车：{}", shoppingCart);

        // 设置用户id，指定是哪个用户的购物车数据
        Long userId = BaseContent.getCurrentId();
        shoppingCart.setUserId(userId);

        // 查询当前菜品（以及对应的口味）或者套餐是否在购物车中
        Long dishId = shoppingCart.getDishId();
        String dishFlavor = shoppingCart.getDishFlavor();
        Long setmealId = shoppingCart.getSetmealId();

        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShoppingCart::getUserId, userId)
                .eq(dishId != null, ShoppingCart::getDishId, dishId)    // 添加到购物车的是菜品
                .eq(StringUtils.isNotEmpty(dishFlavor), ShoppingCart::getDishFlavor, dishFlavor)    // 添加到购物车的菜品有无对应的口味
                .eq(setmealId != null, ShoppingCart::getSetmealId, setmealId);   // 添加到购物车的是套餐

        // SQL: select * from shopping_cart where user_id = ? and dish_id/setmeal/id = ?
        ShoppingCart cartServiceOne = shoppingCartService.getOne(queryWrapper);

        if(cartServiceOne != null) {
            // 如果已经存在，就在原来的数量基础上+1
            Integer number = cartServiceOne.getNumber();
            cartServiceOne.setNumber(number + 1);
            shoppingCartService.updateById(cartServiceOne);
        }else {
            // 如果不存在，则添加到购物车，数量默认为1
            shoppingCart.setNumber(1);
            shoppingCartService.save(shoppingCart);
            cartServiceOne = shoppingCart;
        }

        return R.success(cartServiceOne);
    }

    /**
     * 让购物车的商品-1
     * @return
     */
    @PostMapping("/sub")
    public R<ShoppingCart> sub(@RequestBody ShoppingCart shoppingCart) {
        // 减到0，删除，并且返回时数量要设置为0，这是前端代码要求的
        Long dishId = shoppingCart.getDishId();
        Long setmealId = shoppingCart.getSetmealId();

        // 根据userid和dishid查询当前菜品（以及对应的口味）或者套餐在购物车中有多份
        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(shoppingCart.getDishId() != null, ShoppingCart::getDishId, dishId);
        queryWrapper.eq(shoppingCart.getSetmealId() != null, ShoppingCart::getSetmealId, setmealId);
        queryWrapper.eq(ShoppingCart::getUserId, BaseContent.getCurrentId());
        ShoppingCart cartServiceOne = shoppingCartService.getOne(queryWrapper);


        int number = cartServiceOne.getNumber();
        cartServiceOne.setNumber(number - 1);
        if(number > 1) {
            // 如果有多份，就在原来的数量基础上-1，并更新shopping_cart表
            shoppingCartService.updateById(cartServiceOne);
        }else {
            // 如果只有一份，则删除shopping_cart表中对应菜品的数据
            shoppingCartService.removeById(cartServiceOne.getId());
        }

        return R.success(cartServiceOne);
    }

    /**
     * 根据条件查询对应的购物车数据
     * @return
     */
    @GetMapping("/list")
    public R<List<ShoppingCart>> list() {

        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShoppingCart::getUserId, BaseContent.getCurrentId());
        queryWrapper.orderByDesc(ShoppingCart::getCreateTime);

        List<ShoppingCart> list = shoppingCartService.list(queryWrapper);

        return R.success(list);
    }

    /**
     * 清空购物车
     * @return
     */
    @DeleteMapping("/clean")
    public R<String> clean() {

        Long userId = BaseContent.getCurrentId();

        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(userId != null, ShoppingCart::getUserId, userId);

        // SQL: delete from shopping_cart where user_id = ?
        shoppingCartService.remove(queryWrapper);

        return R.success("购物车已清空");
    }
}
