package com.nikun.xiaogong.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nikun.xiaogong.entity.Employee;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EmployeeMapper extends BaseMapper<Employee> {
}