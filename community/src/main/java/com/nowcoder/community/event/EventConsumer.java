package com.nowcoder.community.event;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.nowcoder.community.config.ThreadPoolConfig;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Event;
import com.nowcoder.community.entity.Message;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.service.ElasticsearchService;
import com.nowcoder.community.service.MessageService;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.qiniu.common.QiniuException;
import com.qiniu.common.Zone;
import com.qiniu.http.Response;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

@Component
public class EventConsumer implements CommunityConstant { //事件消费者

    private static final Logger logger = LoggerFactory.getLogger(EventConsumer.class);

    @Autowired
    private MessageService messageService;

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private ElasticsearchService elasticsearchService;

    @Value("${wk.image.command}")
    private String wkImageCommand;

    @Value("${wk.image.storage}")
    private String wkImageStorage;

    @Value("${qiniu.key.access}")
    private String accessKey;

    @Value("${qiniu.key.secret}")
    private String secretKey;

    @Value("${qiniu.bucket.share.name}")
    private String shareBucketName;

    @Autowired
    private ThreadPoolTaskScheduler taskScheduler;


    @KafkaListener(topics = {TOPIC_COMMENT, TOPIC_LIKE, TOPIC_FOLLOW})
        public void handleCommentMessage(ConsumerRecord record){
            if(record == null || record.value() == null){
                logger.error("消息的内容为空!");
                return;
        }

            Event event = JSONObject.parseObject(record.value().toString(), Event.class);
            if(event == null){
                logger.error("消息格式错误!");
                return;
            }

            //发送站内通知
        Message message = new Message();
        message.setFromId(SYSTEM_USER_ID); //设置 发送通知用户ID ：系统
        message.setToId(event.getEntityUserId());// 设置 接收通知用户ID ： 某一个用户
        message.setConversationId(event.getTopic());//设置 发送通知的主题 ： comment、like、follow
        message.setCreateTime(new Date()); //设置发送通知的时间

        Map<String, Object> content = new HashMap<>(); //内容
        content.put("userId", event.getUserId());
        content.put("entityType", event.getEntityType());
        content.put("entityId", event.getEntityId());

        if(!event.getData().isEmpty()){ //判断事件对象是非空
            for(Map.Entry<String, Object> entry : event.getData().entrySet()){
                content.put(entry.getKey(), entry.getValue());
            }
        }

        message.setContent(JSONObject.toJSONString(content));
        messageService.addMessage(message); //存入消息


    }

    //消费发帖事件
    @KafkaListener(topics = {TOPIC_PUBLISH})
    public void handlePublishMessage(ConsumerRecord record){
        if(record == null || record.value() == null){
            logger.error("消息的内容为空!");
            return;
        }

        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if(event == null){
            logger.error("消息格式错误!");
            return;
        }

        DiscussPost post = discussPostService.findDiscussPostById(event.getEntityId()); //查询帖子
        elasticsearchService.saveDiscussPost(post);//
    }

    //消费删帖事件
    @KafkaListener(topics = {TOPIC_DELETE})
    public void handleDletehMessage(ConsumerRecord record){
        if(record == null || record.value() == null){
            logger.error("消息的内容为空!");
            return;
        }

        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if(event == null){
            logger.error("消息格式错误!");
            return;
        }

        elasticsearchService.deleteDiscussPost(event.getEntityId());//删除帖子
    }

    //消费分享事件
    @KafkaListener(topics = TOPIC_SHARE)
    public void handleShareMessage(ConsumerRecord record){
        if(record == null || record.value() == null){
            logger.error("消息的内容为空!");
            return;
        }

        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if(event == null){
            logger.error("消息格式错误!");
            return;
        }

        String htmlUrl = (String) event.getData().get("htmlUrl");
        String fileName = (String) event.getData().get("fileName");
        String suffix = (String) event.getData().get("suffix");

        String cmd = wkImageCommand + " --quality 75 "
                +htmlUrl + " " + wkImageStorage + "/" + fileName + suffix; //生成长图的命令

        try {
            Runtime.getRuntime().exec(cmd);
            logger.info("生成长图成功: " + cmd);
        }catch (IOException e){
            logger.error("生成长图失败: " + e.getMessage());
        }

        // 启用定时器，监视该图片， 一旦生成了，则上传至七牛云
        UploadTask task = new UploadTask(fileName, suffix);
        Future future = taskScheduler.scheduleAtFixedRate(task, 500);
        task.setFuture(future);

    }
    class UploadTask implements Runnable{

        //文件名称
        private String fileName;
        //文件后缀
        private String suffix;
        //启动任务的返回值
        private Future future;
        //开始时间
        private long startTime;
        //上传次数
        private int uploadTimes;

        public UploadTask(String fileName, String suffix){
            this.fileName = fileName;
            this.suffix = suffix;
            this.startTime = System.currentTimeMillis();
        }

        public void setFuture(Future future){
            this.future = future;
        }

        @Override
        public void run(){
            //生成图片失败
            if(System.currentTimeMillis() - startTime > 30000){ //生成图片时间超过30秒
                logger.error("执行时间过长,终止任务:" + fileName);
                future.cancel(true); //停止定时器
                return;
            }
            //上传失败
            if(uploadTimes >= 3){ //上传七牛云服务器的次数超过3次
                logger.error("上传次数过多,终止任务:" + fileName);
                future.cancel(true); //停止定时器
                return;
            }

            String path = wkImageStorage + "/" + fileName + suffix;
            File file = new File(path);
            if(file.exists()){
                logger.info(String.format("开始第%d次上传[%s].",++uploadTimes, fileName));
                //设置响应信息
                StringMap policy = new StringMap();
                policy.put("returnBody", CommunityUtil.getJSONString(0));

                //生成上传凭证
                Auth auth = Auth.create(accessKey, secretKey);
                String uploadToken = auth.uploadToken(shareBucketName, fileName, 3600, policy);
                //指定上传机房
                UploadManager manager = new UploadManager(new Configuration(Zone.zone0()));
                try{
                    //开始上传图片
                    Response response = manager.put(
                            path, fileName, uploadToken, null, "image/" + suffix, false);
                    //处理响应结果
                    JSONObject json = JSONObject.parseObject(response.bodyString());
                    if(json == null || json.get("code") == null || !json.get("code").toString().equals("0")) {
                        logger.info(String.format("第%d次上传失败[%s].", uploadTimes, fileName));
                    }else{
                        logger.info(String.format("第%d次上传成功[%s]", uploadTimes, fileName));
                        future.cancel(true);
                    }

                }catch (QiniuException e){
                    logger.info(String.format("第%d次上传失败[%d].", uploadTimes, fileName));
                }
            }else{
                logger.info("等待图片生成[" + fileName + "].");
            }

        }

    }


}
