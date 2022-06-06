package com.nowcoder.community.service;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.nowcoder.community.dao.DiscussPostMapper;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.util.SensitiveFilter;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class DiscussPostService {

    private static final Logger logger = LoggerFactory.getLogger(DiscussPostService.class); //创建一个logger日志 记录DiscussPostService.class的内容

    @Autowired
    private DiscussPostMapper discussPostMapper;

    @Autowired
    private SensitiveFilter sensitiveFilter;

    @Value("${caffeine.post.max-size}")
    private int maxSize;

    @Value("${caffeine.post.expire-seconds}")
    private int expireSeconds;

    //Caffeine核心接口 : Cache, LoadingCache, AsyncLoadingCache

    //帖子列表缓存
    private LoadingCache<String, List<DiscussPost>> postListCache; //当本地缓存没有某数据时， 其他线程等待，调用LoadingCache接口去数据库将该数据取回来

    //帖子总数缓存
    private LoadingCache<Integer, Integer> postRowsCache;

    @PostConstruct
    public void init(){
        //初始化帖子列表缓存
        postListCache = Caffeine.newBuilder()
                .maximumSize(maxSize) //设置缓存的大小
                .expireAfterWrite(expireSeconds, TimeUnit.SECONDS)//设置缓存的过期时间 180秒
                .build(new CacheLoader<String, List<DiscussPost>>() { //定义 查询数据库得到初始化数据的 办法
                    @Nullable
                    @Override
                    public List<DiscussPost> load(@NonNull String key) throws Exception {
                        if(key == null || key.length() == 0){
                            throw new IllegalArgumentException("参数错误！");
                        }

                        String[] params = key.split(":");
                        if(params == null || params.length != 2){
                            throw new IllegalArgumentException("参数错误！");
                        }
                        int offset = Integer.valueOf(params[0]);
                        int limit = Integer.valueOf(params[1]);

                        //二级缓存: Redis -> mysql (若redis中不存在，再访问数据库)

                        return discussPostMapper.selectDiscussPosts(0, offset, limit, 1);
                    }
                });

        //初始化帖子总数缓存
        postRowsCache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireSeconds, TimeUnit.SECONDS)
                .build(new CacheLoader<Integer, Integer>() {
                    @Nullable
                    @Override
                    public Integer load(@NonNull Integer key) throws Exception {
                        logger.debug("load post list from DB.");
                        return discussPostMapper.selectDiscussPostRows(key);
                    }
                });

    }


    public List<DiscussPost> findDiscussPosts(int userId, int offset, int limit, int orderMode){//查询某页的数据
        if(userId == 0 && orderMode == 1){
            return postListCache.get(offset + ":" + limit); //启用缓存， 从缓存中访问结果
        }
        logger.debug("load post list from DB.");
        return discussPostMapper.selectDiscussPosts(userId, offset, limit, orderMode); //从数据库中访问结果
    }

    public int findDiscussPostRows(int userId){ //查询该用户id的帖子有多少行 0是指所有的用户帖子
        if(userId == 0){
            return postRowsCache.get(userId);//启用缓存， 从缓存中访问结果
        }
        logger.debug("load post rows from DB.");
        return discussPostMapper.selectDiscussPostRows(userId);//从数据库中访问结果
    }

    public int addDiscussPost(DiscussPost post){
        if(post == null){
            throw new IllegalArgumentException("参数不能为空！");
        }

        //转义HTML标记
        post.setTitle(HtmlUtils.htmlEscape(post.getTitle()));
        post.setContent(HtmlUtils.htmlEscape(post.getContent()));

        //过滤敏感词
        post.setTitle(sensitiveFilter.filter(post.getTitle())); //过滤标题敏感词
        post.setContent(sensitiveFilter.filter(post.getContent()));//过滤内容敏感词

        return discussPostMapper.insertDiscussPost(post);
    }

    public DiscussPost findDiscussPostById(int id){  //查询帖子方法
        return discussPostMapper.selectDiscussPostById(id);
    }

    public int updateCommentCount(int id, int commentCount){
        return discussPostMapper.updateCommentCount(id, commentCount);
    }

    public int updateType(int id, int type){
        return discussPostMapper.updateType(id, type);
    }

    public int updateStatus(int id, int status){
        return discussPostMapper.updateStatus(id, status);
    }

    public int updateScore(int id, double score){
        return discussPostMapper.updateScore(id, score);
    }

}
