package com.nowcoder.community.util;




import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CommunityUtil {

    //生成随机字符串
    public static String generateUUID(){
        return UUID.randomUUID().toString().replaceAll("-","");//UUID.randomUUID().toString()得到一个随机字符串，
                                                                                //但该字符串中存在"-":，因此使用replaceAll("-","")将"-"都替换掉
    }
    /** MD5加密
     *  特点：
     *  只能加密，不能解密，相同的字符串每次加密都是同样的加密结果，容易被破解： hello -> abc123def456 ,
     *  解决方法：在输入的字符串后加入随机的字符串，再进行加密，以此来提升安全性
     *  如:hello + 3e4a8 -> abc123def456abc
     */
    public static String md5(String key){
        if(StringUtils.isBlank(key)){  //判断key是否为空 是 true 只有空格也是空
            return null;
        }
        return DigestUtils.md5DigestAsHex(key.getBytes()); //将传入的key值 加密成一个16进制的结果返回（但要求传入的参数为Byte类型，因此要key进行转换）
    }

    public static String getJSONString(int code, String msg, Map<String, Object> map){ //服务器向浏览器返回数据，将这些数据整合在一起，返回 code为编号, msg为提示,  map为业务数据
        JSONObject json = new JSONObject();
        json.put("code", code);//将传入的参数装入json对象中
        json.put("msg", msg);
        if(map != null){
            for (String key : map.keySet()){ //遍历map的key值
                json.put(key, map.get(key));
            }
        }
        return json.toString();
    }
    //根据输入参数不同 对getJSONString方法进行重载
    public static String getJSONString(int code, String msg){ //服务器向浏览器返回数据，将这些数据整合在一起，返回 code为编号, msg为提示,  map为业务数据
        return getJSONString(code, msg, null);
    }
    //根据输入参数不同 对getJSONString方法进行重载
    public static String getJSONString(int code){ //服务器向浏览器返回数据，将这些数据整合在一起，返回 code为编号, msg为提示,  map为业务数据
        return getJSONString(code, null, null);
    }

    public static void main(String[] args){   //main方法进行测试
        Map<String, Object> map = new HashMap<>();
        map.put("name", "张三");
        map.put("age",25);
        System.out.println(getJSONString(0, "ok", map)); //结果会获得一个json格式的字符串 {"msg":"ok","code":0,"name":"张三","age":25}

    }

}
