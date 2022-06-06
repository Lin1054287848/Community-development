package com.nowcoder.community.service;

import com.nowcoder.community.dao.LoginTicketMapper;
import com.nowcoder.community.dao.UserMapper;
import com.nowcoder.community.entity.LoginTicket;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.MailClient;
import com.nowcoder.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.jaas.JaasGrantedAuthority;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.thymeleaf.TemplateEngine;
import org.apache.commons.lang3.StringUtils;
import org.thymeleaf.context.Context;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class UserService implements CommunityConstant {

    @Autowired
    private UserMapper userMapper;

    @Autowired //Ioc 注入
    private MailClient mailClient; //注入邮件客户端

    @Autowired //Ioc 注入
    private TemplateEngine templateEngine; //注入模板引擎

    @Value("${community.path.domain}") //因为是注入一个固定的值，而不是一个bean，因此不能使用@Autowired 注入，应该使用@Value("${参数}")
    private String domain; //注入域名，声明一个变量来接收这个值

    @Value("${server.servlet.context-path}")
    private String contextPath;//注入项目名（及应用的路径），声明一个变量来接收这个值

    //@Autowired
    //private LoginTicketMapper loginTicketMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    public User findUserById(int id){
        //return userMapper.selectById(id); //直接从mysql查询数据 // 注释掉，redis优化， 使用redis缓存查询数据 ，查不到再查mysql
        User user = getCache(id); //从redis缓存中查询数据
        if(user == null){ //缓存没查到，从mysql数据库中查询数据，再将查到的数据存入缓存中
            user = initCache(id);
        }
        return user;
    }

    public Map<String, Object> register(User user){
        Map<String, Object> map = new HashMap<>();

        //空值处理
        if(user == null){
            throw new IllegalArgumentException("参数不能为空");
        }
        //判断输入是否为空
        if(StringUtils.isBlank(user.getUsername())){
            map.put("usernameMsg","账号不能为空!");
            return map;
        }
        if(StringUtils.isBlank(user.getUsername())){
            map.put("passwordnameMsg","密码不能为空!");
            return map;
        }
        if(StringUtils.isBlank(user.getUsername())){
            map.put("emailMsg","邮箱不能为空!");
            return map;
        }

        //验证账号是否存在
        User u = userMapper.selectByName(user.getUsername());
        if(u != null){
            map.put("usernameMsg","该账户已存在!");
        }
        //验证邮箱是否存在
        u = userMapper.selectByEmail(user.getEmail());
        if(u != null){
            map.put("usernameMsg","该邮箱已被注册!");
        }

        //注册用户
        user.setSalt(CommunityUtil.generateUUID().substring(0,5));
        user.setPassword(CommunityUtil.md5(user.getPassword() + user.getSalt())); // 用户设置的密码 + 随机字符串 进行MD5加密， 得到实际存入密码
        user.setType(0);//用户类型   0:普通用户
        user.setStatus(0); //状态 ,0: 没有激活
        user.setActivationCode(CommunityUtil.generateUUID()); //发送激活码
        user.setHeaderUrl(String.format("http://images.nowcoder.com/head/%dt.png",new Random().nextInt(1000)));//用户头像地址
        user.setCreateTime(new Date());//用户注册的时间
        userMapper.insertUser(user); //将用户添加到数据库中, 因为配置了mybatis.configuration.useGeneratedKeys=true，因此将用户添加到数据库后，会自动生成id

        //激活邮件
        Context context = new Context();
        context.setVariable("email", user.getEmail()); //设置上下文中 email的参数
        //url : http://localhost:8080/conmmunity/activation/101(用户id)/code(激活码)   激活路径模板
        String url = domain + contextPath + "/activation/" + user.getId() + "/" + user.getActivationCode(); //拼接激活路径（激活网址）
        context.setVariable("url", url); //设置上下文中 url的参数
        String content = templateEngine.process("/mail/activation", context); //利用模板引擎生成 邮件内容 ，将context的参数传入模板中
        mailClient.sendMail(user.getEmail(), "激活账号", content); //发送邮件，user.getEmail():要发送到的用户邮箱,"激活账号":邮件主题，content:邮件内容


        return map; //如果map是空的 表示没有出现问题 ，成功注册
    }

    public int activation(int userId, String code){   //对用户进行激活
        User user = userMapper.selectById(userId);
        if(user.getStatus() == 1){
            return ACTIVATION_REPEAT;
        }else if(user.getActivationCode().equals(code)){
            userMapper.updateStatus(userId, 1);
            clearCache(userId); //对用户进行了修改，数据发生了变更，此时要对缓存进行清除
            return ACTIVATION_SUCCESS;
        }else{
            return ACTIVATION_FAILURE;
        }
    }

    public Map<String, Object> login(String username, String password, int expiredSeconds){
        Map<String, Object> map = new HashMap<>();

        //空值处理
        if(StringUtils.isBlank(username)){ //StringUtils.isBlank(username)判断username是否为空
            map.put("usernameMsg", "账号不能为空！");
            return map;
        }
        if(StringUtils.isBlank(password)){ //StringUtils.isBlank(username)判断username是否为空
            map.put("passwordMsg", "密码不能为空！");
            return map;
        }

        //验证账号
        User user = userMapper.selectByName(username);
        if(user == null){
            map.put("usernameMsg", "该账号不存在!");
            return map;
        }

        //验证状态
        if(user.getStatus() == 0){
            map.put("usernameMsg", "该账号未激活!");
            return map;
        }

        //验证密码
        password = CommunityUtil.md5(password + user.getSalt());
        if(!user.getPassword().equals(password)){
            map.put("passwordMsg", "密码不正确!");
            return map;
        }

        //若执行到这一步 证明登录正确，随即生成生成登录凭证
        LoginTicket loginTicket = new LoginTicket();
        loginTicket.setUserId(user.getId());
        loginTicket.setTicket(CommunityUtil.generateUUID()); //生成一个随机字符串作为凭证Ticket
        loginTicket.setStatus(0); //设置为有效状态、
        loginTicket.setExpired(new Date(System.currentTimeMillis() + expiredSeconds * 1000)); //expiredSeconds为过期时间长，new Date(System.currentTimeMillis() + expiredSeconds * 1000)过期的时刻
        //loginTicketMapper.insertLoginTicket(loginTicket);//存入登录凭证  //注释掉 重构使用redis

        //redis重构部分
        String redisKey = RedisKeyUtil.getKaptchaKey(loginTicket.getTicket()); //获取键值
        redisTemplate.opsForValue().set(redisKey, loginTicket); //redis中存入登录凭证

        map.put("ticket", loginTicket.getTicket()); //返回凭证
        return map;
    }

    public void logout(String ticket){ //退出登录
        //loginTicketMapper.updateStatus(ticket, 1); //将用户状态该为无效1 //注释掉 重构使用redis
        String redisKey = RedisKeyUtil.getKaptchaKey(ticket); //获取键值
        LoginTicket loginTicket = (LoginTicket) redisTemplate.opsForValue().get(redisKey);//获得该凭证的用户对象
        loginTicket.setStatus(1);//将用户状态该为无效1
        redisTemplate.opsForValue().set(redisKey, loginTicket);//修改用户状态后，再次存入redis 覆盖原有的value值


    }

    public LoginTicket findLoginTicket(String ticket){ //查询凭证方法
        //return loginTicketMapper.selectByTicket(ticket); //存在则返回ticket值，否则为null  //注释掉 重构使用redis
        String redisKey = RedisKeyUtil.getKaptchaKey(ticket); //获取键值
        return (LoginTicket) redisTemplate.opsForValue().get(redisKey);//存在则返回ticket值，否则为null
    }

    public int updateHeader(int userId, String headerUrl){ //更新用户头像的方法
        //return userMapper.updateHeader(userId, headerUrl);
        int rows = userMapper.updateHeader(userId, headerUrl);
        clearCache(userId); //对用户进行了修改，数据发生了变更，此时要对缓存进行清除
        return rows;
    }

    // 修改密码
    public Map<String, Object> updatePassword(int userId, String oldPassword, String newPassword) {
        Map<String, Object> map = new HashMap<>();

        // 空值处理
        if (StringUtils.isBlank(oldPassword)) {
            map.put("oldPasswordMsg", "原密码不能为空!");
            return map;
        }
        if (StringUtils.isBlank(newPassword)) {
            map.put("newPasswordMsg", "新密码不能为空!");
            return map;
        }

        // 验证原始密码
        User user = userMapper.selectById(userId);
        oldPassword = CommunityUtil.md5(oldPassword + user.getSalt());
        if (!user.getPassword().equals(oldPassword)) {
            map.put("oldPasswordMsg", "原密码输入有误!");
            return map;
        }

        // 更新密码
        newPassword = CommunityUtil.md5(newPassword + user.getSalt());
        userMapper.updatePassword(userId, newPassword);

        return map;
    }

    //查询方法
    public User findUserByName(String username){
        return userMapper.selectByName(username);
    }

    //1.优先从缓存中取值
    private User getCache(int userId) {
        String redisKey = RedisKeyUtil.getUserKey(userId); //获取用户的key
        return (User) redisTemplate.opsForValue().get(redisKey);
    }

    //2.取不到时，初始化缓存数据(往缓存中存入数据)
    private User initCache(int userId){
        User user = userMapper.selectById(userId); //从mysql数据库中，用id查询User
        String redisKey = RedisKeyUtil.getUserKey(userId); //获取用户的key
        redisTemplate.opsForValue().set(redisKey, user, 3600, TimeUnit.SECONDS);//向redis存入数据，并设置过期时间3600秒
        return user; //返回得到的user
    }

    //3.数据变更时清除缓存数据
    private void clearCache(int userId){
        String redisKey = RedisKeyUtil.getUserKey(userId); //获取用户的key
        redisTemplate.delete(redisKey);//删除key
    }

    public Collection<? extends GrantedAuthority> getAuthorities(int userId){
        User user = this.findUserById(userId);

        List<GrantedAuthority> list = new ArrayList<>();
        list.add(new GrantedAuthority() {
            @Override
            public String getAuthority() {
                switch (user.getType()){
                    case 1:
                        return AUTHORITY_ADMIN;
                    case 2:
                        return AUTHORITY_MODERATOR;
                    default:
                        return AUTHORITY_USER;

                }
            }
        });
        return list;
    }



}
