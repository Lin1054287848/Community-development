package com.nowcoder.community.controller.interceptor;

import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.DataService;
import com.nowcoder.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class DataInterceptor implements HandlerInterceptor {

    @Autowired
    private DataService dataService;

    @Autowired
    private HostHolder hostHolder;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        //统计UV
        String ip = request.getRemoteHost();
        dataService.recordUV(ip); //进来一个ip， 记录一个ip

        //统计DAU
        User user = hostHolder.getUser();
        if(user != null){
            dataService.recordDAU(user.getId()); //登录一个用户id，记录一个用户id
        }

        return true;
    }
}
