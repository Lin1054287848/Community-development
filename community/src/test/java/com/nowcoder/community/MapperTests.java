package com.nowcoder.community;
import com.nowcoder.community.dao.*;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.LoginTicket;
import com.nowcoder.community.entity.Message;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.AlphaService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class MapperTests {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private DiscussPostMapper discussPostMapper;

    @Autowired
    private LoginTicketMapper loginTicketMapper;

    @Autowired
    private MessageMapper messageMapper;

    @Test
    public void testSelectUser(){ //测试查询功能
        User user = userMapper.selectById(101);
        System.out.println(user);

        user = userMapper.selectByName("liubei");
        System.out.println(user);

        user = userMapper.selectByEmail("nowcoder101@sina.com");
        System.out.println(user);
    }

    @Test
    public void testInsertUser(){ //测试插入功能
         User user = new User();
         user.setUsername("test");
         user.setPassword("123456");
         user.setSalt("abc");
         user.setEmail("test@qq.com");
         user.setHeaderUrl("http://www.nowcoder.com/101.png");
         user.setCreateTime(new Date());

         int rows = userMapper.insertUser(user); //插入行，。并传回插入的行数
         System.out.println(rows);
         System.out.println(user.getId());
    }

    @Test
    public void updateUser() { //测试修改功能
        int rows = userMapper.updateStatus(150,1); //修改状态
        System.out.println(rows);

        rows = userMapper.updateHeader(150,"http://www.nowcoder.com/102.png"); //修改头像路径
        System.out.println(rows);

        rows = userMapper.updatePassword(150,"654321"); //修改头像路径
        System.out.println(rows);

    }
    @Test
    public void testSelectPosts(){ //测试查询多条数据
        List<DiscussPost> list = discussPostMapper.selectDiscussPosts(149,0,10, 0); //用户id为 149的 第1条开始 共10条
        for(DiscussPost post : list){
            System.out.println(post);
        }
        int rows = discussPostMapper.selectDiscussPostRows(149); //用户id为 149 的 获取他发布的条数总行数
        System.out.println(rows);

    }

    @Test
    public void testInsertLoginTicket(){ //用户插入
        LoginTicket loginTicket = new LoginTicket();
        loginTicket.setUserId(101); //设置用户id
        loginTicket.setTicket("abc"); //设置用户的凭证Ticket
        loginTicket.setStatus(0);//设置用户是否有效 0：有效  1：无效
        loginTicket.setExpired(new Date(System.currentTimeMillis()+1000*60*10)); //设置用户过期时间 10分钟

        loginTicketMapper.insertLoginTicket(loginTicket);//存入loginTicket
    }

    @Test
    public void testSelectLoginTicket() { //用户查询
        LoginTicket loginTicket = loginTicketMapper.selectByTicket("abc"); //获得凭证为"abc"的用户对象
        System.out.println(loginTicket);
        loginTicketMapper.updateStatus("abc", 1); //修改用户状态

        loginTicket = loginTicketMapper.selectByTicket("abc"); //获得凭证为"abc"的用户对象
        System.out.println(loginTicket);

    }

    @Test
    public void testSelectLetters(){
        List<Message> list = messageMapper.selectConversations(111, 0, 20);
        for(Message message : list){
            System.out.println(message);
        }

        int count = messageMapper.selectConversationCount(111);
        System.out.println(count);

        list = messageMapper.selectLetters("111_112", 0, 10);
        for(Message message : list){
            System.out.println(message);
        }

        count = messageMapper.selectLetterCount("111_112");
        System.out.println(count);

        count = messageMapper.selectLetterUnreadCount(131, "111_131");
        System.out.println(count);
    }

}
