package com.nowcoder.community.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.Date;

@Document(indexName = "discusspost", type = "_doc", shards = 6, replicas = 3) //使其与ES搜索引擎进行关联
public class DiscussPost {

    @Id
    private int id; //帖子的id

    @Field(type = FieldType.Integer)
    private int userId; //用户id

    //对该内容"互联网校招"进行关联,对内容进行拆分, 对多个相关词汇进行搜索
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String title; //帖子的标题

    //对该内容"互联网校招"进行关联,对内容进行拆分, 对多个相关词汇进行搜索
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String content; //帖子的内容

    @Field(type = FieldType.Integer)
    private int type; //帖子的类型

    @Field(type = FieldType.Integer)
    private int status; //帖子的状态

    @Field(type = FieldType.Date)
    private Date createTime; //创建时间

    @Field(type = FieldType.Integer)
    private int commentCount; //评论的数量

    @Field(type = FieldType.Double)
    private double score; //帖子的分数

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public int getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(int commentCount) {
        this.commentCount = commentCount;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    @Override
    public String toString() {
        return "DiscussPost{" +
                "id=" + id +
                ", userId=" + userId +
                ", title='" + title + '\'' +
                ", content='" + content + '\'' +
                ", type=" + type +
                ", status=" + status +
                ", createTime=" + createTime +
                ", commentCount=" + commentCount +
                ", score=" + score +
                '}';
    }
}

