package com.nowcoder.community.controller;

import com.nowcoder.community.entity.Event;
import com.nowcoder.community.event.EventProducer;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

//http://localhost:8080/community/share?htmlUrl=https://www.nowcoder.com  分享图片的网址

@Controller
public class ShareController implements CommunityConstant {
    private static final Logger logger = LoggerFactory.getLogger(ShareController.class);

    @Autowired
    private EventProducer eventProducer;

    @Value("${community.path.domain}")
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Value("${wk.image.storage}")
    private String wkImageStorage;

    @Value("${qiniu.bucket.share.url}")
    private String shareBucketUrl;

    @RequestMapping(path = "/share", method = RequestMethod.GET)
    @ResponseBody
    public String share(String htmlUrl){
        //文件名
        String fileName = CommunityUtil.generateUUID();

        //异步生成长图
        Event event = new Event() //实例化事件
                .setTopic(TOPIC_SHARE) //设置主题
                .setData("htmlUrl", htmlUrl) //设置url地址
                .setData("fileName", fileName) //设置文件名
                .setData("suffix", ".png"); //设置后缀
        eventProducer.fireEvent(event); //触发事件

        //返回访问路径
        Map<String, Object> map = new HashMap<>();
//        map.put("shareUrl", domain + contextPath + "/share/image/" + fileName);  //上传七牛云服务器， 返回路径改变
        map.put("shareUrl",shareBucketUrl + "/" + fileName);
        return CommunityUtil.getJSONString(0, null, map); //map存放返回路径
    }

    //废弃
    //获取长图
    @RequestMapping(path = "/share/image/{fileName}", method = RequestMethod.GET)
    public void getShareImage(@PathVariable("fileName") String fileName, HttpServletResponse response){
        if(StringUtils.isBlank(fileName)){
            throw new IllegalArgumentException("文件名不能为空!");
        }

        response.setContentType("image/png");
        File file = new File(wkImageStorage + "/" + fileName + ".png");
        try { //读取图片
            OutputStream os = response.getOutputStream(); //获取字节流
            FileInputStream fis = new FileInputStream(file);//读取文件， 将文件转换为输入流
            byte[] buffer = new byte[1024]; //创建 缓冲区
            int b = 0;
            while((b = fis.read(buffer)) != -1){ //读到就写入
                os.write(buffer, 0, b);
            }
        }catch (IOException e){
            logger.error("获取长图失败: " + e.getMessage());
        }
    }


}
