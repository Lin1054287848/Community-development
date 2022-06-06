package com.nowcoder.community.dao;

import com.nowcoder.community.entity.Comment;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CommentMapper {

    List<Comment> selectCommentsByEntity(int entityType, int entityId, int offset, int limit);//查询实体，entityType, entity为实体的条件, offset分页的条件, limit为每页显示行数的限制

    int selectCountByEntity(int entityType, int entityId); //查询数据的条数

    int insertComment(Comment comment); //增加评论

    Comment selectCommentById(int id); //根据id查询评论
}
