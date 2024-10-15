package com.kary.spring.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.kary.spring.entity.ChatMessage;
import com.kary.spring.service.ChatMemoryService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author karywoodOyo
 */
@Configuration
public class AIConfig {
    @Bean
    @Qualifier("chatMemories")
    public Map<Integer, ChatMemory> getChatMemoryMap() {
        return new ConcurrentHashMap<>();
    }
    @Bean
    @Qualifier("activeChatClients")
    public Map<Integer, ChatClient> getChatClients() {
        return new ConcurrentHashMap<>();
    }
    // 存放聊天记录
    // Map<userId, Map<conversationId, List<{messageType,content}>>
    @Bean
    @Qualifier("chatMessages")
    public Map<Integer, Map<String, List<ChatMessage>>> getChatMessages() {
        return new ConcurrentHashMap<>();
    }
    @Bean
    @Qualifier("chatMessagesLock")
    public Lock getChatMessagesLock() {
        return new ReentrantLock();
    }

    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration();
        redisStandaloneConfiguration.setHostName("47.121.136.84");
        redisStandaloneConfiguration.setPort(6379);
        redisStandaloneConfiguration.setPassword("redis_FRTWdK");
        redisStandaloneConfiguration.setDatabase(0);
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .commandTimeout(Duration.ofSeconds(2))  // 设置超时时间
                .build();
        return new LettuceConnectionFactory(redisStandaloneConfiguration,clientConfig);
    }

    @Bean
    @Qualifier("redisTemplate")
    public RedisTemplate<String, ChatMessage> redisTemplate() {
        RedisTemplate<String, ChatMessage> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory());
        // 设置序列化器
        // 使用 String 序列化器来序列化 key
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // 使用 JSON 序列化器来序列化 value 和 hash value
        // 配置自定义的Jackson ObjectMapper
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // 如果不需要类型信息，禁用类型标识
        objectMapper.setDefaultTyping(new ObjectMapper.DefaultTypeResolverBuilder(ObjectMapper.DefaultTyping.NON_FINAL)
                .init(JsonTypeInfo.Id.NONE, null)
                .inclusion(JsonTypeInfo.As.PROPERTY));

        // 将配置好的 ObjectMapper 直接传递给 Jackson2JsonRedisSerializer 的构造函数
        Jackson2JsonRedisSerializer<ChatMessage> serializer = new Jackson2JsonRedisSerializer<>(objectMapper,ChatMessage.class);

        // 设置Value的序列化器
        template.setValueSerializer(serializer);
        template.setHashValueSerializer(serializer);
        template.afterPropertiesSet();// 初始化操作
        return template;
    }

}
