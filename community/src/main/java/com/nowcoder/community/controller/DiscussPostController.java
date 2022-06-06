package com.nowcoder.community.controller;

import com.nowcoder.community.entity.*;
import com.nowcoder.community.event.EventProducer;
import com.nowcoder.community.service.CommentService;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.service.LikeService;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.HostHolder;
import com.nowcoder.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.*;

import static com.nowcoder.community.util.CommunityConstant.*;

@Controller
@RequestMapping("/discuss")
public class DiscussPostController { //与帖子相关的业务

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private HostHolder hostHolder; //用于获取当前用户

    @Autowired
    private UserService userService;

    @Autowired
    private CommentService commentService;

    @Autowired
    private LikeService likeService; //注入点赞查询的相关功能

    @Autowired
    private EventProducer eventProducer;

    @Autowired
    private RedisTemplate redisTemplate;

    @RequestMapping(path = "/add", method = RequestMethod.POST)
    @ResponseBody
    public String addDiscussPost(String title, String content){
        User user = hostHolder.getUser(); //获取登录用户
        if(user == null){ //若还没有登录用户就不能发帖，返回错误提示
            return CommunityUtil.getJSONString(403,"你还没有登录！");
        }

        DiscussPost post = new DiscussPost(); //创建post对象

        post.setUserId(user.getId()); // 向当前对象中传入用户id
        post.setTitle(title);//传入帖子标题
        post.setContent(content);//传入帖子内容
        post.setCreateTime(new Date());//传入当前时间

        discussPostService.addDiscussPost(post); //将该对象添加到帖子中

        //触发发帖事件 //将新发布的帖子 存入ES服务器里（用于搜索引擎搜索）
        Event event = new Event()
                .setTopic(TOPIC_PUBLISH) //设置发帖主题
                .setUserId(user.getId()) //设置用户id
                .setEntityType(ENTITY_TYPE_POST) //事件关于哪个实体（是关于帖子的）
                .setEntityId(post.getId()); //设置实体的id（帖子的id）

        eventProducer.fireEvent(event);//将对象传入， 触发事件

        //计算帖子的热度分数
        String redisKey = RedisKeyUtil.getPostScoreKey();
        redisTemplate.opsForSet().add(redisKey, post.getId()); //将新添加的帖子id存入set中， 使用set的原因是一个帖子无论操作多少次只需要存入一次

        //报错的情况，将来统一处理
        return CommunityUtil.getJSONString(0,"发布成功!");
    }

    @RequestMapping(path = "/detail/{discussPostId}", method = RequestMethod.GET)
    public String getDiscussPost(@PathVariable("discussPostId") int discussPostId, Model model, Page page){
        //帖子
        DiscussPost post = discussPostService.findDiscussPostById(discussPostId); //获取帖子的数据
        model.addAttribute("post", post); //将帖子的数据传给模板
        //查询该帖子的作者
        User user = userService.findUserById(post.getUserId());
        model.addAttribute("user", user);

        //点赞数量
        long likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_POST, discussPostId);
        model.addAttribute("likeCount", likeCount);
        //点赞状态
        int likeStatus = hostHolder.getUser() == null ? 0 : likeService.findEntityLikeStatus(hostHolder.getUser().getId(), ENTITY_TYPE_POST, discussPostId);//用户没有登录显示帖子的点赞状态为0
        model.addAttribute("likeStatus", likeStatus);

        //评论的分页信息
        page.setLimit(5); //设置每页显示5条数据
        page.setPath("/discuss/detail/" + discussPostId);//设置帖子的路径
        page.setRows(post.getCommentCount());//设置帖子的评论数

        //评论：给帖子的评论
        //回复：给评论的评论

        //评论的列表
        List<Comment> commentList = commentService.findCommentsByEntity(ENTITY_TYPE_POST,
                post.getId(), page.getOffset(), page.getLimit()); //获取当前帖子中的所有评论
        //评论的VO（显示）列表
        List<Map<String, Object>> commentVoList = new ArrayList<>();
        if(commentList != null){
            for(Comment comment : commentList){
                //评论的VO
                Map<String, Object> commentVo = new HashMap<>();
                //评论
                commentVo.put("comment", comment); //存入评论
                //作者
                commentVo.put("user", userService.findUserById(comment.getUserId()));//存入评论的作者

                //点赞数量
                likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_COMMENT, comment.getId());
                commentVo.put("likeCount", likeCount);
                //点赞状态
                likeStatus = hostHolder.getUser() == null ? 0 : likeService.findEntityLikeStatus(hostHolder.getUser().getId(), ENTITY_TYPE_COMMENT, comment.getId());//用户没有登录显示帖子的点赞状态为0
                commentVo.put("likeStatus", likeStatus);

                //回复列表
                List<Comment> replyList = commentService.findCommentsByEntity(
                        ENTITY_TYPE_COMMENT, comment.getId(), 0, Integer.MAX_VALUE);
                //回复VO列表
                List<Map<String, Object>> replyVoList = new ArrayList<>();
                if(replyList != null){
                    for(Comment reply : replyList){
                        Map<String, Object> replyVo = new HashMap<>();
                        //回复
                        replyVo.put("reply", reply);
                        //作者
                        replyVo.put("user", userService.findUserById(reply.getUserId()));
                        //回复目标
                        User target = reply.getTargetId() == 0 ? null : userService.findUserById(reply.getTargetId());
                        replyVo.put("target", target);

                        //点赞数量
                        likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_COMMENT, reply.getId());
                        replyVo.put("likeCount", likeCount);
                        //点赞状态
                        likeStatus = hostHolder.getUser() == null ? 0 : likeService.findEntityLikeStatus(hostHolder.getUser().getId(), ENTITY_TYPE_COMMENT, reply.getId());//用户没有登录显示帖子的点赞状态为0
                        replyVo.put("likeStatus", likeStatus);

                        replyVoList.add(replyVo);
                    }
                }
                commentVo.put("replys", replyVoList);

                //回复数量
                int replyCount = commentService.findCommentCount(ENTITY_TYPE_COMMENT, comment.getId());//获取每一个评论(comment.getId())的回复(ENTITY_TYPE_COMMENT)数量
                commentVo.put("replyCount", replyCount);
                commentVoList.add(commentVo);
            }
        }
        model.addAttribute("comments", commentVoList);//最后将最终的结果放入model里传入模板


        return "/site/discuss-detail";
    }

    // 置顶、取消置顶
    @RequestMapping(path = "/top", method = RequestMethod.POST)
    @ResponseBody
    public String setTop(int id){ //帖子置顶
        DiscussPost discussPostById = discussPostService.findDiscussPostById(id);
        // 获取置顶状态，1为置顶，0为正常状态,1^1=0 0^1=1
        int type = discussPostById.getType()^1;
        discussPostService.updateType(id, type);
        // 返回的结果
        Map<String, Object> map = new HashMap<>();
        map.put("type", type);

        //触发发帖事件 //将新发布的帖子 存入ES服务器里（用于搜索引擎搜索）
        Event event = new Event()
                .setTopic(TOPIC_PUBLISH) //设置发帖主题
                .setUserId(hostHolder.getUser().getId()) //设置用户id
                .setEntityType(ENTITY_TYPE_POST) //事件关于哪个实体（是关于帖子的）
                .setEntityId(id); //设置实体的id（帖子的id）
        eventProducer.fireEvent(event);//将对象传入， 触发事件

        return CommunityUtil.getJSONString(0, null, map); //提示成功
    }

    // 加精、取消加精
    @RequestMapping(path = "/wonderful", method = RequestMethod.POST)
    @ResponseBody
    public String setWonderful(int id){ //帖子加精
        DiscussPost discussPostById = discussPostService.findDiscussPostById(id);
        int status = discussPostById.getStatus()^1;
        // 1为加精，0为正常， 1^1=0, 0^1=1
        discussPostService.updateStatus(id, status);
        // 返回的结果
        Map<String, Object> map = new HashMap<>();
        map.put("status", status);

        //计算帖子的热度分数
        String redisKey = RedisKeyUtil.getPostScoreKey();
        redisTemplate.opsForSet().add(redisKey, id); //将新添加的帖子id存入set中， 使用set的原因是一个帖子无论操作多少次只需要存入一次

        //触发发帖事件 //将新发布的帖子 存入ES服务器里（用于搜索引擎搜索）
        Event event = new Event()
                .setTopic(TOPIC_PUBLISH) //设置发帖主题
                .setUserId(hostHolder.getUser().getId()) //设置用户id
                .setEntityType(ENTITY_TYPE_POST) //事件关于哪个实体（是关于帖子的）
                .setEntityId(id); //设置实体的id（帖子的id）
        eventProducer.fireEvent(event);//将对象传入， 触发事件

        return CommunityUtil.getJSONString(0, null, map); //提示成功
    }

    //删帖
    @RequestMapping(path = "/delete", method = RequestMethod.POST)
    @ResponseBody
    public String setDelete(int id){ //帖子加精
        discussPostService.updateStatus(id, 2);

        //触发删帖事件
        Event event = new Event()
                .setTopic(TOPIC_DELETE) //设置删帖主题
                .setUserId(hostHolder.getUser().getId()) //设置用户id
                .setEntityType(ENTITY_TYPE_POST) //事件关于哪个实体（是关于帖子的）
                .setEntityId(id); //设置实体的id（帖子的id）
        eventProducer.fireEvent(event);//将对象传入， 触发事件

        return CommunityUtil.getJSONString(0); //提示成功
    }
}
