package com.chatapp.service;

import com.chatapp.dto.ChatMessage;
import com.chatapp.entity.Message;
import com.chatapp.entity.MessageType;
import com.chatapp.entity.User;
import com.chatapp.entity.UserStatus;
import com.chatapp.exception.UserNotFoundException;
import com.chatapp.repository.MessageRepository;
import com.chatapp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private MessageService messageService;

    private User sender;
    private User receiver;
    private Message testMessage;
    private ChatMessage chatMessage;

    @BeforeEach
    void setUp() {
        sender = new User();
        sender.setId(1L);
        sender.setUsername("sender");
        sender.setStatus(UserStatus.ONLINE);

        receiver = new User();
        receiver.setId(2L);
        receiver.setUsername("receiver");
        receiver.setStatus(UserStatus.ONLINE);

        testMessage = new Message();
        testMessage.setId(1L);
        testMessage.setSender(sender);
        testMessage.setReceiver(receiver);
        testMessage.setContent("Test message");
        testMessage.setType(MessageType.CHAT);
        testMessage.setTimestamp(LocalDateTime.now());

        chatMessage = new ChatMessage();
        chatMessage.setSenderUsername("sender");
        chatMessage.setReceiverUsername("receiver");
        chatMessage.setContent("Test message");
        chatMessage.setType(MessageType.CHAT);
    }

    @Test
    void saveAndBroadcastPrivateMessage_Success() {
        // Given
        when(userRepository.findByUsername("sender")).thenReturn(Optional.of(sender));
        when(userRepository.findByUsername("receiver")).thenReturn(Optional.of(receiver));
        when(messageRepository.save(any(Message.class))).thenReturn(testMessage);

        // When
        ChatMessage result = messageService.saveAndBroadcastPrivateMessage(chatMessage);

        // Then
        assertNotNull(result);
        assertEquals("sender", result.getSenderUsername());
        assertEquals("receiver", result.getReceiverUsername());
        assertEquals("Test message", result.getContent());
        assertEquals(MessageType.CHAT, result.getType());
        
        verify(userRepository).findByUsername("sender");
        verify(userRepository).findByUsername("receiver");
        verify(messageRepository).save(any(Message.class));
        verify(messagingTemplate).convertAndSendToUser(eq("receiver"), eq("/queue/messages"), any(ChatMessage.class));
        verify(messagingTemplate).convertAndSendToUser(eq("sender"), eq("/queue/messages"), any(ChatMessage.class));
    }

    @Test
    void saveAndBroadcastPrivateMessage_SenderNotFound_ThrowsException() {
        // Given
        when(userRepository.findByUsername("sender")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(UserNotFoundException.class, () -> {
            messageService.saveAndBroadcastPrivateMessage(chatMessage);
        });
        
        verify(userRepository).findByUsername("sender");
        verify(messageRepository, never()).save(any(Message.class));
        verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), any());
    }

    @Test
    void saveAndBroadcastPrivateMessage_ReceiverNotFound_ThrowsException() {
        // Given
        when(userRepository.findByUsername("sender")).thenReturn(Optional.of(sender));
        when(userRepository.findByUsername("receiver")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(UserNotFoundException.class, () -> {
            messageService.saveAndBroadcastPrivateMessage(chatMessage);
        });
        
        verify(userRepository).findByUsername("sender");
        verify(userRepository).findByUsername("receiver");
        verify(messageRepository, never()).save(any(Message.class));
        verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), any());
    }

    @Test
    void saveAndBroadcastGroupMessage_Success() {
        // Given
        ChatMessage groupMessage = new ChatMessage();
        groupMessage.setSenderUsername("sender");
        groupMessage.setContent("Group message");
        groupMessage.setType(MessageType.CHAT);
        groupMessage.setGroupId("group1");

        Message savedGroupMessage = new Message();
        savedGroupMessage.setId(1L);
        savedGroupMessage.setSender(sender);
        savedGroupMessage.setContent("Group message");
        savedGroupMessage.setType(MessageType.CHAT);
        savedGroupMessage.setGroupId("group1");
        savedGroupMessage.setTimestamp(LocalDateTime.now());

        when(userRepository.findByUsername("sender")).thenReturn(Optional.of(sender));
        when(messageRepository.save(any(Message.class))).thenReturn(savedGroupMessage);

        // When
        ChatMessage result = messageService.saveAndBroadcastGroupMessage(groupMessage);

        // Then
        assertNotNull(result);
        assertEquals("sender", result.getSenderUsername());
        assertEquals("Group message", result.getContent());
        assertEquals("group1", result.getGroupId());
        assertEquals(MessageType.CHAT, result.getType());
        
        verify(userRepository).findByUsername("sender");
        verify(messageRepository).save(any(Message.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/group/group1"), any(ChatMessage.class));
    }

    @Test
    void saveAndBroadcastGroupMessage_EmptyGroupId_ThrowsException() {
        // Given
        ChatMessage groupMessage = new ChatMessage();
        groupMessage.setSenderUsername("sender");
        groupMessage.setContent("Group message");
        groupMessage.setType(MessageType.CHAT);
        groupMessage.setGroupId("");

        when(userRepository.findByUsername("sender")).thenReturn(Optional.of(sender));

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            messageService.saveAndBroadcastGroupMessage(groupMessage);
        });
        
        verify(userRepository).findByUsername("sender");
        verify(messageRepository, never()).save(any(Message.class));
        verify(messagingTemplate, never()).convertAndSend(anyString(), any());
    }

    @Test
    void broadcastGroupMemberStatusChange_Success() {
        // When
        messageService.broadcastGroupMemberStatusChange("group1", "testuser", MessageType.JOIN);

        // Then
        verify(messagingTemplate).convertAndSend(eq("/topic/group/group1"), any(ChatMessage.class));
    }

    @Test
    void saveMessage_PrivateMessage_Success() {
        // Given
        when(messageRepository.save(any(Message.class))).thenReturn(testMessage);

        // When
        Message result = messageService.saveMessage(sender, receiver, "Test content", MessageType.CHAT, null);

        // Then
        assertNotNull(result);
        assertEquals(sender, result.getSender());
        assertEquals(receiver, result.getReceiver());
        assertEquals("Test content", result.getContent());
        assertEquals(MessageType.CHAT, result.getType());
        
        verify(messageRepository).save(any(Message.class));
    }

    @Test
    void saveMessage_GroupMessage_Success() {
        // Given
        Message groupMessage = new Message();
        groupMessage.setId(1L);
        groupMessage.setSender(sender);
        groupMessage.setContent("Group content");
        groupMessage.setType(MessageType.CHAT);
        groupMessage.setGroupId("group1");
        groupMessage.setTimestamp(LocalDateTime.now());

        when(messageRepository.save(any(Message.class))).thenReturn(groupMessage);

        // When
        Message result = messageService.saveMessage(sender, null, "Group content", MessageType.CHAT, "group1");

        // Then
        assertNotNull(result);
        assertEquals(sender, result.getSender());
        assertNull(result.getReceiver());
        assertEquals("Group content", result.getContent());
        assertEquals("group1", result.getGroupId());
        assertEquals(MessageType.CHAT, result.getType());
        
        verify(messageRepository).save(any(Message.class));
    }

    @Test
    void getConversationHistory_Success() {
        // Given
        List<Message> messages = Arrays.asList(testMessage);
        when(messageRepository.findMessagesBetweenUsers(1L, 2L, 50)).thenReturn(messages);

        // When
        List<ChatMessage> result = messageService.getConversationHistory(1L, 2L, 50);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("sender", result.get(0).getSenderUsername());
        assertEquals("receiver", result.get(0).getReceiverUsername());
        assertEquals("Test message", result.get(0).getContent());
        
        verify(messageRepository).findMessagesBetweenUsers(1L, 2L, 50);
    }

    @Test
    void getPrivateMessageHistory_Success() {
        // Given
        List<Message> messages = Arrays.asList(testMessage);
        when(userRepository.findByUsername("sender")).thenReturn(Optional.of(sender));
        when(userRepository.findByUsername("receiver")).thenReturn(Optional.of(receiver));
        when(messageRepository.findMessagesBetweenUsers(1L, 2L, 50)).thenReturn(messages);

        // When
        List<ChatMessage> result = messageService.getPrivateMessageHistory("sender", "receiver", 50);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("sender", result.get(0).getSenderUsername());
        assertEquals("receiver", result.get(0).getReceiverUsername());
        
        verify(userRepository).findByUsername("sender");
        verify(userRepository).findByUsername("receiver");
        verify(messageRepository).findMessagesBetweenUsers(1L, 2L, 50);
    }

    @Test
    void getPrivateMessageHistory_UserNotFound_ThrowsException() {
        // Given
        when(userRepository.findByUsername("sender")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(UserNotFoundException.class, () -> {
            messageService.getPrivateMessageHistory("sender", "receiver", 50);
        });
        
        verify(userRepository).findByUsername("sender");
        verify(messageRepository, never()).findMessagesBetweenUsers(anyLong(), anyLong(), anyInt());
    }

    @Test
    void getGroupMessageHistory_Success() {
        // Given
        Message groupMessage = new Message();
        groupMessage.setId(1L);
        groupMessage.setSender(sender);
        groupMessage.setContent("Group message");
        groupMessage.setType(MessageType.CHAT);
        groupMessage.setGroupId("group1");
        groupMessage.setTimestamp(LocalDateTime.now());

        List<Message> messages = Arrays.asList(groupMessage);
        when(messageRepository.findGroupMessages("group1", 50)).thenReturn(messages);

        // When
        List<ChatMessage> result = messageService.getGroupMessageHistory("group1", 50);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("sender", result.get(0).getSenderUsername());
        assertEquals("Group message", result.get(0).getContent());
        assertEquals("group1", result.get(0).getGroupId());
        
        verify(messageRepository).findGroupMessages("group1", 50);
    }

    @Test
    void getGroupMessageHistory_EmptyGroupId_ThrowsException() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            messageService.getGroupMessageHistory("", 50);
        });
        
        verify(messageRepository, never()).findGroupMessages(anyString(), anyInt());
    }

    @Test
    void broadcastUserStatusChange_Success() {
        // When
        messageService.broadcastUserStatusChange("testuser", MessageType.JOIN);

        // Then
        verify(messagingTemplate).convertAndSend(eq("/topic/public"), any(ChatMessage.class));
    }
}