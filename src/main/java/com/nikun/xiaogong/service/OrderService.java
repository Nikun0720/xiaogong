package com.nikun.xiaogong.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nikun.xiaogong.entity.Orders;

public interface OrderService extends IService<Orders> {

    /**
     * 用户下单
     * @param orders
     */
    void submit(Orders orders);

    /**
     * 再来一单（只能是已完成的订单才能再来一单）
     * @param orderId
     */
    void again(Long orderId);
}
