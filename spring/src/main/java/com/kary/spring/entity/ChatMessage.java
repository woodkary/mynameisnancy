package com.kary.spring.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author karywoodOyo
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessage {
    String messageType;//user和assistant
    String content;
}
