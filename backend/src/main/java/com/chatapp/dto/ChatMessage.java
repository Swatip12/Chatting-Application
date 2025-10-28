package com.chatapp.dto;

import com.chatapp.entity.MessageType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * DTO for WebSocket chat messages
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
    
    private Long id;
    
    @NotBlank(message = "Sender username is required")
    private String senderUsername;
    
    private String receiverUsername;
    
    @NotBlank(message = "Message content is required")
    private String content;
    
    @NotNull(message = "Message type is required")
    private MessageType type;
    
    private LocalDateTime timestamp;
    
    private String groupId;
    
    /**
     * Constructor for private messages
     */
    public ChatMessage(String senderUsername, String receiverUsername, String content, MessageType type) {
        this.senderUsername = senderUsername;
        this.receiverUsername = receiverUsername;
        this.content = content;
        this.type = type;
        this.timestamp = LocalDateTime.now();
    }
    
    /**
     * Constructor for group messages
     */
    public ChatMessage(String senderUsername, String content, MessageType type, String groupId) {
        this.senderUsername = senderUsername;
        this.content = content;
        this.type = type;
        this.groupId = groupId;
        this.timestamp = LocalDateTime.now();
    }
}