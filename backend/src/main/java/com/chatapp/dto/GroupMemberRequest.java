package com.chatapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

/**
 * DTO for adding/removing group members
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroupMemberRequest {
    
    @NotBlank(message = "Username is required")
    private String username;
}