package com.chatapp.controller;

import com.chatapp.dto.ApiResponse;
import com.chatapp.dto.ChatMessage;
import com.chatapp.dto.UserResponse;
import com.chatapp.service.MessageService;
import com.chatapp.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.util.List;

/**
 * REST controller for message history and retrieval operations
 */
@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class MessageController {

    private final MessageService messageService;
    private final UserService userService;

    /**
     * Get private message history between current user and another user
     * @param userId the other user's ID (as path variable)
     * @param limit maximum number of messages to retrieve (default 50, max 100)
     * @param authentication current user's authentication
     * @return list of messages between the two users
     */
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<List<ChatMessage>>> getPrivateMessageHistory(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int limit,
            Authentication authentication) {
        
        log.info("Getting private message history for user {} with limit {}", userId, limit);
        
        try {
            String currentUsername = authentication.getName();
            
            // Get current user ID
            UserResponse currentUser = userService.findByUsername(currentUsername);
            Long currentUserId = currentUser.getId();
            
            // Validate that the target user exists
            userService.findById(userId);
            
            // Get conversation history between the two users
            List<ChatMessage> messages = messageService.getConversationHistory(currentUserId, userId, limit);
            
            return ResponseEntity.ok(new ApiResponse<>(true, "Message history retrieved successfully", messages));
            
        } catch (Exception e) {
            log.error("Error retrieving private message history: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Failed to retrieve message history: " + e.getMessage(), null));
        }
    }

    /**
     * Get group message history
     * @param groupId the group ID
     * @param limit maximum number of messages to retrieve (default 50, max 100)
     * @param authentication current user's authentication
     * @return list of group messages
     */
    @GetMapping("/group/{groupId}")
    public ResponseEntity<ApiResponse<List<ChatMessage>>> getGroupMessageHistory(
            @PathVariable String groupId,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int limit,
            Authentication authentication) {
        
        log.info("Getting group message history for group {} with limit {}", groupId, limit);
        
        try {
            List<ChatMessage> messages = messageService.getGroupMessageHistory(groupId, limit);
            
            return ResponseEntity.ok(new ApiResponse<>(true, "Group message history retrieved successfully", messages));
            
        } catch (IllegalArgumentException e) {
            log.error("Invalid group ID: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Invalid group ID: " + e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error retrieving group message history: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Failed to retrieve group message history: " + e.getMessage(), null));
        }
    }


}