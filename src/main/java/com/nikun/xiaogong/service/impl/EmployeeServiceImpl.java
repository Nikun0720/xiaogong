package com.nikun.xiaogong.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nikun.xiaogong.entity.Employee;
import com.nikun.xiaogong.mapper.EmployeeMapper;
import com.nikun.xiaogong.service.EmployeeService;
import org.springframework.stereotype.Service;

@Service
public class EmployeeServiceImpl extends ServiceImpl<EmployeeMapper, Employee> implements EmployeeService {
}
