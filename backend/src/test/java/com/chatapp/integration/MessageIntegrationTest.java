package com.chatapp.integration;

import com.chatapp.entity.Message;
import com.chatapp.entity.MessageType;
import com.chatapp.entity.User;
import com.chatapp.entity.UserStatus;
import com.chatapp.repository.MessageRepository;
import com.chatapp.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureTestMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureTestMvc
@ActiveProfiles("test")
@Transactional
public class MessageIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private User user1;
    private User user2;

    @BeforeEach
    void setUp() {
        messageRepository.deleteAll();
        userRepository.deleteAll();

        // Create test users
        user1 = new User("user1", passwordEncoder.encode("password123"));
        user1.setStatus(UserStatus.ONLINE);
        user1 = userRepository.save(user1);

        user2 = new User("user2", passwordEncoder.encode("password123"));
        user2.setStatus(UserStatus.ONLINE);
        user2 = userRepository.save(user2);
    }

    @Test
    void getPrivateMessageHistory_Success() throws Exception {
        // Create test messages
        Message message1 = new Message(user1, user2, "Hello from user1", MessageType.CHAT);
        message1.setTimestamp(LocalDateTime.now().minusMinutes(10));
        messageRepository.save(message1);

        Message message2 = new Message(user2, user1, "Reply from user2", MessageType.CHAT);
        message2.setTimestamp(LocalDateTime.now().minusMinutes(5));
        messageRepository.save(message2);

        mockMvc.perform(get("/api/messages/{userId}", user2.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].senderUsername", is("user1")))
                .andExpect(jsonPath("$[0].receiverUsername", is("user2")))
                .andExpect(jsonPath("$[0].content", is("Hello from user1")))
                .andExpect(jsonPath("$[1].senderUsername", is("user2")))
                .andExpect(jsonPath("$[1].receiverUsername", is("user1")))
                .andExpect(jsonPath("$[1].content", is("Reply from user2")));
    }

    @Test
    void getGroupMessageHistory_Success() throws Exception {
        // Create test group messages
        Message groupMessage1 = new Message(user1, "Group message from user1", MessageType.CHAT, "group1");
        groupMessage1.setTimestamp(LocalDateTime.now().minusMinutes(10));
        messageRepository.save(groupMessage1);

        Message groupMessage2 = new Message(user2, "Group message from user2", MessageType.CHAT, "group1");
        groupMessage2.setTimestamp(LocalDateTime.now().minusMinutes(5));
        messageRepository.save(groupMessage2);

        mockMvc.perform(get("/api/messages/group/{groupId}", "group1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].senderUsername", is("user1")))
                .andExpect(jsonPath("$[0].content", is("Group message from user1")))
                .andExpect(jsonPath("$[0].groupId", is("group1")))
                .andExpect(jsonPath("$[1].senderUsername", is("user2")))
                .andExpect(jsonPath("$[1].content", is("Group message from user2")))
                .andExpect(jsonPath("$[1].groupId", is("group1")));
    }

    @Test
    void getMessageHistory_NoMessages_ReturnsEmptyList() throws Exception {
        mockMvc.perform(get("/api/messages/{userId}", user2.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getGroupMessageHistory_NonExistentGroup_ReturnsEmptyList() throws Exception {
        mockMvc.perform(get("/api/messages/group/{groupId}", "nonexistent")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getMessageHistory_InvalidUserId_ReturnsError() throws Exception {
        mockMvc.perform(get("/api/messages/{userId}", 999L)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void messageHistoryPagination_ReturnsLimitedResults() throws Exception {
        // Create many messages
        for (int i = 0; i < 60; i++) {
            Message message = new Message(user1, user2, "Message " + i, MessageType.CHAT);
            message.setTimestamp(LocalDateTime.now().minusMinutes(60 - i));
            messageRepository.save(message);
        }

        mockMvc.perform(get("/api/messages/{userId}", user2.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(50))); // Should be limited to 50
    }

    @Test
    void messageHistoryOrdering_ReturnsChronologicalOrder() throws Exception {
        // Create messages with specific timestamps
        Message oldMessage = new Message(user1, user2, "Old message", MessageType.CHAT);
        oldMessage.setTimestamp(LocalDateTime.now().minusHours(2));
        messageRepository.save(oldMessage);

        Message newMessage = new Message(user2, user1, "New message", MessageType.CHAT);
        newMessage.setTimestamp(LocalDateTime.now().minusMinutes(30));
        messageRepository.save(newMessage);

        mockMvc.perform(get("/api/messages/{userId}", user2.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].content", is("Old message"))) // Oldest first
                .andExpect(jsonPath("$[1].content", is("New message")));
    }
}