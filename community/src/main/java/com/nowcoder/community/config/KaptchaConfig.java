package com.nowcoder.community.config;

import com.google.code.kaptcha.Producer;
import com.google.code.kaptcha.impl.DefaultKaptcha;
import com.google.code.kaptcha.util.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

@Configuration //注释为配置类
//随机验证码的配置工具类
public class KaptchaConfig {

    @Bean
    public Producer kaptchaProducer(){
        Properties properties = new Properties();
        properties.setProperty("kaptcha.image.width","100"); //设置随机验证码图片的宽度为100
        properties.setProperty("kaptcha.image.height","40"); //设置随机验证码图片的高度为40
        properties.setProperty("kaptcha.textproducer.font.size","32");//设置随机验证码图片的字体大小为32号
        properties.setProperty("kaptcha.textproducer.font.color","0,0,0");//设置随机验证码图片的字体颜色为(0,0,0)黑色
        properties.setProperty("kaptcha.textproducer.char.string","0123456789ABCDEFGHIGKLMNOPQRSTUVWXYZ");//设置随机验证码图片中的随机字符的取值范围
        properties.setProperty("kaptcha.textproducer.char.length","4");//设置随机验证码图片的随机字符的个数
        properties.setProperty("kaptcha.noise.impl","com.google.code.kaptcha.impl.NoNoise");//设置随机验证码图片的噪声干扰类。防止机器人暴力破解验证码

        DefaultKaptcha kaptcha = new DefaultKaptcha();
        Config config = new Config(properties);
        kaptcha.setConfig(config);
        return kaptcha;
    }
}
