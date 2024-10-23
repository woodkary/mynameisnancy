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
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
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
        try {
            // 序列化用户消息和助手消息
            String userMessageJson = objectMapper.writeValueAsString(new ChatMessage("user", userMessage));
            String assistantMessageJson = objectMapper.writeValueAsString(new ChatMessage("assistant", assistantMessage));

            // 使用管道来批量存储
            redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                connection.lPush(key.getBytes(), userMessageJson.getBytes());
                connection.lPush(key.getBytes(), assistantMessageJson.getBytes());
                return null;
            });

        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }



    public List<String> getConversationIds(Integer userId) {
        String pattern = "chatMemories2:" + userId + ":*";

        // 使用 Redis SCAN 而非 KEYS 来提高性能
        Set<String> keys = redisTemplate.execute((RedisConnection connection) -> {
            Set<String> result = new HashSet<>();
            Cursor<byte[]> cursor = connection.scan(ScanOptions.scanOptions().match(pattern).build());
            cursor.forEachRemaining(key -> result.add(new String(key)));
            return result;
        });

        // 如果 keys 为空，返回空列表而不是 null
        return Optional.ofNullable(keys)
                .map(k -> k.stream()
                        .map(key -> key.split(":")[2]) // 获取聊天标题部分
                        .collect(Collectors.toList()))
                .orElseGet(Collections::emptyList);
    }

}
