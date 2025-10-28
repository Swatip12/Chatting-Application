package com.chatapp.controller;

import com.chatapp.dto.ChatMessage;
import com.chatapp.dto.UserStatusMessage;
import com.chatapp.entity.MessageType;
import com.chatapp.entity.UserStatus;
import com.chatapp.service.MessageService;
import com.chatapp.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    @Mock
    private MessageService messageService;

    @Mock
    private UserService userService;

    @Mock
    private Principal principal;

    @Mock
    private SimpMessageHeaderAccessor headerAccessor;

    @InjectMocks
    private ChatController chatController;

    private ChatMessage chatMessage;
    private UserStatusMessage userStatusMessage;

    @BeforeEach
    void setUp() {
        chatMessage = new ChatMessage();
        chatMessage.setSenderUsername("sender");
        chatMessage.setReceiverUsername("receiver");
        chatMessage.setContent("Test message");
        chatMessage.setType(MessageType.CHAT);

        userStatusMessage = new UserStatusMessage();
        userStatusMessage.setUsername("testuser");
        userStatusMessage.setType(MessageType.JOIN);

        when(principal.getName()).thenReturn("authenticatedUser");
    }

    @Test
    void sendMessage_Success() {
        // Given
        ChatMessage savedMessage = new ChatMessage();
        savedMessage.setId(1L);
        savedMessage.setSenderUsername("authenticatedUser");
        savedMessage.setReceiverUsername("receiver");
        savedMessage.setContent("Test message");
        savedMessage.setType(MessageType.CHAT);
        savedMessage.setTimestamp(LocalDateTime.now());

        when(messageService.saveAndBroadcastPrivateMessage(any(ChatMessage.class))).thenReturn(savedMessage);

        // When
        chatController.sendMessage(chatMessage, principal);

        // Then
        verify(messageService).saveAndBroadcastPrivateMessage(argThat(message -> 
            message.getSenderUsername().equals("authenticatedUser") &&
            message.getReceiverUsername().equals("receiver") &&
            message.getContent().equals("Test message") &&
            message.getType().equals(MessageType.CHAT) &&
            message.getTimestamp() != null
        ));
    }

    @Test
    void sendMessage_ServiceThrowsException_HandlesGracefully() {
        // Given
        when(messageService.saveAndBroadcastPrivateMessage(any(ChatMessage.class)))
            .thenThrow(new RuntimeException("Service error"));

        // When
        chatController.sendMessage(chatMessage, principal);

        // Then
        verify(messageService).saveAndBroadcastPrivateMessage(any(ChatMessage.class));
        // Should not throw exception - error is logged and handled
    }

    @Test
    void sendGroupMessage_Success() {
        // Given
        ChatMessage groupMessage = new ChatMessage();
        groupMessage.setSenderUsername("sender");
        groupMessage.setContent("Group message");
        groupMessage.setType(MessageType.CHAT);
        groupMessage.setGroupId("group1");

        ChatMessage savedGroupMessage = new ChatMessage();
        savedGroupMessage.setId(1L);
        savedGroupMessage.setSenderUsername("authenticatedUser");
        savedGroupMessage.setContent("Group message");
        savedGroupMessage.setType(MessageType.CHAT);
        savedGroupMessage.setGroupId("group1");
        savedGroupMessage.setTimestamp(LocalDateTime.now());

        when(messageService.saveAndBroadcastGroupMessage(any(ChatMessage.class))).thenReturn(savedGroupMessage);

        // When
        chatController.sendGroupMessage(groupMessage, principal);

        // Then
        verify(messageService).saveAndBroadcastGroupMessage(argThat(message -> 
            message.getSenderUsername().equals("authenticatedUser") &&
            message.getContent().equals("Group message") &&
            message.getType().equals(MessageType.CHAT) &&
            message.getGroupId().equals("group1") &&
            message.getTimestamp() != null
        ));
    }

    @Test
    void sendGroupMessage_EmptyGroupId_DoesNotProcess() {
        // Given
        ChatMessage groupMessage = new ChatMessage();
        groupMessage.setSenderUsername("sender");
        groupMessage.setContent("Group message");
        groupMessage.setType(MessageType.CHAT);
        groupMessage.setGroupId("");

        // When
        chatController.sendGroupMessage(groupMessage, principal);

        // Then
        verify(messageService, never()).saveAndBroadcastGroupMessage(any(ChatMessage.class));
    }

    @Test
    void sendGroupMessage_NullGroupId_DoesNotProcess() {
        // Given
        ChatMessage groupMessage = new ChatMessage();
        groupMessage.setSenderUsername("sender");
        groupMessage.setContent("Group message");
        groupMessage.setType(MessageType.CHAT);
        groupMessage.setGroupId(null);

        // When
        chatController.sendGroupMessage(groupMessage, principal);

        // Then
        verify(messageService, never()).saveAndBroadcastGroupMessage(any(ChatMessage.class));
    }

    @Test
    void sendGroupMessage_ServiceThrowsException_HandlesGracefully() {
        // Given
        ChatMessage groupMessage = new ChatMessage();
        groupMessage.setSenderUsername("sender");
        groupMessage.setContent("Group message");
        groupMessage.setType(MessageType.CHAT);
        groupMessage.setGroupId("group1");

        when(messageService.saveAndBroadcastGroupMessage(any(ChatMessage.class)))
            .thenThrow(new RuntimeException("Service error"));

        // When
        chatController.sendGroupMessage(groupMessage, principal);

        // Then
        verify(messageService).saveAndBroadcastGroupMessage(any(ChatMessage.class));
        // Should not throw exception - error is logged and handled
    }

    @Test
    void addUser_Success() {
        // Given
        Map<String, Object> sessionAttributes = new HashMap<>();
        when(headerAccessor.getSessionAttributes()).thenReturn(sessionAttributes);

        // When
        chatController.addUser(userStatusMessage, headerAccessor, principal);

        // Then
        verify(userService).updateUserStatus("authenticatedUser", UserStatus.ONLINE);
        verify(messageService).broadcastUserStatusChange("authenticatedUser", MessageType.JOIN);
        assertEquals("authenticatedUser", sessionAttributes.get("username"));
    }

    @Test
    void addUser_ServiceThrowsException_HandlesGracefully() {
        // Given
        Map<String, Object> sessionAttributes = new HashMap<>();
        when(headerAccessor.getSessionAttributes()).thenReturn(sessionAttributes);
        when(userService.updateUserStatus(anyString(), any(UserStatus.class)))
            .thenThrow(new RuntimeException("Service error"));

        // When
        chatController.addUser(userStatusMessage, headerAccessor, principal);

        // Then
        verify(userService).updateUserStatus("authenticatedUser", UserStatus.ONLINE);
        // Should not throw exception - error is logged and handled
    }

    @Test
    void removeUser_Success() {
        // When
        chatController.removeUser(userStatusMessage, principal);

        // Then
        verify(userService).updateUserStatus("authenticatedUser", UserStatus.OFFLINE);
        verify(messageService).broadcastUserStatusChange("authenticatedUser", MessageType.LEAVE);
    }

    @Test
    void removeUser_ServiceThrowsException_HandlesGracefully() {
        // Given
        when(userService.updateUserStatus(anyString(), any(UserStatus.class)))
            .thenThrow(new RuntimeException("Service error"));

        // When
        chatController.removeUser(userStatusMessage, principal);

        // Then
        verify(userService).updateUserStatus("authenticatedUser", UserStatus.OFFLINE);
        // Should not throw exception - error is logged and handled
    }
}