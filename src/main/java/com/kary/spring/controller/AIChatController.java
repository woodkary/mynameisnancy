package com.kary.spring.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
   Map<String, ChatClient> activeChatClients;
   @Autowired
   @Qualifier("chatMemoryService")
   ChatMemoryService chatMemoryService;
   @Autowired
   ThreadPoolTaskScheduler taskScheduler;

    /**
     * 根据用户id获取用户的会话id列表
     *
     * @param userId 用户id
     * @return 用户的会话id列表
     */
    @GetMapping("/getConversationID")
    public Map<String, Object> getConversationID(@RequestParam(value = "userId") Integer userId) {
        // 从redis中获取userId对应的conversationId
        List<String> conversationIds = chatMemoryService.getConversationIds(userId);
        return Map.of(
                "code", 200,
                "userId", userId,
                "conversationIds", conversationIds,
                "message", "获取成功"
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
    /**
     * 根据用户id和会话id获取某次聊天历史
     *
     * @param userId 用户id
     * @param conversationId 会话id
     * @return 聊天历史
     */
    @GetMapping("/ChatHistory")
    public Map<String, Object> getChatHistory(@RequestParam(value = "userId") Integer userId,
                                              @RequestParam(value = "conversationId") String conversationId) throws JsonProcessingException {
        // 从redis中获取userId和conversationId对应的历史消息 todo:先从客户端内存中获取
        List<ChatMessage> chatMessages = chatMemoryService.getChatMessages("chatMemories2:" + userId + ":" + conversationId);

        return Map.of(
                "code", 200,
                "userId", userId,
                "conversationId", conversationId,
                "chatMessages", chatMessages,
                "message", "获取成功"
        );

    }
    @PostMapping("/chat")
    public Map<String, Object> chat(@RequestBody ChatRequest chatRequest) throws JsonProcessingException {
        if (chatRequest.getContent() == null || chatRequest.getContent().isEmpty()) {
            return Map.of(
                    "code", 400,
                    "message", "内容不能为空"
            );
        }

        String clientKey = chatRequest.getUserId() + "_" + chatRequest.getConversationId();
        ChatClient chatClient = activeChatClients.get(clientKey);

        if (chatClient == null) {
            String key = "chatMemories2:" + chatRequest.getUserId() + ":" + chatRequest.getConversationId();
            List<ChatMessage> previousMessages = chatMemoryService.getChatMessages(key);

            ChatMemory chatMemory = new InMemoryChatMemory();
            for (ChatMessage message : previousMessages) {
                if (Objects.equals(message.getMessageType(), "user")) {
                    chatMemory.add(chatRequest.getConversationId(), List.of(new UserMessage(message.getContent())));
                } else {
                    chatMemory.add(chatRequest.getConversationId(), List.of(new AssistantMessage(message.getContent())));
                }
            }

            chatClient = ChatClient.builder(chatModel)
                    .defaultAdvisors(
                            new MessageChatMemoryAdvisor(chatMemory),
                            new SimpleLoggerAdvisor()
                    ).build();

            activeChatClients.put(clientKey, chatClient);
        }

        String assistedContent = chatClient.prompt()
                .user(chatRequest.getContent())
                .advisors(a -> a
                        .param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatRequest.getConversationId())
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 256)
                )
                .call()
                .content();

        String key = "chatMemories2:" + chatRequest.getUserId() + ":" + chatRequest.getConversationId();
        taskScheduler.submit(() -> chatMemoryService.saveChatMessage(key, chatRequest.getContent(), assistedContent));

        return Map.of(
                "code", 200,
                "message", assistedContent
        );
    }

}
