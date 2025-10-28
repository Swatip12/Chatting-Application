package com.chatapp.integration;

import com.chatapp.dto.ChatMessage;
import com.chatapp.dto.UserStatusMessage;
import com.chatapp.entity.MessageType;
import com.chatapp.entity.User;
import com.chatapp.entity.UserStatus;
import com.chatapp.repository.MessageRepository;
import com.chatapp.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
public class WebSocketIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private WebSocketStompClient stompClient;
    private StompSession stompSession;
    private User testUser1;
    private User testUser2;

    @BeforeEach
    void setUp() throws Exception {
        messageRepository.deleteAll();
        userRepository.deleteAll();

        // Create test users
        testUser1 = new User("testuser1", passwordEncoder.encode("password123"));
        testUser1.setStatus(UserStatus.ONLINE);
        testUser1 = userRepository.save(testUser1);

        testUser2 = new User("testuser2", passwordEncoder.encode("password123"));
        testUser2.setStatus(UserStatus.ONLINE);
        testUser2 = userRepository.save(testUser2);

        // Setup WebSocket client
        stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        String url = "ws://localhost:" + port + "/ws/chat";
        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("username", testUser1.getUsername());

        stompSession = stompClient.connect(url, connectHeaders, new StompSessionHandlerAdapter()).get(5, TimeUnit.SECONDS);
    }

    @Test
    void sendPrivateMessage_Success() throws Exception {
        // Setup message receiver
        BlockingQueue<ChatMessage> receivedMessages = new LinkedBlockingQueue<>();
        
        stompSession.subscribe("/user/" + testUser2.getUsername() + "/queue/messages", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return ChatMessage.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                receivedMessages.offer((ChatMessage) payload);
            }
        });

        // Send private message
        ChatMessage message = new ChatMessage();
        message.setSenderUsername(testUser1.getUsername());
        message.setReceiverUsername(testUser2.getUsername());
        message.setContent("Hello from integration test");
        message.setType(MessageType.CHAT);

        stompSession.send("/app/chat.sendMessage", message);

        // Verify message was received
        ChatMessage receivedMessage = receivedMessages.poll(5, TimeUnit.SECONDS);
        assertNotNull(receivedMessage);
        assertEquals("Hello from integration test", receivedMessage.getContent());
        assertEquals(testUser1.getUsername(), receivedMessage.getSenderUsername());
        assertEquals(testUser2.getUsername(), receivedMessage.getReceiverUsername());

        // Verify message was persisted to database
        Thread.sleep(1000); // Allow time for database save
        long messageCount = messageRepository.countPrivateMessages(testUser1.getId(), testUser2.getId());
        assertEquals(1, messageCount);
    }

    @Test
    void sendGroupMessage_Success() throws Exception {
        String groupId = "test-group";
        
        // Setup group message receiver
        BlockingQueue<ChatMessage> receivedMessages = new LinkedBlockingQueue<>();
        
        stompSession.subscribe("/topic/group/" + groupId, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return ChatMessage.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                receivedMessages.offer((ChatMessage) payload);
            }
        });

        // Send group message
        ChatMessage groupMessage = new ChatMessage();
        groupMessage.setSenderUsername(testUser1.getUsername());
        groupMessage.setContent("Hello group from integration test");
        groupMessage.setType(MessageType.CHAT);
        groupMessage.setGroupId(groupId);

        stompSession.send("/app/chat.sendGroupMessage", groupMessage);

        // Verify message was received
        ChatMessage receivedMessage = receivedMessages.poll(5, TimeUnit.SECONDS);
        assertNotNull(receivedMessage);
        assertEquals("Hello group from integration test", receivedMessage.getContent());
        assertEquals(testUser1.getUsername(), receivedMessage.getSenderUsername());
        assertEquals(groupId, receivedMessage.getGroupId());

        // Verify message was persisted to database
        Thread.sleep(1000); // Allow time for database save
        long messageCount = messageRepository.countByGroupId(groupId);
        assertEquals(1, messageCount);
    }

    @Test
    void userJoinChat_Success() throws Exception {
        // Setup user status receiver
        BlockingQueue<ChatMessage> receivedMessages = new LinkedBlockingQueue<>();
        
        stompSession.subscribe("/topic/public", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return ChatMessage.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                receivedMessages.offer((ChatMessage) payload);
            }
        });

        // Send user join message
        UserStatusMessage joinMessage = new UserStatusMessage();
        joinMessage.setUsername(testUser1.getUsername());
        joinMessage.setType(MessageType.JOIN);

        stompSession.send("/app/chat.addUser", joinMessage);

        // Verify join message was broadcast
        ChatMessage receivedMessage = receivedMessages.poll(5, TimeUnit.SECONDS);
        assertNotNull(receivedMessage);
        assertEquals(testUser1.getUsername() + " joined the chat", receivedMessage.getContent());
        assertEquals(MessageType.JOIN, receivedMessage.getType());

        // Verify user status was updated in database
        User updatedUser = userRepository.findById(testUser1.getId()).orElse(null);
        assertNotNull(updatedUser);
        assertEquals(UserStatus.ONLINE, updatedUser.getStatus());
    }

    @Test
    void userLeaveChat_Success() throws Exception {
        // Setup user status receiver
        BlockingQueue<ChatMessage> receivedMessages = new LinkedBlockingQueue<>();
        
        stompSession.subscribe("/topic/public", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return ChatMessage.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                receivedMessages.offer((ChatMessage) payload);
            }
        });

        // Send user leave message
        UserStatusMessage leaveMessage = new UserStatusMessage();
        leaveMessage.setUsername(testUser1.getUsername());
        leaveMessage.setType(MessageType.LEAVE);

        stompSession.send("/app/chat.removeUser", leaveMessage);

        // Verify leave message was broadcast
        ChatMessage receivedMessage = receivedMessages.poll(5, TimeUnit.SECONDS);
        assertNotNull(receivedMessage);
        assertEquals(testUser1.getUsername() + " left the chat", receivedMessage.getContent());
        assertEquals(MessageType.LEAVE, receivedMessage.getType());

        // Verify user status was updated in database
        User updatedUser = userRepository.findById(testUser1.getId()).orElse(null);
        assertNotNull(updatedUser);
        assertEquals(UserStatus.OFFLINE, updatedUser.getStatus());
    }

    @Test
    void connectionHandling_Success() throws Exception {
        // Test that connection is established
        assertTrue(stompSession.isConnected());

        // Test disconnection
        stompSession.disconnect();
        
        // Wait for disconnection
        Thread.sleep(1000);
        
        assertFalse(stompSession.isConnected());
    }
}