package com.nowcoder.community.config;

import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter implements CommunityConstant {

    @Override
    public void configure(WebSecurity web) throws Exception{
        web.ignoring().antMatchers("/resources/**"); //忽略掉对静态资源的拦截 ，/resources/下的所有静态资源可以直接访问
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception{
        //授权
        http.authorizeRequests()
                .antMatchers( //需要用户登录之后才能访问
                        "/user/setting",//用户设置
                        "/user/upload",//上传头像
                        "/discuss/add",//用户发帖
                        "/comment/add/**",//添加评论
                        "/letter/**", //私信所有的功能
                        "/notice/**",//通知
                        "/like",//点赞
                        "/follow",//关注
                        "/unfollow"//取消关注
                )
                .hasAnyAuthority( //拥有该权限的用户：普通用户、管理员、版主
                        AUTHORITY_USER,
                        AUTHORITY_ADMIN,
                        AUTHORITY_MODERATOR
                )
                .antMatchers(
                        "/discuss/top",
                        "/discuss/wonderful"
                )
                .hasAnyAuthority(
                        AUTHORITY_MODERATOR //版主能够使用置顶和加精的功能
                )
                .antMatchers(
                        "/discuss/delete",
                        "/data/**",
                        "/actuator/**" //项目监控， 权限控制，只能管理员访问
                )
                .hasAnyAuthority(
                        AUTHORITY_ADMIN //管理员能够使用删除的功能
                )
                .anyRequest().permitAll() //除了antMatchers内的路径，其他路径无论登不登录都能够直接访问
                .and().csrf().disable(); //禁用csrf凭证和检查
        //权限不够时的处理
        http.exceptionHandling()
                .authenticationEntryPoint(new AuthenticationEntryPoint() {
                    //用户没有登录时，需要做的处理
                    @Override
                    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException e) throws IOException, ServletException {
                        String xRequestWith = request.getHeader("x-requested-with");//获取当前请求是异步请求还是同步请求
                        if("XMLHttpRequest".equals(xRequestWith)){ //异步请求 ，返回json字符串响应，提醒登录
                            response.setContentType("application/plain;charset=utf-8"); //声明返回的数据类型，为普通的字符串，（支持中文）
                            PrintWriter writer = response.getWriter();
                            writer.write(CommunityUtil.getJSONString(403,"你还没有重新登录哦！")); //返回json字符串
                        }else{ //同步请求 ，重定向，返回登录用户的页面，提醒登录
                            response.sendRedirect(request.getContextPath() + "/login");//重定向到登录页面
                        }
                    }
                })
                .accessDeniedHandler(new AccessDeniedHandler() {
                    //用户权限不足时，需要做的处理
                    @Override
                    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException e) throws IOException, ServletException {
                        String xRequestWith = request.getHeader("x-requested-with");//获取当前请求是异步请求还是同步请求
                        if("XMLHttpRequest".equals(xRequestWith)){ //异步请求 ，返回json字符串响应，提醒登录
                            response.setContentType("application/plain;charset=utf-8"); //声明返回的数据类型，为普通的字符串，（支持中文）
                            PrintWriter writer = response.getWriter();
                            writer.write(CommunityUtil.getJSONString(403,"你没有访问次功能的权限！")); //返回json字符串
                        }else{ //同步请求 ，重定向，返回登录用户的页面，提醒登录
                            response.sendRedirect(request.getContextPath() + "/denied");//重定向到拒绝访问时的提示页面
                        }
                    }
                });

        //Security底层默认会拦截/logout请求，进行退出处理， 因为Security采用的是Filter 过滤器进行拦截 做权限的管理，
        //而Filter的执行在DispatchServlet之前，因此肯定在Controller之前，所以如果Security提前拦截到/logout帮我们进行用户退出以后，
        //程序将不会继续向下执行，因此我们所写的logout代码将不会被执行
        //解决方法：覆盖它默认的逻辑，才能执行我们自己的退出代码
        http.logout().logoutUrl("/securitylogout");//修改security拦截的退出请求， 将url改成一个不存在的/securitylogout，实现不会对退出请求进行拦截的目的
    }
}
