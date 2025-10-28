package com.chatapp.dto;

import com.chatapp.entity.MessageType;
import com.chatapp.entity.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * DTO for user status change messages (join/leave notifications)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserStatusMessage {
    
    @NotBlank(message = "Username is required")
    private String username;
    
    @NotNull(message = "Message type is required")
    private MessageType type;
    
    @NotNull(message = "User status is required")
    private UserStatus status;
    
    private LocalDateTime timestamp;
    
    private String groupId;
    
    public UserStatusMessage(String username, MessageType type, UserStatus status) {
        this.username = username;
        this.type = type;
        this.status = status;
        this.timestamp = LocalDateTime.now();
    }
    
    public UserStatusMessage(String username, MessageType type, UserStatus status, String groupId) {
        this.username = username;
        this.type = type;
        this.status = status;
        this.groupId = groupId;
        this.timestamp = LocalDateTime.now();
    }
}