package com.chatapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * DTO for group creation and update requests
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroupRequest {
    
    @NotBlank(message = "Group name is required")
    @Size(min = 1, max = 100, message = "Group name must be between 1 and 100 characters")
    private String name;
    
    @Size(max = 500, message = "Group description cannot exceed 500 characters")
    private String description;
}