package com.nikun.xiaogong.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nikun.xiaogong.common.BaseContent;
import com.nikun.xiaogong.entity.*;
import com.nikun.xiaogong.exception.CustomException;
import com.nikun.xiaogong.mapper.OrderMapper;
import com.nikun.xiaogong.service.*;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Orders> implements OrderService {

    @Autowired
    private ShoppingCartService shoppingCartService;

    @Autowired
    private UserService userService;

    @Autowired
    private AddressBookService addressBookService;

    @Autowired
    private OrderDetailService orderDetailService;

    /**
     * 用户下单
     * @param orders
     */
    @Override
    @Transactional
    public void submit(Orders orders) {

        // 获得当前用户id
        Long userId = BaseContent.getCurrentId();

        // 查询当前用户的购物车数据
        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShoppingCart::getUserId, userId);
        List<ShoppingCart> shoppingCarts = shoppingCartService.list(queryWrapper);
        if(shoppingCarts == null || shoppingCarts.size() == 0) {
            throw new CustomException("购物车为空，不能下单");
        }

        // 查询用户数据
        User user = userService.getById(userId);

        // 查询地址数据
        Long addressBookId = orders.getAddressBookId();
        AddressBook addressBook = addressBookService.getById(addressBookId);
        if(addressBook == null) {
            throw new CustomException("用户地址信息有误，不能下单");
        }

        // 设置订单属性
        long orderId = IdWorker.getId();    // 生成订单号，IdWorker是MyBatisPlus中的

        AtomicInteger amount = new AtomicInteger(0);    // 原子操作，保证在多线程的情况下也是没问题的
        List<OrderDetail> orderDetails = shoppingCarts.stream().map(item -> {
            OrderDetail orderDetail = new OrderDetail();

            // 设置属性
            orderDetail.setOrderId(orderId);
            /*orderDetail.setNumber(item.getNumber());
            orderDetail.setDishFlavor(item.getDishFlavor());
            orderDetail.setDishId(item.getDishId());
            orderDetail.setSetmealId(item.getSetmealId());
            orderDetail.setName(item.getName());
            orderDetail.setImage(item.getImage());
            orderDetail.setAmount(item.getAmount());*/
            BeanUtils.copyProperties(item, orderDetail, "id");

            amount.addAndGet(item.getAmount().multiply(new BigDecimal(item.getNumber())).intValue());
            return orderDetail;
        }).collect(Collectors.toList());

        orders.setId(orderId);
        orders.setOrderTime(LocalDateTime.now());
        orders.setCheckoutTime(LocalDateTime.now());
        orders.setStatus(2);
        orders.setAmount(new BigDecimal(amount.get()));     //总金额
        orders.setUserId(userId);
        orders.setNumber(String.valueOf(orderId));
        orders.setUserName(user.getName());
        orders.setConsignee(addressBook.getConsignee());
        orders.setPhone(addressBook.getPhone());
        orders.setAddress((addressBook.getProvinceName() == null ? "" : addressBook.getProvinceName())
                + (addressBook.getCityName() == null ? "" : addressBook.getCityName())
                + (addressBook.getDistrictName() == null ? "" : addressBook.getDistrictName())
                + (addressBook.getDetail() == null ? "" : addressBook.getDetail()));

        // 向订单表插入数据，一条数据
        this.save(orders);

        // 向订单明细表插入数据，多条数据
        orderDetailService.saveBatch(orderDetails);

        // 清空购物车数据
        shoppingCartService.remove(queryWrapper);

    }

    /**
     * 再来一单（只能是已完成的订单才能再来一单）
     * @param orderId
     */
    @Override
    @Transactional
    public void again(Long orderId) {

        // 根据orderId查询order表
        Orders order = this.getById(orderId);


        // 生成订单号，IdWorker是MyBatisPlus中的
        long newOrderId = IdWorker.getId();

        // 更新新订单的部分属性
        order.setId(newOrderId);
        order.setNumber(Long.toString(newOrderId));
        order.setStatus(2);
        order.setOrderTime(LocalDateTime.now());
        order.setCheckoutTime(LocalDateTime.now());

        // 将新的订单插入order表中
        this.save(order);

        // 根据orderId查询order_detail表
        LambdaQueryWrapper<OrderDetail> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OrderDetail::getOrderId, orderId);
        List<OrderDetail> orderDetailList = orderDetailService.list(queryWrapper);

        // 更改每个订单详情的orderId属性
        orderDetailList.forEach(item -> {
            item.setOrderId(newOrderId);
            item.setId(null);
        });

        // 将新的订单详情插入order_list表中
        orderDetailService.saveBatch(orderDetailList);

    }
}
