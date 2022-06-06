package com.nowcoder.community;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.BoundValueOperations;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.concurrent.TimeUnit;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class RedisTests {

    @Autowired
    private RedisTemplate redisTemplate;

    @Test
    public void testStrings() {
        String redisKey = "test:count"; //声明key

        redisTemplate.opsForValue().set(redisKey, 1); //在该key中存入值 1 opsForValue() 是String类型

        System.out.println(redisTemplate.opsForValue().get(redisKey));
        System.out.println(redisTemplate.opsForValue().increment(redisKey)); // increment将redisKey加1
        System.out.println(redisTemplate.opsForValue().decrement(redisKey));// decrement将redisKey减1
    }

    @Test
    public void testHashes() {
        String redisKey = "test:user";

        redisTemplate.opsForHash().put(redisKey, "id", 1);//存入hash类型的值
        redisTemplate.opsForHash().put(redisKey, "username", "zhangsan");

        System.out.println(redisTemplate.opsForHash().get(redisKey, "id"));
        System.out.println(redisTemplate.opsForHash().get(redisKey, "username"));
    }

    @Test
    public void testLists() {
        String redisKey = "test:ids";

        redisTemplate.opsForList().leftPush(redisKey, 101);//从列表中插入，从左边插入，，rightPush从右边插入
        redisTemplate.opsForList().leftPush(redisKey, 102);
        redisTemplate.opsForList().leftPush(redisKey, 103);

        System.out.println(redisTemplate.opsForList().size(redisKey)); //获取list 的key的数目
        System.out.println(redisTemplate.opsForList().index(redisKey, 0)); //获取第0个位置所对应的数据
        System.out.println(redisTemplate.opsForList().range(redisKey, 0, 2)); //获取从0到2范围的数据

        System.out.println(redisTemplate.opsForList().leftPop(redisKey)); //从列表中删除，从左边删除
        System.out.println(redisTemplate.opsForList().leftPop(redisKey));
        System.out.println(redisTemplate.opsForList().leftPop(redisKey));
    }

    @Test
    public void testSets() {
        String redisKey = "test:teachers";

        redisTemplate.opsForSet().add(redisKey, "刘备", "关羽", "张飞", "赵云", "诸葛亮");

        System.out.println(redisTemplate.opsForSet().size(redisKey));//获取set中 的key的数目
        System.out.println(redisTemplate.opsForSet().pop(redisKey));//从set中 随机弹出一个值
        System.out.println(redisTemplate.opsForSet().members(redisKey));//统计集合中所有的数据是什么
    }

    @Test
    public void testSortedSets() {
        String redisKey = "test:students";

        redisTemplate.opsForZSet().add(redisKey, "唐僧", 80);//添加sortlist类型中的数值
        redisTemplate.opsForZSet().add(redisKey, "悟空", 90);
        redisTemplate.opsForZSet().add(redisKey, "八戒", 50);
        redisTemplate.opsForZSet().add(redisKey, "沙僧", 70);
        redisTemplate.opsForZSet().add(redisKey, "白龙马", 60);

        System.out.println(redisTemplate.opsForZSet().zCard(redisKey));
        System.out.println(redisTemplate.opsForZSet().score(redisKey, "八戒"));
        System.out.println(redisTemplate.opsForZSet().reverseRank(redisKey, "八戒")); //根据score，默认从小到大排序，输出排名的位置reverseRank
        System.out.println(redisTemplate.opsForZSet().reverseRange(redisKey, 0, 2)); //根据score，倒序从大到小排序 0-2范围，输入列表reverseRange
    }

    @Test
    public void testKeys() {
        redisTemplate.delete("test:user");

        System.out.println(redisTemplate.hasKey("test:user"));

        redisTemplate.expire("test:students", 10, TimeUnit.SECONDS);
    }

    // 批量发送命令,节约网络开销.
    @Test
    public void testBoundOperations() {
        String redisKey = "test:count";
        BoundValueOperations operations = redisTemplate.boundValueOps(redisKey); //批量发送命令,节约网络开销.，防止多次访问rides
        operations.increment();
        operations.increment();
        operations.increment();
        operations.increment();
        operations.increment();
        System.out.println(operations.get());
    }

    // 编程式事务 为了能够缩小事务管理范围，精确的开启一个方法中的某些步骤进行事务管理
    @Test
    public void testTransaction() {
        Object result = redisTemplate.execute(new SessionCallback() {//匿名实现
            @Override
            public Object execute(RedisOperations redisOperations) throws DataAccessException { //定义事务范围redisOperations执行命令的对象，用其来管理事务
                String redisKey = "text:tx";


                redisOperations.multi(); //启用事务

                redisOperations.opsForSet().add(redisKey, "zhangsan");
                redisOperations.opsForSet().add(redisKey, "lisi");
                redisOperations.opsForSet().add(redisKey, "wangwu");

                System.out.println(redisOperations.opsForSet().members(redisKey));//[] //在事务中查询时 ，由于事务还没有提交，查询无效，因此数据库中的内容还没有更新，查询的结果与预期不符


                return redisOperations.exec();// 提交事务
            }
        });
        System.out.println(result);//result用来接收返回值  [1, 1, 1, [wangwu, zhangsan, lisi]]
    }

}
