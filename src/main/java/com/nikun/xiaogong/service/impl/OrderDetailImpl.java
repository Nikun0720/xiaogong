package com.nikun.xiaogong.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nikun.xiaogong.entity.OrderDetail;
import com.nikun.xiaogong.mapper.OrderDetailMapper;
import com.nikun.xiaogong.service.OrderDetailService;
import org.springframework.stereotype.Service;

@Service
public class OrderDetailImpl extends ServiceImpl<OrderDetailMapper, OrderDetail> implements OrderDetailService {
}
