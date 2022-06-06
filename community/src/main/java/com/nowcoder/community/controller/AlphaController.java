package com.nowcoder.community.controller;

import com.nowcoder.community.service.AlphaService;
import com.nowcoder.community.util.CommunityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

@Controller
@RequestMapping("/alpha") //类似ip地址扩展的页面 http://localhost:8080/alpha
public class AlphaController {


    @Autowired
    private AlphaService alphaService;

    @RequestMapping("/hello")//类似ip地址扩展的页面 http://localhost:8080/alpha/hello
    @ResponseBody
    public String sayHello(){
        return "Hello Spring Boot";
    }

    @RequestMapping("/data")//类似ip地址扩展的页面 http://localhost:8080/alpha/hello
    @ResponseBody
    public String getData(){
        return alphaService.find(); //调用了 alphaService.find()方法
    }

    @RequestMapping("/http") //访问路径
    public void http(HttpServletRequest request, HttpServletResponse response){   //request 请求对象 response 响应对象
        //获取请求数据
        System.out.println(request.getMethod()); //获取请求方式
        System.out.println(request.getContextPath()); //获取请求的路径
        Enumeration<String> enumeration = request.getHeaderNames();//迭代器对象 得到所有请求行的 key  请求行是key-value结构
        while(enumeration.hasMoreElements()){
            String name = enumeration.nextElement();
            String value = request.getHeader(name);
            System.out.println(name + ": "+value);
        } //获取迭代器对象中的有请求头的数据
        System.out.println(request.getParameter("code")); //输出参数名为code的数据

        //返回响应数据
        response.setContentType("text/html;charset=utf-8");//设置返回响应的类型 ,这里设置返回一个网页类型的文本 charset=utf-8是为了支持中文
        try(PrintWriter writer = response.getWriter();) {  //try()在try的（）中写入方法对象，在编译时会自动存在一个finally来close


            writer.write("<h1>牛客网</h1>");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    //GET请求  希望从服务器获取某些数据 默认就是GET请求
    // /students?current=1&limit=20 分页显示所有学生， 分页显示的条件 当前是第几页 current=1， 一页限制显示多少个学生limit=20
    @RequestMapping(path = "/students", method = RequestMethod.GET) //path是路径，method是限制请求的方法，限制GET请求才能处理
    @ResponseBody
    public String getStudents(
            @RequestParam(name = "current", required = false, defaultValue = "1") int current, //current可以不传  不传的话值为1
            @RequestParam(name = "limit", required = false, defaultValue = "10")int limit){
        System.out.println(current);
        System.out.println(limit);

        return "some students";
    }
    // /student/123
    @RequestMapping(path = "/student/{id}",method = RequestMethod.GET)
    @ResponseBody
    public String getStudent(@PathVariable("id") int id){
        System.out.println(id);
        return "a student";
    }

    //POST请求 //发送数据
    @RequestMapping(path = "/student", method = RequestMethod.POST)
    @ResponseBody
    public String saveStudent(String name, int age){
        System.out.println(name);
        System.out.println(age);
        return "success";
    }

    //响应HTML数据
    @RequestMapping(path = "/teacher", method = RequestMethod.GET)
    public ModelAndView getTeacher(){
        ModelAndView mav = new ModelAndView();
        mav.addObject("name","张三");
        mav.addObject("age","30");
        mav.setViewName("/demo/view"); //默认在resources/templates目录下
        return mav;
    }
    @RequestMapping(path = "/school", method = RequestMethod.GET)
    public String getSchool(Model model){   //Model 和 ModelAndView 功能类似  //该方法更简洁
        model.addAttribute("name", "北京大学");
        model.addAttribute("age", 80);
        return "demo/view";
    }

    //响应JSON数据（异步请求）
    //Java对象 -> JSON字符串 -> JS对象 浏览器解析使用JS，因此JSON字符串可以更好的
    @RequestMapping(path = "/emp", method = RequestMethod.GET)
    @ResponseBody
    public Map<String,Object> getEmp(){
        Map<String,Object> emp = new HashMap<>();
        emp = new HashMap<>();
        emp.put("name","张三");
        emp.put("age",23);
        emp.put("salary",8000.00);
        return emp;
    }

    @RequestMapping(path = "/emps", method = RequestMethod.GET)
    @ResponseBody
    public List<Map<String,Object>> getEmps(){
        List<Map<String,Object>> list = new ArrayList<>();
        Map<String,Object> emp = new HashMap<>();
        emp = new HashMap<>();
        emp.put("name","张三");
        emp.put("age",23);
        emp.put("salary",8000.00);
        list.add(emp);

        emp = new HashMap<>();
        emp.put("name","张四");
        emp.put("age",25);
        emp.put("salary",9000.00);
        list.add(emp);

        emp = new HashMap<>();
        emp.put("name","张五");
        emp.put("age",30);
        emp.put("salary",10000.00);
        list.add(emp);

        return list;
    }

    //cookie示例
    //设置cookie
    @RequestMapping(path = "/cookie/set", method = RequestMethod.GET)
    @ResponseBody
    public String setCookie(HttpServletResponse response){
        //创建cookie
        Cookie cookie = new Cookie("code", CommunityUtil.generateUUID());
        //设置cookie生效的范围
        cookie.setPath("community/alpha");
        //设置生效时间 默认是关掉页面就失效
        cookie.setMaxAge(60*10); //设置生效时间为10分钟
        //发送cookie
        response.addCookie(cookie);
        return "set cookie";
    }
    //接收cookie
    @RequestMapping(path = "/cookie/get", method = RequestMethod.GET)
    @ResponseBody
    public String getCookie(@CookieValue("code") String code){  //获取Cookie中对应code的值 赋值给String code
        System.out.println(code);
        return "get cookie:"+code;
    }

    //Session示例
    //设置Session
    @RequestMapping(value = "/session/set", method = RequestMethod.GET)
    @ResponseBody
    public String setSession(HttpSession session){
        session.setAttribute("id",1); //往session中存入数据
        session.setAttribute("name","Test");//往session中存入数据，共存入两条
        return "set session";
    }
    //接收Session
    @RequestMapping(value = "/session/get", method = RequestMethod.GET)
    @ResponseBody
    public String getSession(HttpSession session){
        System.out.println(session.getAttribute("id"));
        System.out.println(session.getAttribute("name"));
        return "id:"+session.getAttribute("id")+"\nname:"+session.getAttribute("name");
    }

    //ajax示例
    @RequestMapping(path = "/ajax", method = RequestMethod.POST)
    //在使用@RequestMapping后，返回值通常解析为跳转路径。加上@responsebody后，返回结果直接写入HTTP response body中，不会被解析为跳转路径。
    // 比如异步请求，希望响应的结果是json数据，那么加上@responsebody后，就会直接返回json数据。
    @ResponseBody //因为是异步请求， 服务器不向浏览器访问网页 ，而是字符串
    public String testAjax(String name, int age){
        System.out.println(name);
        System.out.println(age);
        return CommunityUtil.getJSONString(0,"操作成功");
    }







}
