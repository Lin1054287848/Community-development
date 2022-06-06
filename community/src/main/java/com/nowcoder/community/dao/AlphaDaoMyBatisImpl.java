package com.nowcoder.community.dao;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Repository
@Primary //优先被装配
public class AlphaDaoMyBatisImpl implements AlphaDao{
    @Override
    public String select(){
        return "MyBatis";
    }
}
