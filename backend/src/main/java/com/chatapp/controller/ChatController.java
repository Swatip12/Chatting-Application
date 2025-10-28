package com.chatapp.controller;

import com.chatapp.dto.ChatMessage;
import com.chatapp.dto.UserStatusMessage;
import com.chatapp.entity.MessageType;
import com.chatapp.entity.UserStatus;
import com.chatapp.service.MessageService;
import com.chatapp.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import javax.validation.Valid;
import java.security.Principal;
import java.time.LocalDateTime;

/**
 * WebSocket controller for handling real-time chat messages and user status updates
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final MessageService messageService;
    private final UserService userService;

    /**
     * Handle private message sending between users
     * Endpoint: /app/chat.sendMessage
     */
    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Valid @Payload ChatMessage chatMessage, Principal principal) {
        try {
            log.info("Received private message from {} to {}: {}", 
                    chatMessage.getSenderUsername(), chatMessage.getReceiverUsername(), chatMessage.getContent());
            
            // Set timestamp and ensure sender matches authenticated user
            chatMessage.setTimestamp(LocalDateTime.now());
            chatMessage.setSenderUsername(principal.getName());
            chatMessage.setType(MessageType.CHAT);
            
            // Save and broadcast message using MessageService
            messageService.saveAndBroadcastPrivateMessage(chatMessage);
            
            log.info("Private message processed successfully from {} to {}", 
                    chatMessage.getSenderUsername(), chatMessage.getReceiverUsername());
            
        } catch (Exception e) {
            log.error("Error processing private message: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle group message sending
     * Endpoint: /app/chat.sendGroupMessage
     */
    @MessageMapping("/chat.sendGroupMessage")
    public void sendGroupMessage(@Valid @Payload ChatMessage chatMessage, Principal principal) {
        try {
            log.info("Received group message from {} to group {}: {}", 
                    chatMessage.getSenderUsername(), chatMessage.getGroupId(), chatMessage.getContent());
            
            // Set timestamp and ensure sender matches authenticated user
            chatMessage.setTimestamp(LocalDateTime.now());
            chatMessage.setSenderUsername(principal.getName());
            chatMessage.setType(MessageType.CHAT);
            
            // Validate group ID
            if (chatMessage.getGroupId() == null || chatMessage.getGroupId().trim().isEmpty()) {
                log.error("Group ID is required for group messages");
                return;
            }
            
            // Save and broadcast group message using MessageService
            messageService.saveAndBroadcastGroupMessage(chatMessage);
            
            log.info("Group message processed successfully from {} to group {}", 
                    chatMessage.getSenderUsername(), chatMessage.getGroupId());
            
        } catch (Exception e) {
            log.error("Error processing group message: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle user joining the chat
     * Endpoint: /app/chat.addUser
     */
    @MessageMapping("/chat.addUser")
    public void addUser(@Valid @Payload UserStatusMessage userStatusMessage, 
                       SimpMessageHeaderAccessor headerAccessor,
                       Principal principal) {
        try {
            String username = principal.getName();
            log.info("User {} joining chat", username);
            
            // Add username to WebSocket session
            headerAccessor.getSessionAttributes().put("username", username);
            
            // Update user status to online
            userService.updateUserStatus(username, UserStatus.ONLINE);
            
            // Broadcast user join status using MessageService
            messageService.broadcastUserStatusChange(username, MessageType.JOIN);
            
            log.info("User {} successfully joined chat", username);
            
        } catch (Exception e) {
            log.error("Error adding user to chat: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle user leaving the chat
     * Endpoint: /app/chat.removeUser
     */
    @MessageMapping("/chat.removeUser")
    public void removeUser(@Valid @Payload UserStatusMessage userStatusMessage,
                          Principal principal) {
        try {
            String username = principal.getName();
            log.info("User {} leaving chat", username);
            
            // Update user status to offline
            userService.updateUserStatus(username, UserStatus.OFFLINE);
            
            // Broadcast user leave status using MessageService
            messageService.broadcastUserStatusChange(username, MessageType.LEAVE);
            
            log.info("User {} successfully left chat", username);
            
        } catch (Exception e) {
            log.error("Error removing user from chat: {}", e.getMessage(), e);
        }
    }
}