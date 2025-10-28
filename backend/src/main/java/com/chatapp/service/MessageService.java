package com.chatapp.service;

import com.chatapp.dto.ChatMessage;
import com.chatapp.entity.Message;
import com.chatapp.entity.MessageType;
import com.chatapp.entity.User;
import com.chatapp.exception.UserNotFoundException;
import com.chatapp.repository.MessageRepository;
import com.chatapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service class for message persistence and real-time broadcasting
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Save and broadcast a private message between two users
     * @param chatMessage the chat message to save and broadcast
     * @return saved message as ChatMessage DTO
     */
    public ChatMessage saveAndBroadcastPrivateMessage(ChatMessage chatMessage) {
        log.info("Saving and broadcasting private message from {} to {}", 
                chatMessage.getSenderUsername(), chatMessage.getReceiverUsername());

        try {
            // Find sender and receiver users
            User sender = userRepository.findByUsername(chatMessage.getSenderUsername())
                    .orElseThrow(() -> new UserNotFoundException("Sender not found: " + chatMessage.getSenderUsername()));
            
            User receiver = userRepository.findByUsername(chatMessage.getReceiverUsername())
                    .orElseThrow(() -> new UserNotFoundException("Receiver not found: " + chatMessage.getReceiverUsername()));

            // Create and save message entity
            Message message = new Message(sender, receiver, chatMessage.getContent(), chatMessage.getType());
            Message savedMessage = messageRepository.save(message);

            // Convert back to DTO with saved message ID and timestamp
            ChatMessage savedChatMessage = convertToDTO(savedMessage);

            // Broadcast to receiver
            messagingTemplate.convertAndSendToUser(
                    chatMessage.getReceiverUsername(),
                    "/queue/messages",
                    savedChatMessage
            );

            // Send confirmation to sender
            messagingTemplate.convertAndSendToUser(
                    chatMessage.getSenderUsername(),
                    "/queue/messages",
                    savedChatMessage
            );

            log.info("Private message saved and broadcasted successfully with ID: {}", savedMessage.getId());
            return savedChatMessage;

        } catch (Exception e) {
            log.error("Error saving and broadcasting private message: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Save and broadcast a group message
     * @param chatMessage the group chat message to save and broadcast
     * @return saved message as ChatMessage DTO
     */
    public ChatMessage saveAndBroadcastGroupMessage(ChatMessage chatMessage) {
        log.info("Saving and broadcasting group message from {} to group {}", 
                chatMessage.getSenderUsername(), chatMessage.getGroupId());

        try {
            // Find sender user
            User sender = userRepository.findByUsername(chatMessage.getSenderUsername())
                    .orElseThrow(() -> new UserNotFoundException("Sender not found: " + chatMessage.getSenderUsername()));

            // Create and save group message entity
            Message message = new Message(sender, chatMessage.getContent(), chatMessage.getType(), chatMessage.getGroupId());
            Message savedMessage = messageRepository.save(message);

            // Convert back to DTO with saved message ID and timestamp
            ChatMessage savedChatMessage = convertToDTO(savedMessage);

            // Broadcast to group topic
            messagingTemplate.convertAndSend("/topic/group/" + chatMessage.getGroupId(), savedChatMessage);

            log.info("Group message saved and broadcasted successfully with ID: {}", savedMessage.getId());
            return savedChatMessage;

        } catch (Exception e) {
            log.error("Error saving and broadcasting group message: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Save a message to the database with automatic timestamp generation
     * @param sender the sender user
     * @param receiver the receiver user (null for group messages)
     * @param content the message content
     * @param messageType the type of message
     * @param groupId the group ID (null for private messages)
     * @return saved message entity
     */
    public Message saveMessage(User sender, User receiver, String content, MessageType messageType, String groupId) {
        log.info("Saving message from {} with type {}", sender.getUsername(), messageType);

        try {
            Message message;
            if (groupId != null) {
                // Group message
                message = new Message(sender, content, messageType, groupId);
            } else {
                // Private message
                message = new Message(sender, receiver, content, messageType);
            }

            Message savedMessage = messageRepository.save(message);
            log.info("Message saved successfully with ID: {} and timestamp: {}", 
                    savedMessage.getId(), savedMessage.getTimestamp());
            
            return savedMessage;

        } catch (Exception e) {
            log.error("Error saving message: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Get conversation history between two users with pagination
     * @param userId1 first user ID
     * @param userId2 second user ID
     * @param limit maximum number of messages to retrieve
     * @return list of messages in chronological order (oldest first)
     */
    @Transactional(readOnly = true)
    public List<ChatMessage> getConversationHistory(Long userId1, Long userId2, int limit) {
        log.info("Retrieving conversation history between user IDs {} and {} (limit: {})", userId1, userId2, limit);

        try {
            List<Message> messages = messageRepository.findMessagesBetweenUsers(userId1, userId2, limit);

            List<ChatMessage> chatMessages = messages.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            log.info("Retrieved {} messages between user IDs {} and {}", chatMessages.size(), userId1, userId2);
            return chatMessages;

        } catch (Exception e) {
            log.error("Error retrieving conversation history: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Get message history between two users by username
     * @param username1 first user's username
     * @param username2 second user's username
     * @param limit maximum number of messages to retrieve (default 50)
     * @return list of messages in chronological order
     */
    @Transactional(readOnly = true)
    public List<ChatMessage> getPrivateMessageHistory(String username1, String username2, int limit) {
        log.info("Retrieving message history between {} and {} (limit: {})", username1, username2, limit);

        try {
            // Find both users
            User user1 = userRepository.findByUsername(username1)
                    .orElseThrow(() -> new UserNotFoundException("User not found: " + username1));
            
            User user2 = userRepository.findByUsername(username2)
                    .orElseThrow(() -> new UserNotFoundException("User not found: " + username2));

            // Get messages between the two users
            return getConversationHistory(user1.getId(), user2.getId(), limit);

        } catch (Exception e) {
            log.error("Error retrieving message history: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Get group message history with proper filtering
     * @param groupId the group ID
     * @param limit maximum number of messages to retrieve (default 50)
     * @return list of group messages in chronological order (oldest first)
     */
    @Transactional(readOnly = true)
    public List<ChatMessage> getGroupMessageHistory(String groupId, int limit) {
        log.info("Retrieving group message history for group {} (limit: {})", groupId, limit);

        try {
            // Validate groupId
            if (groupId == null || groupId.trim().isEmpty()) {
                throw new IllegalArgumentException("Group ID cannot be null or empty");
            }

            List<Message> messages = messageRepository.findGroupMessages(groupId, limit);

            List<ChatMessage> chatMessages = messages.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            log.info("Retrieved {} messages for group {}", chatMessages.size(), groupId);
            return chatMessages;

        } catch (Exception e) {
            log.error("Error retrieving group message history: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Get group message history with pagination using Spring Data
     * @param groupId the group ID
     * @param pageable pagination information
     * @return list of group messages with pagination
     */
    @Transactional(readOnly = true)
    public List<ChatMessage> getGroupMessageHistoryWithPagination(String groupId, org.springframework.data.domain.Pageable pageable) {
        log.info("Retrieving group message history for group {} with pagination", groupId);

        try {
            // Validate groupId
            if (groupId == null || groupId.trim().isEmpty()) {
                throw new IllegalArgumentException("Group ID cannot be null or empty");
            }

            List<Message> messages = messageRepository.findGroupMessageHistory(groupId, pageable);

            List<ChatMessage> chatMessages = messages.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            log.info("Retrieved {} messages for group {} with pagination", chatMessages.size(), groupId);
            return chatMessages;

        } catch (Exception e) {
            log.error("Error retrieving group message history with pagination: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Broadcast user status change to all connected clients
     * @param username the username whose status changed
     * @param messageType JOIN or LEAVE
     */
    public void broadcastUserStatusChange(String username, MessageType messageType) {
        log.info("Broadcasting user status change: {} - {}", username, messageType);

        try {
            // Create status message
            ChatMessage statusMessage = new ChatMessage();
            statusMessage.setSenderUsername(username);
            statusMessage.setContent(messageType == MessageType.JOIN ? 
                    username + " joined the chat" : username + " left the chat");
            statusMessage.setType(messageType);
            statusMessage.setTimestamp(LocalDateTime.now());

            // Broadcast to public topic for all users to see
            messagingTemplate.convertAndSend("/topic/public", statusMessage);

            log.info("User status change broadcasted successfully for user: {}", username);

        } catch (Exception e) {
            log.error("Error broadcasting user status change: {}", e.getMessage(), e);
        }
    }

    /**
     * Convert Message entity to ChatMessage DTO
     * @param message the message entity
     * @return ChatMessage DTO
     */
    private ChatMessage convertToDTO(Message message) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setId(message.getId());
        chatMessage.setSenderUsername(message.getSender().getUsername());
        
        if (message.getReceiver() != null) {
            chatMessage.setReceiverUsername(message.getReceiver().getUsername());
        }
        
        chatMessage.setContent(message.getContent());
        chatMessage.setType(message.getType());
        chatMessage.setTimestamp(message.getTimestamp());
        chatMessage.setGroupId(message.getGroupId());
        
        return chatMessage;
    }
}