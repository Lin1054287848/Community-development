package com.nowcoder.community.dao;

import org.springframework.stereotype.Repository;

@Repository("AlphaHibernate") //自定义Bean的名称
public class AlphaDaoHibernateImpl implements AlphaDao{
    @Override
    public String select(){
        return "Hibernate";
    }
}
