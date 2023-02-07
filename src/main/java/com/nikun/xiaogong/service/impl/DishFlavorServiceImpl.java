package com.nikun.xiaogong.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nikun.xiaogong.entity.DishFlavor;
import com.nikun.xiaogong.mapper.DishFlavorMapper;
import com.nikun.xiaogong.service.DishFlavorService;
import org.springframework.stereotype.Service;

@Service
public class DishFlavorServiceImpl extends ServiceImpl<DishFlavorMapper, DishFlavor> implements DishFlavorService {
}
