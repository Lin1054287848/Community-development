package com.nowcoder.community.service;

import com.nowcoder.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

@Service
public class LikeService {

    @Autowired
    private RedisTemplate redisTemplate;

    //点赞
    public void like(int userId, int entityType, int entityId, int entityUserId){ // entityType实体类型, entityId实体id entityUserId实体所属的用户
//        String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);//获取Key
//        boolean isMember = redisTemplate.opsForSet().isMember(entityLikeKey, userId);//判断该set中是否已经存在该内容 存在则返回true 不存在返回false
//        if(isMember){ //判断是否已经点赞
//            redisTemplate.opsForSet().remove(entityLikeKey, userId); //是，则删除该数据 ，表示取消赞
//        }else{
//            redisTemplate.opsForSet().add(entityLikeKey, userId);//否，则添加该数据 ，表示赞  储存类型为Set  传入 Key:Value ( Key:Set() )
//        }
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId); //获得某实体的点赞数量
                String userLikeKey = RedisKeyUtil.getUserLikeKey(entityUserId); //获得某用户的点赞数量

                boolean isMember = operations.opsForSet().isMember(entityLikeKey, userId);//判断当前用户有没有对该实体点赞

                operations.multi();//开启事务

                if(isMember){//如果已经赞过了
                    operations.opsForSet().remove(entityLikeKey, userId); //该贴子的点赞取消 ，即删除该用户的点赞记录
                    operations.opsForValue().decrement(userLikeKey); //该帖子的作者的被点赞 总数减一
                }else{
                    operations.opsForSet().add(entityLikeKey, userId); //给该贴子的点赞 ，即增加该用户的点赞记录
                    operations.opsForValue().increment(userLikeKey); //该帖子的作者的被点赞 总数加一
                }

                return operations.exec();
            }
        });

    }


        //查询某实体点赞的数量
    public long findEntityLikeCount(int entityType, int entityId) {
        String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId); //key
        return redisTemplate.opsForSet().size(entityLikeKey); //查询 该key 中 value（set）中的储存元素个数

    }

    //查询某人对某实体的点赞状态
    public int findEntityLikeStatus(int userId, int entityType, int entityId){
        String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
        return redisTemplate.opsForSet().isMember(entityLikeKey, userId) ? 1 : 0; //某人对某实体的点过赞 1 否则 0
    }

    //查询某个用户获得的赞
    public int findUserLikeCount(int userId){
        String userLikeKey = RedisKeyUtil.getUserLikeKey(userId);
        Integer count = (Integer) redisTemplate.opsForValue().get(userLikeKey);
        return count == null ? 0 : count.intValue();
    }


}
