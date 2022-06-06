package com.nowcoder.community.controller.interceptor;

import com.nowcoder.community.annotation.LoginRequired;
import com.nowcoder.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.util.logging.Handler;

@Component
public class LoginRequiredInterceptor implements HandlerInterceptor {

    @Autowired
    private HostHolder hostHolder;//注入拦截器

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception{ //在访问前判断
        if(handler instanceof HandlerMethod){ //判断拦截器拦截的是方法（） 而不是静态资源
            HandlerMethod handlerMethod = (HandlerMethod) handler;// 将Object类型转换为HandlerMethod类型
            Method method = handlerMethod.getMethod(); //通过该方法handlerMethod.getMethod() 获取handler拦截到的对象
            LoginRequired loginRequired = method.getAnnotation(LoginRequired.class);
            if(loginRequired != null && hostHolder.getUser() == null){ //如果当前方法需要登录， user为null 代表没登录
                response.sendRedirect(request.getContextPath() + "/login"); //重定向到登录界面
                return false;
            }
        }
        return true;
    }
}
