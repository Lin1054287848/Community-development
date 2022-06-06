package com.nowcoder.community.quartz;

import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.service.ElasticsearchService;
import com.nowcoder.community.service.LikeService;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.RedisKeyUtil;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PostScoreRefreshJob implements Job, CommunityConstant {
    private static  final Logger logger = LoggerFactory.getLogger(PostScoreRefreshJob.class);

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private LikeService likeService;

    @Autowired
    private ElasticsearchService elasticsearchService;

    private static final Date epoch;
    static {
        try {
            epoch = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2014-08-01 00:00:00");
        }catch (ParseException e){
            throw new RuntimeException("初始化牛客纪元失败！", e);
        }
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException{
        String redisKey = RedisKeyUtil.getPostScoreKey();
        BoundSetOperations operations = redisTemplate.boundSetOps(redisKey);

        if(operations.size() == 0){
            logger.info("[任务取消] 没有需要刷新的帖子!");
            return;
        }

        logger.info("[任务开始] 正在刷新帖子分数:" + operations.size());
        while (operations.size() > 0){ //每调用的一个刷新方法，size-1
            this.refresh((Integer) operations.pop()); //每刷新一个帖子的方法 ，就弹出它
        }
        logger.info("[任务结束] 帖子分数刷新完毕!");
    }
    private void refresh(int postId){ //帖子分数刷新方法
        DiscussPost post = discussPostService.findDiscussPostById(postId);

        if(post == null){
            logger.error("帖子不存在: id = " + postId);
            return;
        }

        //是否加精
        boolean wonderful = post.getStatus() == 1;
        //评论数量
        int commentCount = post.getCommentCount();
        //点赞数量
        long likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_POST, postId);

        //计算帖子的分数权重
        //log(加精分数 + 评论数*10 + 点赞数*2)+(发布时间 - 社区成立时间)    //加精分数设为75
        double w = (wonderful ? 75 : 0) + commentCount * 10 + likeCount * 2;
        //分数 = 帖子的权重 + 距离天数
        double score = Math.log10(Math.max(w, 1)) +   // 使得w不小于1， 若w小于1则log10(w)为负数，不符合常理， 最多log10(w)等于0 ，不加分
                (post.getCreateTime().getTime() - epoch.getTime())/(1000 * 3600 * 24);  // .getTime()是毫秒数，当前时间与社区成立时间的 相差天数
        //更新帖子分数
        discussPostService.updateScore(postId, score);
        //同步搜索的数据
        post.setScore(score); //更新帖子的热度分数
        elasticsearchService.saveDiscussPost(post); //再将该实体（帖子）传入
    }


}
