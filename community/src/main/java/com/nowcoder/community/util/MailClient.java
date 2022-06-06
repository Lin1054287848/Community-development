package com.nowcoder.community.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

@Component //注解
public class MailClient {

    private static final Logger logger = LoggerFactory.getLogger(MailClient.class); //设置日志

    @Autowired
    private JavaMailSender mailSender; //注入当前的bean中，就可以直接使用

    @Value("${spring.mail.username}")
    private String from; //发件人

    public void sendMail(String to, String subject, String content){ //to发给谁 ， subject邮件的主题， content发送的内容

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message); //帮助类，去构建message更详细的内容
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true);
            mailSender.send(helper.getMimeMessage()); //发送邮件
        } catch (MessagingException e) {
            logger.error("发送邮件失败:" + e.getMessage());
        }
    }
}
