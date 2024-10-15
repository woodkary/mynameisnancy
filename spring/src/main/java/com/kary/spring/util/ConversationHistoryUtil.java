package com.kary.spring.util;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.messages.Message;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

/**
 * @author karywoodOyo
 */
public class ConversationHistoryUtil {
    public static Map<String, List<Message>> getConversationHistory(ChatMemory chatMemory) throws NoSuchFieldException, IllegalAccessException {
        Class<InMemoryChatMemory> chatMemoryClass= InMemoryChatMemory.class;
        //使用反射访问其私有变量 Map<String, List<Message>> conversationHistory
        Field field = chatMemoryClass.getDeclaredField("conversationHistory");
        field.setAccessible(true);
        return (Map<String, List<Message>>) field.get(chatMemory);
    }
}
