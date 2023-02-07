package com.nikun.xiaogong.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nikun.xiaogong.entity.User;
import com.nikun.xiaogong.mapper.UserMapper;
import com.nikun.xiaogong.service.UserService;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
}
