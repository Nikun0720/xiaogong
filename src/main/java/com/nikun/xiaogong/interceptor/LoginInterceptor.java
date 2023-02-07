package com.nikun.xiaogong.interceptor;

import com.alibaba.fastjson.JSON;
import com.nikun.xiaogong.common.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * 拦截器
 * 在未登录时不准访问内部页面
 * 放行静态资源
 */
@Slf4j
public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        String requestURI = request.getRequestURI();
        log.info("preHandle拦截的路径是：{}", requestURI);

        // 登录检查逻辑
        HttpSession session = request.getSession();

        Object employee = session.getAttribute("employee");
        if(employee != null) {
            log.info("放行");
            return true;
        }

        Object user = session.getAttribute("user");
        if(user != null) {
            log.info("放行");
            return true;
        }

        // 拦截住、未登录，跳转到登录页
        log.info("拦截住或未登录");
        response.getWriter().write(JSON.toJSONString(R.error("NOTLOGIN")));
        return false;
    }
}
