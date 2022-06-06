package com.nowcoder.community.util;


public class RedisKeyUtil {

    private static final String SPLIT = ":";
    private static final String PREFIX_ENTITY_LIKE = "like:entity"; //前缀 某个帖子/评论的赞
    private static final String PREFIX_USER_LIKE = "like:user"; //某个用户的赞的前缀
    private static final String PREFIX_FOLLOWEE = "followee"; //A关注B    B被关注者  ——B目标
    private static final String PREFIX_FOLLOWER = "follower"; //A关注B  , A关注者 ——A粉丝
    private static final String PREFIX_KAPTCHA = "kaptcha"; //验证码
    private static final String PREFIX_TICKET = "ticket"; //登录凭证
    private static final String PREFIX_USER = "user"; //用户
    private static final String PREFIX_UV = "uv"; //独立访客数
    private static final String PREFIX_DAU = "dau"; //日活跃用户数
    private static final String PREFIX_POST = "post";

    //某个实体的赞
    //like:entity:entityType:entityId -> set(userId) //用set不用int的好处是 ，既可以得到点赞的数量，也可以得到具体点赞的人的id
    public static String getEntityLikeKey(int entityType, int entityId){//获取某个实体的赞的Key值
        return PREFIX_ENTITY_LIKE + SPLIT + entityType + entityId;
    }

    //某个用户的赞
    //like:user:userId -> int
    public static String getUserLikeKey(int userId){//获取某个用户的赞的Key值
        return PREFIX_USER_LIKE + SPLIT + userId;
    }

    //某个用户关注的实体
    //followee:userId:entityType -> zset(entityId, now)
    public static String getFolloweeKey(int userId, int entityType){//获取某个用户关注的实体的Key值
        return PREFIX_FOLLOWEE + SPLIT + userId + SPLIT + entityType;
    }

    //某个实体拥有的粉丝
    //follower:entityType:entityId -> zset(userId,now)
    public static String getFollowerKey(int entityType, int entityId){//获取某个实体拥有的粉丝的Key值
        return PREFIX_FOLLOWER + SPLIT + entityType + SPLIT + entityId;
    }

    //登录验证码
    public static String getKaptchaKey(String owner){ //获取登录验证码的Key值
        return  PREFIX_KAPTCHA + SPLIT + owner;      //owner为用户的临时凭证，因为此时用户还没有登录没有userId 只能使用临时凭证存入cookie来记录某一个用户正在登录
    }

    //登录的凭证
    public static String getTicketKey(String ticket){//获取登录的凭证的Key值
        return PREFIX_TICKET + SPLIT + ticket;
    }

    //用户
    public static String getUserKey(int userId){
        return PREFIX_USER + SPLIT + userId;
    }

    //单日UV  独立访客数
    public static String getUVKey(String date){
        return PREFIX_UV + SPLIT + date;
    }

    //区间UV  独立访客数
    public static String getUVKey(String startDate, String endDate){
        return PREFIX_UV + SPLIT + startDate + SPLIT + endDate;
    }

    //单日活跃用户数
    public static String getDAUKey(String date){
        return PREFIX_DAU + SPLIT + date;
    }

    //区间活跃用户数
    public static String getDAUKey(String startDate, String endDate){
        return PREFIX_DAU + SPLIT + startDate + SPLIT + endDate;
    }

    //产生热度分数产生变化的帖子分数
    public static String getPostScoreKey(){
        return PREFIX_POST + SPLIT + "score";
    }

}
