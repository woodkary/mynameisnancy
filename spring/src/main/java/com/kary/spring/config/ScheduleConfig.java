package com.kary.spring.config;

import com.kary.spring.entity.ChatMessage;
import com.kary.spring.service.ChatMemoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

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
public class ScheduleConfig {
    @Autowired
    @Qualifier("chatMemoryService")
    ChatMemoryService chatMemoryService;
    // 显式锁对象
    @Autowired
    @Qualifier("chatMessagesLock")
    Lock lock;
    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(10); // 配置线程池大小
        return taskScheduler;
    }
    //建立定时任务，定时将chatMessages中的消息推送到redis中
    @Bean
    public Runnable scheduleTasks(
            @Autowired ThreadPoolTaskScheduler taskScheduler,
            @Autowired Map<Integer, Map<String, List<ChatMessage>>> chatMessages
    ) {
        Runnable task = () -> {
            // 使用锁来确保遍历和清空是原子操作
            lock.lock();
            try {
                chatMessages.forEach((userId, conversationMap) ->
                        conversationMap.forEach((conversationId, messages) -> {
                            chatMemoryService.addChatMemory(userId, conversationId, messages);
                        }));
                // 清空chatMessages缓存
                chatMessages.clear();
            } finally {
                lock.unlock();
            }
        };
        taskScheduler.scheduleAtFixedRate(task, Duration.ofSeconds(5));  // 每隔5秒执行一次
        return task;
    }
}
