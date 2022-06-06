package com.nowcoder.community.dao;

import com.nowcoder.community.entity.DiscussPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DiscussPostMapper {

    List<DiscussPost> selectDiscussPosts(int userId, int offset, int limit, int orderMode);

    //@Param注解用于给参数取别名，
    //如果只有一个参数，并且在<if>里使用,则必须加别名
    int selectDiscussPostRows(@Param("userId") int userId); //声明查询帖子的行数的方法

    int insertDiscussPost(DiscussPost discussPost);//声明增加帖子的方法

    DiscussPost selectDiscussPostById(int id); //查询帖子的详细信息

    int updateCommentCount(int id, int commentCount);

    int updateType(int id, int type); //修改帖子类型 普通:0、置顶:1

    int updateStatus(int id, int status); //修改帖子状态 正常:0 加精:1 拉黑:2

    int updateScore(int id, double score); //更新帖子的热度分数


}
