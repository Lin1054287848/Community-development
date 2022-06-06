package com.nowcoder.community.dao;


import com.nowcoder.community.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper {

    /**
     * 在接口中定义抽象方法的时候
     * （抽象方法的一般格式;public abstract 返回值类型 方法名（）；）
     * (1)但是在接口中抽象方法，修饰符必须是两个固定的关键字 public abstract
     * (2)其中public abstract 可以省略
     * (3)方法的三要素可以随意更改
     * public interface Myinterface {
     * 	//以下都可以
     * 	public abstract void method();
     * 	public void method1();
     * 	abstract void method2();
     * 	void method3();
     * }
     * @param id
     * @return
     */
    User selectById(int id);  //用id查询User //接口的抽象方法声明 不需要具体的方法体 {}用;代替

    User selectByName(String username);//用Name查询User

    User selectByEmail(String email);//用Email查询User

    int insertUser(User user);  //插入User

    int updateStatus(int id, int status); //修改User  ，status状态

    int updateHeader(int id, String headerUrl); // 更新头像， 头像图片的路径

    int updatePassword(int id, String password);// 更新密码
}
