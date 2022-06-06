package com.nowcoder.community.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.File;

@Configuration
public class WkConfig {

    private static final Logger logger = LoggerFactory.getLogger(WkConfig.class); //日志

    @Value("${wk.image.storage}") //通过@Value将外部的值动态注入到Bean中， 注入wk-image储存的路径
    private String wkImageStorage; //这里的值来自application.properties，spring boot启动时默认加载此文件 注入到wkImageStorage  配置外部文件属性

    @PostConstruct //初始化
    public void init(){
        //创建WK图片目录
        File file = new File(wkImageStorage); //file是一个目录 传入wkImageStorage为目录的路径
        if(!file.exists()){ //判断该目录是否存在
            file.mkdir();//不存在，则创建这个路径
            logger.info("创建WK图片目录: " + wkImageStorage);
        }
    }
}
