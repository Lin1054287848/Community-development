package com.nowcoder.community.dao;

import com.nowcoder.community.entity.LoginTicket;
import org.apache.ibatis.annotations.*;

@Mapper
@Deprecated //设置该主件不推荐使用，意思就是不用了 //使用redis了
public interface LoginTicketMapper {

    @Insert({
            "insert into login_ticket(user_id,ticket,status,expired) ",
            "values(#{userId},#{ticket},#{status},#{expired})"
    })//将多个字符串自动拼接成一个cookie
    @Options(useGeneratedKeys = true, keyProperty = "id") //自动生成主键，将生成的值注入给 id ——自动生成id
    int insertLoginTicket(LoginTicket loginTicket); //插入凭证Ticket的方法 ，返回插入行数

    @Select({
            "select id,user_id,ticket,status,expired ",
            "from login_ticket where ticket=#{ticket}"
    })
    LoginTicket selectByTicket(String ticket); //查询凭证Ticket（唯一的标识）的方法

    @Update({
            "<script>",
            "update login_ticket set status=#{status} where ticket=#{ticket} ",
            "<if test=\"ticket!=null\"> ",
            "and 1=1 ",
            "</if>",
            "</script>"
    })
    int updateStatus(String ticket, int status);//修改凭证的状态
}
