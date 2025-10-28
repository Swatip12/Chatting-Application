package com.chatapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for group information responses
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroupResponse {
    
    private String id;
    private String name;
    private String description;
    private String createdBy;
    private List<UserResponse> members;
    private int memberCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    /**
     * Constructor for basic group info without members list
     */
    public GroupResponse(String id, String name, String description, String createdBy, 
                        int memberCount, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.createdBy = createdBy;
        this.memberCount = memberCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}