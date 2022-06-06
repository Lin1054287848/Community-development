package com.nowcoder.community.service;

import com.nowcoder.community.dao.AlphaDao;
import com.nowcoder.community.dao.DiscussPostMapper;
import com.nowcoder.community.dao.UserMapper;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.util.CommunityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Date;

@Service //业务组件--注解
 //@Scope("singlton")//默认的是单个实例 每次访问bean 都只会有一个实例 项目一般都用单例
//@Scope("prototype") //多个实例 每次访问bean 都会创造出一个新的实例
public class AlphaService {

    private static final Logger logger = LoggerFactory.getLogger(AlphaService.class);

    @Autowired
    private AlphaDao alphaDao;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private DiscussPostMapper discussPostMapper;

    @Autowired
    private TransactionTemplate transactionTemplate;

    public AlphaService(){ //构造器
        System.out.println( "实例化AlphaService");
    }
    @PostConstruct //意味着 该方法会在构造器之后进行调用
    public void init(){
        System.out.println( "初始化AlphaService");
    }
    @PreDestroy //在对象销毁之前调用该方法 因为对象销毁之后就无法再调用了 ，一般用来释放某些资源
    public void destroy(){
        System.out.println( "销毁AlphaService");
    }

    public String find(){
        return alphaDao.select(); // 调用了alphaDao.select()方法
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)//注释为事务管理,isolation设置事务隔离的级别
    public Object save1(){
        //新增用户
        User user = new User();
        user.setUsername("alpha");
        user.setSalt(CommunityUtil.generateUUID().substring(0,5));
        user.setPassword(CommunityUtil.md5("123"+user.getSalt()));
        user.setEmail("alpha@qq.com");
        user.setHeaderUrl("http://image.nowcoder.com/head/99t.png");
        user.setCreateTime(new Date());
        userMapper.insertUser(user);

        //新增帖子
        DiscussPost post = new DiscussPost();
        post.setUserId(user.getId());
        post.setTitle("测试");
        post.setContent("测试事务管理的隔离性！");
        post.setCreateTime(new Date());
        discussPostMapper.insertDiscussPost(post);
        discussPostMapper.insertDiscussPost(post);

        Integer.valueOf("abc"); //将abc转为整数 一定会报错， 模拟事务出错

        return "ok";
    }


    // 让该方法在多线程环境下,被异步的调用.
    @Async //写了@Async 就可以对其进行异步调用
    public void execute1() {
        logger.debug("execute1");
    }

    //@Scheduled(initialDelay = 10000, fixedRate = 1000) //@Scheduled自动的执行 ，只要有程序在运行，不需要调用， 延迟10秒运行 ，间隔1秒
    //Spring 首先会通过类 ScheduledAnnotationBeanPostProcessor 的 postProcessAfterInitialization 方法去初始化 bean，
    // 待初始化完 bean 后，就会拦截所有用到“@Scheduled”注解的方法，进行调度处理
    public void execute2() {
        logger.debug("execute2");
    }

}
