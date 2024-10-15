package com.kary.spring.service;

import com.kary.spring.entity.ChatMessage;
import com.kary.spring.util.ConversationHistoryUtil;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author karywoodOyo
 */
@Service
@Qualifier("chatMemoryService")
public class ChatMemoryService {
    @Autowired
    @Qualifier("redisTemplate")
    RedisTemplate<String, ChatMessage> redisTemplate;

    //key格式为chatMemories:userId:conversationId
    public void addChatMemory(Integer userId, String conversationId, List<ChatMessage> newMessages) {
        String key = "chatMemories2:" + userId + ":" + conversationId;
        //将ChatMessage对象列表存入redis
        redisTemplate.opsForList().rightPushAll(key, newMessages);
    }

    public ChatMemory getAllChatMemories(Integer userId) {
        //key前缀为chatMemories:userId:
        String keyPrefix = "chatMemories2:" + userId + ":";
        //获取所有key
        Set<String> keys = redisTemplate.keys(keyPrefix + "*");
        //创建ChatMemory对象
        ChatMemory chatMemory = new InMemoryChatMemory();
        //遍历key，获取ChatMessage对象列表
        if (keys != null) {
            for (String key : keys) {
                //获取后缀，作为conversationId
                String conversationId = key.substring(keyPrefix.length());
                //获取ChatMessage对象列表
                List<ChatMessage> chatMessages = redisTemplate.opsForList().range(key, 0, -1);
                //将List<ChatMessage>转换为List<Message>
                if (chatMessages == null) {
                    chatMessages = List.of();
                }
                List<Message> messages = chatMessages.stream().map(chatMessage -> {
                    if (Objects.equals(chatMessage.getMessageType(), "user")) {
                        return new UserMessage(chatMessage.getContent());
                    } else {
                        return (Message) new AssistantMessage(chatMessage.getContent());
                    }
                }).toList();
                //将List<Message>存入chatMemory
                chatMemory.add(conversationId, messages);
            }
            return chatMemory;
        } else {
            return null;
        }
    }
}
