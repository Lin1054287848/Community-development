package com.nowcoder.community.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.text.SimpleDateFormat;

@Configuration //注解为 配置类
public class AlphaConfig {

    @Bean //方法名就是bean的名字simpleDateFormat
    public SimpleDateFormat simpleDateFormat(){ //日期
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    }
}
