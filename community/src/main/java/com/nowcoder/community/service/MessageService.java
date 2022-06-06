package com.nowcoder.community.service;

import com.nowcoder.community.dao.MessageMapper;
import com.nowcoder.community.entity.Message;
import com.nowcoder.community.util.SensitiveFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.util.List;

@Service
public class MessageService {

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private SensitiveFilter sensitiveFilter;

    public List<Message> findConversations(int userId, int offset, int limit){  //查询某用户的会话，并进行分页，返回列表
        return messageMapper.selectConversations(userId, offset, limit);
    }

    public int findConversationCount(int userId){//查询某用户的总会话数量
        return messageMapper.selectConversationCount(userId);
    }

    public List<Message> findLetters(String conversationId, int offset, int limit){ //查询某用户的私信，并进行分页，返回列表
        return messageMapper.selectLetters(conversationId, offset, limit);
    }


    public int findLetterCount(String conversationId){ //查询某用户的总私信数量
        return messageMapper.selectLetterCount(conversationId);
    }


    public int findLetterUnreadCount(int userId, String conversationId){ //查询私信未读数量
        return messageMapper.selectLetterUnreadCount(userId, conversationId);
    }

    public int addMessage(Message message){ //添加私信
        message.setContent(HtmlUtils.htmlEscape(message.getContent()));
        message.setContent(sensitiveFilter.filter(message.getContent()));
        return messageMapper.insertMessage(message);
    }

    public int readMessage(List<Integer> ids){ //读取消息后 改变消息状态
        return messageMapper.updateStatus(ids, 1);
    }

    public Message findLatestNotice(int userId, String topic){ //查询某个主题下最新的通知
        return messageMapper.selectLatestNotice(userId, topic);
    }

    public int findNoticeCount(int userId, String topic){ //查询某个主题所包含的通知的数量
        return messageMapper.selectNoticeCount(userId, topic);
    }

    public int findNoticeUnreadCount(int userId, String topic){ //未读的通知的数量
        return messageMapper.selectNoticeUnreadCount(userId, topic);
    }

    public List<Message> findNotices(int userId, String topic, int offset, int limit){ //通知详细列表
        return messageMapper.selectNotices(userId, topic, offset, limit);
    }

}
