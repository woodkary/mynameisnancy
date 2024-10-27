package com.kary.spring.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.kary.spring.entity.ChatMessage;
import com.kary.spring.service.ChatMemoryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
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
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

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
    @GetMapping("/getConversationIDList")
    public Map<String, Object> getConversationIDList(@RequestParam(value = "userId") Long userId) {
        System.out.println("userId:" + userId);
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
        Long userId;
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
    @GetMapping("/chat")
    public Map<String, Object> getChatHistory(@RequestParam(value = "userId") Long userId,
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
        //首先检查 chatRequest.getContent() 是否为空。如果为空，则返回错误响应：
        if (chatRequest.getContent() == null || chatRequest.getContent().isEmpty()) {
            return Map.of(
                    "code", 400,
                    "message", "内容不能为空"
            );
        }
        //聊天记录是否存在
        AtomicReference<Boolean> isNew = new AtomicReference<>(false);

        String clientKey = chatRequest.getUserId() + "_" + chatRequest.getConversationId();
        ChatClient chatClient = activeChatClients.computeIfAbsent(clientKey, key -> {
            // 从 Redis 或其他存储中获取聊天记录
            String memoryKey = "chatMemories2:" + chatRequest.getUserId() + ":" + chatRequest.getConversationId();
            List<ChatMessage> previousMessages = null;
            try {
                previousMessages = chatMemoryService.getChatMessages(memoryKey);
                if(!previousMessages.isEmpty()){
                    isNew.set(true);
                }
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }

            // 创建新的 ChatMemory 实例，并填充之前的消息
            ChatMemory chatMemory = new InMemoryChatMemory();
            for (ChatMessage message : previousMessages) {
                if (Objects.equals(message.getMessageType(), "user")) {
                    chatMemory.add(chatRequest.getConversationId(), List.of(new UserMessage(message.getContent())));
                } else {
                    chatMemory.add(chatRequest.getConversationId(), List.of(new AssistantMessage(message.getContent())));
                }
            }

            // 创建新的 ChatClient 实例并返回
            return ChatClient.builder(chatModel)
                    .defaultAdvisors(
                            new MessageChatMemoryAdvisor(chatMemory),
                            new SimpleLoggerAdvisor()
                    ).build();
        });

        // 调用AI
        String assistedContent = chatClient.prompt()
                .user(chatRequest.getContent())
                .advisors(a -> a
                        .param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatRequest.getConversationId())
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 256)
                )
                .call()
                .content();

        // 异步保存聊天记录
        String key = "chatMemories2:" + chatRequest.getUserId() + ":" + chatRequest.getConversationId();
        taskScheduler.submit(() -> chatMemoryService.saveChatMessage(key, chatRequest.getContent(), assistedContent));

        return Map.of(
                "code", 200,
                "message", assistedContent,
                "isNew", isNew.get()//是否是新的聊天记录
        );
    }

}
