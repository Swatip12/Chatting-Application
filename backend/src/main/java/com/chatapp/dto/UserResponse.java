package com.chatapp.dto;

import com.chatapp.entity.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for user response (without password)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    
    private Long id;
    private String username;
    private UserStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime lastSeen;
    
    /**
     * Constructor from User entity
     */
    public UserResponse(com.chatapp.entity.User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.status = user.getStatus();
        this.createdAt = user.getCreatedAt();
        this.lastSeen = user.getLastSeen();
    }
}