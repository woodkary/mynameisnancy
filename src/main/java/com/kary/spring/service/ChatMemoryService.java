package com.kary.spring.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * @author karywoodOyo
 */
@Service
@Qualifier("chatMemoryService")
public class ChatMemoryService {
    @Autowired
    RedisTemplate<String, String> redisTemplate;
    final Lock lock = new ReentrantLock();
    @Autowired
    private ObjectMapper objectMapper;

    // 获取Redis中存储的List<ChatMessage>
    public List<ChatMessage> getChatMessages(String key) throws JsonProcessingException {
        List<String> messagesJson = redisTemplate.opsForList().range(key, 0, -1);
        List<ChatMessage> chatMessages = new ArrayList<>();

        if (messagesJson != null) {
            for (String messageJson : messagesJson) {
                ChatMessage chatMessage = objectMapper.readValue(messageJson, ChatMessage.class);
                chatMessages.add(chatMessage);
            }
        }

        return chatMessages;

    }
    // 存储ChatMessage到Redis
    @Async
    public void saveChatMessage(String key, String userMessage, String assistantMessage) {
        lock.lock();
        try {
            // 序列化用户消息和助手消息
            String userMessageJson = objectMapper.writeValueAsString(new ChatMessage("user", userMessage));
            String assistantMessageJson = objectMapper.writeValueAsString(new ChatMessage("assistant", assistantMessage));

            // 存储到 Redis
            redisTemplate.opsForList().rightPush(key, userMessageJson);
            redisTemplate.opsForList().rightPush(key, assistantMessageJson);

        } catch (JsonProcessingException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }

    public List<String> getConversationIds(Integer userId) {
        List<String> conversationIds = redisTemplate.opsForList().range("chatList:" + userId, 0, -1);
        if (conversationIds == null) {
            conversationIds = new ArrayList<>();
        }
        return conversationIds;
    }
}
