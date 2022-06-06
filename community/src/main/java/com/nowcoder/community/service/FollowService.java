package com.nowcoder.community.service;

import com.nowcoder.community.entity.User;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class FollowService implements CommunityConstant {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private UserService userService;

    public void follow(int userId, int entityType, int entityId){ //关注
        redisTemplate.execute(new SessionCallback() { //编程式事务声明
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);//获得某个用户关注的实体的Key值
                String followerKey = RedisKeyUtil.getFollowerKey(entityType,entityId);//获得某个实体拥有的粉丝的Key值

                operations.multi(); //开启事务
                //redis存储 key-value值  (followeeKey, zset(entityId, now))
                operations.opsForZSet().add(followeeKey, entityId, System.currentTimeMillis()); //redis 操作的语句 关注数：用户关注的是实体
                operations.opsForZSet().add(followerKey, userId, System.currentTimeMillis()); //redis 操作的语句   粉丝数：实体拥有的用户
                return operations.exec();
            }
        });
    }

    public void unfollow(int userId, int entityType, int entityId){ //取消关注
        redisTemplate.execute(new SessionCallback() { //编程式事务声明
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);//获得某个用户关注的实体的Key值
                String followerKey = RedisKeyUtil.getFollowerKey(entityType,entityId);//获得某个实体拥有的粉丝的Key值

                operations.multi(); //开启事务
                //redis存储 key-value值  (followeeKey, zset(entityId, now))
                operations.opsForZSet().remove(followeeKey, entityId); //redis 操作的语句 关注数：用户关注的是实体
                operations.opsForZSet().remove(followerKey, userId); //redis 操作的语句   粉丝数：实体拥有的用户
                return operations.exec();
            }
        });
    }

    //查询关注的实体数量
    public long findFolloweeCount(int userId, int entityType){
        String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
        return redisTemplate.opsForZSet().zCard(followeeKey);
    }

    //查询实体的粉丝的数量
    public long findFollowerCount(int entityType, int entityId){
        String followerKey = RedisKeyUtil.getFollowerKey(entityType, entityId);
        return redisTemplate.opsForZSet().zCard(followerKey);
    }
    //查询当前用户是否已关注该实体
    public boolean hasFollowed(int userId, int entityType, int entityId){
        String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
        return redisTemplate.opsForZSet().score(followeeKey, entityId) != null; //如果不存在则返回false，存在返回true
    }

    //查询某用户关注的人
    public List<Map<String, Object>> findFollowees(int userId, int offset, int limit){
        String followeeKey = RedisKeyUtil.getFolloweeKey(userId, ENTITY_TYPE_USER);
        Set<Integer> targetIds = redisTemplate.opsForZSet().reverseRange(followeeKey, offset, offset+limit-1);//按关注时间倒序排列

        if(targetIds == null){ //判断该用户关注的人列表 是否为空
            return null;
        }

        List<Map<String, Object>> list = new ArrayList<>(); //将关注的ID转换为更详细的数据存放在集合里
        for(Integer targetId : targetIds){
            Map<String, Object> map = new HashMap<>();
            User user = userService.findUserById(targetId); //查询用户的信息
            map.put("user", user);
            Double score = redisTemplate.opsForZSet().score(followeeKey, targetId);
            map.put("followTime", new Date(score.longValue()));
            list.add(map);

        }
        return list;
    }

    //查询某用户的粉丝
    public List<Map<String, Object>> findFollowers(int userId, int offset, int limit){
        String followerKey = RedisKeyUtil.getFollowerKey(ENTITY_TYPE_USER, userId);
        Set<Integer> targetIds = redisTemplate.opsForZSet().reverseRange(followerKey, offset, offset+limit-1);

        if(targetIds == null){
            return null;
        }
        List<Map<String, Object>> list = new ArrayList<>(); //将关注的ID转换为更详细的数据存放在集合里
        for(Integer targetId : targetIds){
            Map<String, Object> map = new HashMap<>();
            User user = userService.findUserById(targetId); //查询用户的信息
            map.put("user", user);
            Double score = redisTemplate.opsForZSet().score(followerKey, targetId);
            map.put("followTime", new Date(score.longValue()));
            list.add(map);

        }
        return list;
    }

}
