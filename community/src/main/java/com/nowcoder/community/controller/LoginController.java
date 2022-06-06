package com.nowcoder.community.controller;

import com.google.code.kaptcha.Producer;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.RedisKeyUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.imageio.ImageIO;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Controller
public class LoginController implements CommunityConstant {

    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);//声明日志对象
    @Autowired
    private UserService userService;

    @Autowired
    private Producer kaptchaProducer; //通过容器获取，并自动注入验证码对象

    @Autowired
    private RedisTemplate redisTemplate;

    @Value("${server.servlet.context-path}")
    private String contextPath; //设置一个接收该属性的成员


    /**
     * RequestMapping是一个用来处理请求地址映射的注解，可用于类或方法上。用于类上，表示类中的所有响应请求的方法都是以该地址作为父路径。
     * value：  指定请求的实际地址，指定的地址可以是URI Template 模式；
     * method：  指定请求的method类型， GET、POST、PUT、DELETE等；
     */
    @RequestMapping(path = "/register", method = RequestMethod.GET)
    public String getRegisterPage() {  //注册页面
        return "/site/register"; //模板位置
    }

    @RequestMapping(path = "/login", method = RequestMethod.GET)
    public String getLoginPage() {  //注册页面
        return "/site/login"; //模板位置
    }

    @RequestMapping(path = "/register", method = RequestMethod.POST)//注册请求，是浏览器向服务器提交数据，因此使用POST
    public String register(Model model, User user) {  //返回视图的名字
        Map<String, Object> map = userService.register(user);
        if (map == null || map.isEmpty()) {
            //model.addAttribute 往前台传数据，可以传对象，可以传List，通过el表达式 ${}可以获取到，类似于request.setAttribute(“sts”,sts)效果一样。
            model.addAttribute("msg", "注册成功,我们已经向您的邮箱发送了一封激活邮件,请尽快激活"); //设置中间页面的提示消息msg
            model.addAttribute("target", "/index");//设置中间页面的跳转目标"/index"
            return "/site/operate-result"; //从controller层控制跳转到"/site/operate-result"
        } else {
            model.addAttribute("usernameMsg", map.get("usernameMsg"));//设置usernameMsg的错误提示
            model.addAttribute("passwordMsg", map.get("passwordMsg"));//设置passwordMsg的错误提示
            model.addAttribute("emailMsg", map.get("emailMsg"));//设置emailMsg的错误提示
            return "/site/register";
        }

    }

    //url : http://localhost:8080/conmmunity/activation/101(用户id)/code(激活码)   激活路径模板
    @RequestMapping(path = "/activation/{userId}/{code}", method = RequestMethod.GET)
    public String activation(Model model, @PathVariable("userId") int userId, @PathVariable("code") String code) {
        int result = userService.activation(userId, code);
        if(result == ACTIVATION_SUCCESS){
            model.addAttribute("msg", "激活成功，您的账号已经可以正常使用了！"); //设置中间页面的提示消息msg
            model.addAttribute("target", "/login");//设置中间页面的跳转目标"/login"登录页面
        }else if(result == ACTIVATION_REPEAT){
            model.addAttribute("msg", "无效操作，该账号已经激活过了！"); //设置中间页面的提示消息msg
            model.addAttribute("target", "/index");//设置中间页面的跳转目标"/index"首页
        }else{
            model.addAttribute("msg", "激活失败，您提供的激活码不正确！"); //设置中间页面的提示消息msg
            model.addAttribute("target", "/index");//设置中间页面的跳转目标"/index"
        }
        return "/site/operate-result"; //从controller层控制跳转到"/site/operate-result"
    }
    /* MySql操作
    @RequestMapping(path = "/kaptcha", method = RequestMethod.GET)
    public void getKaptcha(HttpServletResponse response, HttpSession session){
        //生成验证码
        String text = kaptchaProducer.createText(); //根据KaptchaConfig的验证码设置生成一个随机字符串
        BufferedImage image = kaptchaProducer.createImage(text);//利用该字符串生成一个与之对应的验证码图片

        //将验证码存入session
        session.setAttribute("kaptcha",text);

        //将图片输出给浏览器
        response.setContentType("image/png"); //声明给浏览器返回的什么类型的数据 image图片，且为png格式
        try {
            OutputStream os = response.getOutputStream(); //获取字节流
            ImageIO.write(image,"png",os);//向浏览器输出图片image，"png"格式,用os输出
        } catch (IOException e) {
            logger.error("响应验证码失败"+e.getMessage());
        }
    }
    */

    /* MySql操作
    @RequestMapping(path = "/login", method = RequestMethod.POST)
    public String login(String username, String password, String code, boolean rememberme,
                        Model model, HttpSession session, HttpServletResponse response){
        String kaptcha = (String) session.getAttribute("kaptcha");
        //检查验证码
        if(StringUtils.isBlank(kaptcha) || StringUtils.isBlank(code) || !kaptcha.equalsIgnoreCase(code)){ //验证码为空，输入的验证码为空，输入的验证码和验证码不同，equalsIgnoreCase代表比较时忽略大小
            model.addAttribute("codeMsg","验证码不正确!");
            return "site/login"; //返回登录页面
        }
        //检查账号，密码
        int expiredSeconds = rememberme ? REMEMBER_EXPIRED_SECONDS : DEFAULT_EXPIRED_SECONDS; //设置凭证过期时间
        Map<String, Object> map = userService.login(username, password, expiredSeconds);
        if(map.containsKey("ticket")){
            Cookie cookie = new Cookie("ticket", map.get("ticket").toString());
            cookie.setPath(contextPath);//设置cookie的有效范围  --全范围 /community
            cookie.setMaxAge(expiredSeconds);//设置cookie的有效时间
            response.addCookie(cookie); //在响应时将cookie发送给浏览器
            return "redirect:/index"; //重定向到首页
        }else{
            model.addAttribute("usernameMsg",map.get("usernameMsg"));
            model.addAttribute("passwordMsg",map.get("passwordMsg"));
            return "site/login"; //返回登录页面
        }
    }
    */

    //重构MySql操作， 使用Redis操作
    @RequestMapping(path = "/kaptcha", method = RequestMethod.GET)
    public void getKaptcha(HttpServletResponse response/*, HttpSession session*/){
        //生成验证码
        String text = kaptchaProducer.createText(); //根据KaptchaConfig的验证码设置生成一个随机字符串
        BufferedImage image = kaptchaProducer.createImage(text);//利用该字符串生成一个与之对应的验证码图片

        //将验证码存入session
        //session.setAttribute("kaptcha",text);

        //验证码的归属，属于哪一个将要登录的用户（哪个即将登录用户的会话窗口）
        String kaptchaOwner = CommunityUtil.generateUUID(); //随机生成一个字符串作为登录用户的凭证
        Cookie cookie = new Cookie("kaptchaOwner", kaptchaOwner); //在cookie中存入kaptchaOwner
        cookie.setMaxAge(60); //设置cookie(验证码)失效时间 60秒
        cookie.setPath(contextPath);//设置cookie的有效路径,contextPath整个项目都有效
        response.addCookie(cookie);//将cookie添加到response中发送给客户端
        //将验证码存入Redis
        String redisKey = RedisKeyUtil.getKaptchaKey(kaptchaOwner);
        redisTemplate.opsForValue().set(redisKey, text, 60, TimeUnit.SECONDS); //redis操作,存入redis数据库,key-value: key:登录验证码的Key值 ，value:随机字符串, 过期时间:60, 时间单位:TimeUnit.SECONDS(秒)

        //将图片输出给浏览器
        response.setContentType("image/png"); //声明给浏览器返回的什么类型的数据 image图片，且为png格式
        try {
            OutputStream os = response.getOutputStream(); //获取字节流
            ImageIO.write(image,"png",os);//向浏览器输出图片image，"png"格式,用os输出
        } catch (IOException e) {
            logger.error("响应验证码失败"+e.getMessage());
        }
    }

    //重构MySql操作， 使用Redis操作
    @RequestMapping(path = "/login", method = RequestMethod.POST)
    public String login(String username, String password, String code, boolean rememberme,
                        Model model, /*HttpSession session, */HttpServletResponse response,
                        @CookieValue("kaptchaOwner") String kaptchaOwner){
        //检查验证码
        //String kaptcha = (String) session.getAttribute("kaptcha");

        String kaptcha = null;
        if(StringUtils.isNoneBlank(kaptchaOwner)){ //判断验证码有没有失效，失效kaptchaOwner则为空
            String redisKey = RedisKeyUtil.getKaptchaKey(kaptchaOwner);
            kaptcha = (String) redisTemplate.opsForValue().get(redisKey); //获取到验证码
        }

        if(StringUtils.isBlank(kaptcha) || StringUtils.isBlank(code) || !kaptcha.equalsIgnoreCase(code)){ //验证码为空，输入的验证码为空，输入的验证码和验证码不同，equalsIgnoreCase代表比较时忽略大小
            model.addAttribute("codeMsg","验证码不正确!");
            return "site/login"; //返回登录页面
        }
        //检查账号，密码
        int expiredSeconds = rememberme ? REMEMBER_EXPIRED_SECONDS : DEFAULT_EXPIRED_SECONDS; //设置凭证过期时间
        Map<String, Object> map = userService.login(username, password, expiredSeconds);
        if(map.containsKey("ticket")){
            Cookie cookie = new Cookie("ticket", map.get("ticket").toString());
            cookie.setPath(contextPath);//设置cookie的有效范围  --全范围 /community
            cookie.setMaxAge(expiredSeconds);//设置cookie的有效时间
            response.addCookie(cookie); //在响应时将cookie发送给浏览器
            return "redirect:/index"; //重定向到首页
        }else{
            model.addAttribute("usernameMsg",map.get("usernameMsg"));
            model.addAttribute("passwordMsg",map.get("passwordMsg"));
            return "site/login"; //返回登录页面
        }
    }

    @RequestMapping(path = "/logout", method = RequestMethod.GET)
    public String logout(@CookieValue("ticket") String ticket){
        userService.logout(ticket);
        SecurityContextHolder.clearContext();
        return "redirect:/login"; //重定向到登录页面
    }

}
