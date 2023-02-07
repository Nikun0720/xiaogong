package com.nikun.xiaogong.config;

import com.nikun.xiaogong.interceptor.LoginInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 拦截器配置类
 */

@Slf4j
//@Configuration
public class InterceptorConfig implements WebMvcConfigurer {

    /**
     * 将拦截器注册到容器中，并指定拦截规则
     * @param registry
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new LoginInterceptor())  // 拦截器注册到容器中
                .addPathPatterns("/**")  // 所有请求都被拦截包括静态资源
                .excludePathPatterns(   // 放行的请求
                        "/employee/login",
                        "/employee/logout",
                        "/backend/api/**",
                        "/backend/images/**",
                        "/backend/js/**",
                        "/backend/page/login/**",
                        "/backend/plugins/**",
                        "/backend/styles/**",
                        "/backend/favicon.ico",
                        "/front/api/**",
                        "/front/fonts/**",
                        "/front/images/**",
                        "/front/js/**",
                        "/front/page/login.html",
                        "/front/styles/**",
                        "/common/**",
                        "/user/sendMsg",    // 移动端发送短信
                        "/user/login"       // 移动端登录
                );
    }
}
