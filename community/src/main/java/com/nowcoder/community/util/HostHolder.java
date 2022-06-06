package com.nowcoder.community.util;

import com.nowcoder.community.entity.User;
import org.springframework.stereotype.Component;

/**
 * 持有用户的信息，用于代替session对象
 */
@Component
public class HostHolder {
    private ThreadLocal<User> users = new ThreadLocal<>();//线程隔离用户

    public void setUsers(User user){ //外界传入user参数，存入ThreadLocal里
        users.set(user);
    }
    public User getUser(){ //从ThreadLocal里去出user返回
        return users.get();
    }
    public void clear(){ //当请求结束时，清除ThreadLocal的user
        users.remove();
    }

}
