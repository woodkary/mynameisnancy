package com.kary.spring.controller;

import com.kary.spring.entity.ChatMessage;
import com.kary.spring.service.ChatMemoryService;
import com.kary.spring.util.ConversationHistoryUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotNull;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

/**
 * @author karywoodOyo
 */
@RestController
public class AIChatController {
   @Autowired
   ChatModel chatModel;
   @Autowired
   @Qualifier("chatMemories")
   Map<Integer, ChatMemory> chatMemories;
   @Autowired
   @Qualifier("activeChatClients")
   Map<Integer, ChatClient> activeChatClients;
   //缓存所有用户的所有对话记录
   @Autowired
   @Qualifier("chatMessages")
   Map<Integer, Map<String, List<ChatMessage>>> chatMessages;
   @Autowired
   @Qualifier("chatMemoryService")
   ChatMemoryService chatMemoryService;
   @Autowired
   @Qualifier("chatMessagesLock")
   Lock lock;
   @GetMapping("/initChat")
   public Map<String, Object> initChat(@RequestParam(value = "userId") Integer userId) throws NoSuchFieldException, IllegalAccessException {
       //先尝试从缓存中获取chatMemory，如果没有则从redis中获取，如果redis中也没有则新建一个InMemoryChatMemory
        ChatMemory chatMemory = chatMemories.compute(userId, (k,chatMem) -> {
            if (chatMem == null) {
                System.out.println("缓存中没有chatMemory，从redis中获取");
                //从redis中获取
                chatMem = chatMemoryService.getAllChatMemories(userId);
                if (chatMem == null) {
                    System.out.println("redis中也没有，新建一个InMemoryChatMemory");
                    //如果redis中也没有，则新建一个InMemoryChatMemory
                    chatMem = new InMemoryChatMemory();
                }
            }
            //将chatMemory存入缓存
            return chatMem;
        });
        ChatClient chatClient=ChatClient
                .builder(chatModel)
                .defaultAdvisors(
                        new MessageChatMemoryAdvisor(chatMemory),
                        new SimpleLoggerAdvisor()
                ).build();
        activeChatClients.put(userId, chatClient);
        Map<String, List<Message>> conversationHistory = ConversationHistoryUtil.getConversationHistory(chatMemory);
        List<String> conversationIds = conversationHistory.keySet().stream().toList();
        return Map.of(
                "code", 200,
                "userId", userId,
                "conversationIds", conversationIds,
                "message", "初始化成功"
                );
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    static class ChatRequest {
        @NotNull
        Integer userId;
        @NotNull
        String conversationId;
        String content;
    }
    @PostMapping("/chat")
    public Map<String, Object> chat(@RequestBody ChatRequest chatRequest){
        ChatClient chatClient = activeChatClients.get(chatRequest.getUserId());
        if (chatClient == null) {
            return Map.of(
                    "code", 400,
                    "message", "用户未初始化"
            );
        }
        System.out.println("提问："+chatRequest.content);
        //得到机器人回复
        String assistedContent = chatClient.prompt()
                .user(chatRequest.content)
                .advisors(a->
                a.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatRequest.conversationId)
                .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 256)
                ).call().content();
        //保存对话记录
        System.out.println("回答："+assistedContent);
        lock.lock();
        try {
            chatMessages.compute(chatRequest.getUserId(), (k, v) -> {
                //userId如果没有对应的对话记录，则新建一个ConcurrentHashMap，保存这位用户的对话记录
                //conversationId如果没有对应的对话记录，则新建一个ArrayList，保存这条对话的记录
                if (v == null) {
                    v = new ConcurrentHashMap<>();
                    v.put(chatRequest.conversationId, new ArrayList<>(List.of(
                            new ChatMessage("user", chatRequest.content),
                            new ChatMessage("assistant", assistedContent)
                    )));
                } else {
                    v.compute(chatRequest.conversationId, (k1, v1) -> {
                        if (v1 == null) {
                            v1 = new ArrayList<>(List.of(
                                    new ChatMessage("user", chatRequest.content),
                                    new ChatMessage("assistant", assistedContent)
                            ));
                        } else {
                            v1.add(new ChatMessage("user", chatRequest.content));
                            v1.add(new ChatMessage("assistant", assistedContent));
                        }
                        return v1;
                    });
                }
                return v;
            });
        }finally {
            lock.unlock();
        }
        System.out.println("保存对话记录成功");
        return Map.of(
                "code", 200,
                "message", assistedContent
        );
    }
}
