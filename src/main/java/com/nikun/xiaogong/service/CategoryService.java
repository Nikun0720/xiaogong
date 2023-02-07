package com.nikun.xiaogong.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nikun.xiaogong.entity.Category;

public interface CategoryService extends IService<Category> {
    void remove(Long id);
}
