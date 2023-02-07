package com.nikun.xiaogong.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nikun.xiaogong.common.R;
import com.nikun.xiaogong.entity.Category;
import com.nikun.xiaogong.service.CategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 分类管理
 */
@Slf4j
@RestController
@RequestMapping("/category")
public class CategoryController {


    @Autowired
    private CategoryService categoryService;

    /**
     * 新增分类
     * @param category
     * @return
     */
    @PostMapping
    public R<String> save(@RequestBody Category category) {
        log.info("category:{}", category);

        categoryService.save(category);

        return R.success("新增分类成功");
    }

    @GetMapping("/page")
    public R<Page> page(int page, int pageSize) {
        //log.info("page = {}, pageSize = {}, name = {}", page, pageSize);

        // 构造分页构造器
        Page pageInfo = new Page(page, pageSize);

        // 构造条件构造器
        LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();
        // 添加排序条件
        queryWrapper.orderByAsc(Category::getType).orderByAsc(Category::getSort);

        // 执行查询
        categoryService.page(pageInfo, queryWrapper);

        return R.success(pageInfo);
    }

    /**
     * 根据id删除分类(注意这里，由于前段发送的请求参数为ids，不是id，因此需要@RequestParam注解)
     * @param id
     * @return
     */
    @DeleteMapping
    public R<String> delete(@RequestParam("ids") Long id) {
        log.info("删除分类，id为：{}", id);

        categoryService.remove(id);

        return R.success("分类信息删除成功");
    }

    /**
     * 根据id修改分类信息
     * @param category
     * @return
     */
    @PutMapping
    public R<String> update(@RequestBody Category category) {

        categoryService.updateById(category);

        return R.success("分类信息修改成功");
    }

    /**
     * 根据条件查询分类数据（新建菜品那个页面）
     * @param category
     * @return
     */
    @GetMapping("/list")
    public R<List<Category>> list(Category category) {
        // 条件构造器
        LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();
        // 添加条件
        queryWrapper.eq(category.getType() != null, Category::getType, category.getType());
        // 添加排序条件
        queryWrapper.orderByAsc(Category::getSort).orderByDesc(Category::getUpdateTime);

        // 执行查询
        List<Category> list = categoryService.list(queryWrapper);

        return R.success(list);
    }
}
