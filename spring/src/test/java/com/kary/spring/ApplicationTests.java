package com.kary.spring;

import com.kary.spring.config.AIConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;

@SpringBootTest(classes = AIConfig.class)
class ApplicationTests {
    @Autowired
    @Qualifier("redisTemplate")
    RedisTemplate<String, Object> redisTemplate;
    @Test
    void contextLoads() {
    }
    @Test
    void testRecord() {
        String key="chatMemories:1:chat1";
        String value = "Q: 你叫什么名字？ | A: 我是一个AI助手。";
        redisTemplate.opsForList().rightPush(key, value);

        // LTRIM
        redisTemplate.opsForList().trim(key, -10, -1);// 只保留最新的10条记录

        List<Object> chats = redisTemplate.opsForList().range(key, 0, -1);
        assert chats != null;
    }

}
