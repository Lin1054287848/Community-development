package com.nowcoder.community.controller;

import com.nowcoder.community.entity.Comment;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Event;
import com.nowcoder.community.event.EventProducer;
import com.nowcoder.community.service.CommentService;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.HostHolder;
import com.nowcoder.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Date;

@Controller
@RequestMapping("/comment")
public class CommentController implements CommunityConstant {

    @Autowired
    private CommentService commentService;

    @Autowired
    private HostHolder hostHolder; //为了得到当前的用户id

    @Autowired
    private EventProducer eventProducer;

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private RedisTemplate redisTemplate;

    @RequestMapping(path = "/add/{discussPostId}", method = RequestMethod.POST)
    public String addComment(@PathVariable("discussPostId") int discussPostId, Comment comment){//@PathVariable 获得路径中的变量参数
        comment.setUserId(hostHolder.getUser().getId()); //设置用户id
        comment.setStatus(0);
        comment.setCreateTime(new Date());
        commentService.addComment(comment);

        //触发评论事件
        Event event = new Event()
                .setTopic(TOPIC_COMMENT)
                .setUserId(hostHolder.getUser().getId())
                .setEntityType(comment.getEntityType())
                .setEntityId(comment.getEntityId())
                .setData("postId", discussPostId); //由于对事件实体方法进行了修改，每次设置完属性后依旧返回的是对象，因此可以连续设置，将所有属性都设置完
        if(comment.getEntityType() == ENTITY_TYPE_POST){
            DiscussPost target = discussPostService.findDiscussPostById(comment.getEntityId());
            event.setEntityUserId(target.getUserId());
        }else if(comment.getEntityType() == ENTITY_TYPE_COMMENT){
            Comment target = commentService.findCommentById(comment.getEntityId());
            event.setEntityUserId(target.getUserId()); //获取作者的id
        }
        eventProducer.fireEvent(event); //触发事件

        if(comment.getEntityType() == ENTITY_TYPE_POST){ //如果评论的是帖子
            //触发发帖事件 //将新发布的帖子 存入ES服务器里（用于搜索引擎搜索）
            event = new Event()
                    .setTopic(TOPIC_PUBLISH) //设置发帖主题
                    .setUserId(comment.getUserId()) //设置用户id
                    .setEntityType(ENTITY_TYPE_POST) //事件关于哪个实体（是关于帖子的）
                    .setEntityId(discussPostId); //设置实体的id（帖子的id）

            eventProducer.fireEvent(event);//将对象传入， 触发事件

            //计算帖子的热度分数
            String redisKey = RedisKeyUtil.getPostScoreKey();
            redisTemplate.opsForSet().add(redisKey, discussPostId); //将新添加的帖子id存入set中， 使用set的原因是一个帖子无论操作多少次只需要存入一次
        }


        return "redirect:/discuss/detail/" + discussPostId; //重定向 到 帖子的详情页面
    }
}
