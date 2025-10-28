package com.chatapp.repository;

import com.chatapp.entity.Message;
import com.chatapp.entity.MessageType;
import com.chatapp.entity.User;
import com.chatapp.entity.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class MessageRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private MessageRepository messageRepository;

    private User user1;
    private User user2;
    private User user3;
    private Message privateMessage1;
    private Message privateMessage2;
    private Message groupMessage1;
    private Message groupMessage2;

    @BeforeEach
    void setUp() {
        // Create users
        user1 = new User();
        user1.setUsername("user1");
        user1.setPassword("password1");
        user1.setStatus(UserStatus.ONLINE);

        user2 = new User();
        user2.setUsername("user2");
        user2.setPassword("password2");
        user2.setStatus(UserStatus.ONLINE);

        user3 = new User();
        user3.setUsername("user3");
        user3.setPassword("password3");
        user3.setStatus(UserStatus.OFFLINE);

        entityManager.persistAndFlush(user1);
        entityManager.persistAndFlush(user2);
        entityManager.persistAndFlush(user3);

        // Create private messages
        privateMessage1 = new Message();
        privateMessage1.setSender(user1);
        privateMessage1.setReceiver(user2);
        privateMessage1.setContent("Hello from user1 to user2");
        privateMessage1.setType(MessageType.CHAT);
        privateMessage1.setTimestamp(LocalDateTime.now().minusMinutes(10));

        privateMessage2 = new Message();
        privateMessage2.setSender(user2);
        privateMessage2.setReceiver(user1);
        privateMessage2.setContent("Reply from user2 to user1");
        privateMessage2.setType(MessageType.CHAT);
        privateMessage2.setTimestamp(LocalDateTime.now().minusMinutes(5));

        // Create group messages
        groupMessage1 = new Message();
        groupMessage1.setSender(user1);
        groupMessage1.setContent("Group message from user1");
        groupMessage1.setType(MessageType.CHAT);
        groupMessage1.setGroupId("group1");
        groupMessage1.setTimestamp(LocalDateTime.now().minusMinutes(8));

        groupMessage2 = new Message();
        groupMessage2.setSender(user2);
        groupMessage2.setContent("Group message from user2");
        groupMessage2.setType(MessageType.CHAT);
        groupMessage2.setGroupId("group1");
        groupMessage2.setTimestamp(LocalDateTime.now().minusMinutes(3));

        entityManager.persistAndFlush(privateMessage1);
        entityManager.persistAndFlush(privateMessage2);
        entityManager.persistAndFlush(groupMessage1);
        entityManager.persistAndFlush(groupMessage2);
    }

    @Test
    void findPrivateMessageHistory_ReturnsMessagesInDescendingOrder() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        List<Message> result = messageRepository.findPrivateMessageHistory(user1.getId(), user2.getId(), pageable);

        // Then
        assertEquals(2, result.size());
        // Should be in descending order by timestamp (most recent first)
        assertEquals("Reply from user2 to user1", result.get(0).getContent());
        assertEquals("Hello from user1 to user2", result.get(1).getContent());
    }

    @Test
    void findPrivateMessageHistory_NoMessages_ReturnsEmpty() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        List<Message> result = messageRepository.findPrivateMessageHistory(user1.getId(), user3.getId(), pageable);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void findBySenderId_ReturnsMessagesSentByUser() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        List<Message> result = messageRepository.findBySenderId(user1.getId(), pageable);

        // Then
        assertEquals(2, result.size()); // 1 private + 1 group message
        result.forEach(message -> assertEquals(user1.getId(), message.getSender().getId()));
    }

    @Test
    void findByReceiverId_ReturnsMessagesReceivedByUser() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        List<Message> result = messageRepository.findByReceiverId(user2.getId(), pageable);

        // Then
        assertEquals(1, result.size()); // Only private messages have receivers
        assertEquals("Hello from user1 to user2", result.get(0).getContent());
        assertEquals(user2.getId(), result.get(0).getReceiver().getId());
    }

    @Test
    void findGroupMessageHistory_ReturnsGroupMessagesInDescendingOrder() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        List<Message> result = messageRepository.findGroupMessageHistory("group1", pageable);

        // Then
        assertEquals(2, result.size());
        // Should be in descending order by timestamp (most recent first)
        assertEquals("Group message from user2", result.get(0).getContent());
        assertEquals("Group message from user1", result.get(1).getContent());
        result.forEach(message -> assertEquals("group1", message.getGroupId()));
    }

    @Test
    void findGroupMessageHistory_NonExistentGroup_ReturnsEmpty() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        List<Message> result = messageRepository.findGroupMessageHistory("nonexistent", pageable);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void findUserConversations_ReturnsAllUserMessages() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        List<Message> result = messageRepository.findUserConversations(user1.getId(), pageable);

        // Then
        assertEquals(2, result.size()); // Only private messages (group messages excluded)
        result.forEach(message -> {
            assertTrue(message.getSender().getId().equals(user1.getId()) || 
                      message.getReceiver().getId().equals(user1.getId()));
            assertNull(message.getGroupId());
        });
    }

    @Test
    void findByTypeOrderByTimestampDesc_ReturnsMessagesByType() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        List<Message> result = messageRepository.findByTypeOrderByTimestampDesc(MessageType.CHAT, pageable);

        // Then
        assertEquals(4, result.size()); // All messages are CHAT type
        result.forEach(message -> assertEquals(MessageType.CHAT, message.getType()));
    }

    @Test
    void countByGroupId_ReturnsCorrectCount() {
        // When
        Long count = messageRepository.countByGroupId("group1");

        // Then
        assertEquals(2L, count);
    }

    @Test
    void countByGroupId_NonExistentGroup_ReturnsZero() {
        // When
        Long count = messageRepository.countByGroupId("nonexistent");

        // Then
        assertEquals(0L, count);
    }

    @Test
    void countPrivateMessages_ReturnsCorrectCount() {
        // When
        Long count = messageRepository.countPrivateMessages(user1.getId(), user2.getId());

        // Then
        assertEquals(2L, count);
    }

    @Test
    void countPrivateMessages_NoMessages_ReturnsZero() {
        // When
        Long count = messageRepository.countPrivateMessages(user1.getId(), user3.getId());

        // Then
        assertEquals(0L, count);
    }

    @Test
    void findMessagesBetweenUsers_ReturnsMessagesInAscendingOrder() {
        // When
        List<Message> result = messageRepository.findMessagesBetweenUsers(user1.getId(), user2.getId(), 10);

        // Then
        assertEquals(2, result.size());
        // Should be in ascending order by timestamp (oldest first)
        assertEquals("Hello from user1 to user2", result.get(0).getContent());
        assertEquals("Reply from user2 to user1", result.get(1).getContent());
    }

    @Test
    void findMessagesBetweenUsers_WithLimit_ReturnsLimitedResults() {
        // When
        List<Message> result = messageRepository.findMessagesBetweenUsers(user1.getId(), user2.getId(), 1);

        // Then
        assertEquals(1, result.size());
        assertEquals("Hello from user1 to user2", result.get(0).getContent());
    }

    @Test
    void findGroupMessages_ReturnsMessagesInAscendingOrder() {
        // When
        List<Message> result = messageRepository.findGroupMessages("group1", 10);

        // Then
        assertEquals(2, result.size());
        // Should be in ascending order by timestamp (oldest first)
        assertEquals("Group message from user1", result.get(0).getContent());
        assertEquals("Group message from user2", result.get(1).getContent());
    }

    @Test
    void findGroupMessages_WithLimit_ReturnsLimitedResults() {
        // When
        List<Message> result = messageRepository.findGroupMessages("group1", 1);

        // Then
        assertEquals(1, result.size());
        assertEquals("Group message from user1", result.get(0).getContent());
    }
}