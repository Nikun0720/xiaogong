package com.nikun.xiaogong.common;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;


/**
 * 自定义元数据对象处理器
 */
@Component
@Slf4j
public class MyMetaObjectHandler implements MetaObjectHandler {

    // 可以通过自动注入来得到request
    // 但视频里用的ThreadLocal，先以视频为主
    //@Autowired
    //private HttpServletRequest request;

    /**
     * 插入操作，自动填充
     * @param metaObject
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        log.info("公共字段自动填充[insert]...");
        //log.info(metaObject.toString());

        if(metaObject.hasSetter("createTime"))  metaObject.setValue("createTime", LocalDateTime.now());
        if(metaObject.hasSetter("updateTime"))  metaObject.setValue("updateTime", LocalDateTime.now());
        if(metaObject.hasSetter("createUser"))  metaObject.setValue("createUser", BaseContent.getCurrentId());
        if(metaObject.hasSetter("updateUser"))  metaObject.setValue("updateUser", BaseContent.getCurrentId());
    }

    /**
     * 更新操作，自动填充
     * @param metaObject
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        log.info("公共字段自动填充[update]...");
        //log.info(metaObject.toString());

        //long id = Thread.currentThread().getId();
        //log.info("线程id为：{}", id);

        metaObject.setValue("updateTime", LocalDateTime.now());
        metaObject.setValue("updateUser", BaseContent.getCurrentId());
    }
}
