package com.chatapp.integration;

import com.chatapp.dto.ChatMessage;
import com.chatapp.dto.UserLoginRequest;
import com.chatapp.dto.UserRegistrationRequest;
import com.chatapp.entity.MessageType;
import com.chatapp.repository.MessageRepository;
import com.chatapp.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureTestMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestMvc
@ActiveProfiles("test")
@Transactional
public class EndToEndIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private WebSocketStompClient stompClient;

    @BeforeEach
    void setUp() {
        messageRepository.deleteAll();
        userRepository.deleteAll();
        
        stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
    }

    @Test
    void completeUserJourneyTest() throws Exception {
        // Step 1: Register two users
        UserRegistrationRequest user1Registration = new UserRegistrationRequest("alice", "password123");
        UserRegistrationRequest user2Registration = new UserRegistrationRequest("bob", "password123");

        mockMvc.perform(post("/api/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(user1Registration)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.user.username", is("alice")));

        mockMvc.perform(post("/api/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(user2Registration)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.user.username", is("bob")));

        // Step 2: Login both users
        UserLoginRequest user1Login = new UserLoginRequest("alice", "password123");
        UserLoginRequest user2Login = new UserLoginRequest("bob", "password123");

        mockMvc.perform(post("/api/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(user1Login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.user.status", is("ONLINE")));

        mockMvc.perform(post("/api/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(user2Login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.user.status", is("ONLINE")));

        // Step 3: Verify users are listed as online
        mockMvc.perform(get("/api/users/online")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()", is(2)));

        // Step 4: Establish WebSocket connections for both users
        String wsUrl = "ws://localhost:" + port + "/ws/chat";
        
        StompHeaders aliceHeaders = new StompHeaders();
        aliceHeaders.add("username", "alice");
        StompSession aliceSession = stompClient.connect(wsUrl, aliceHeaders, new StompSessionHandlerAdapter()).get(5, TimeUnit.SECONDS);

        StompHeaders bobHeaders = new StompHeaders();
        bobHeaders.add("username", "bob");
        StompSession bobSession = stompClient.connect(wsUrl, bobHeaders, new StompSessionHandlerAdapter()).get(5, TimeUnit.SECONDS);

        assertTrue(aliceSession.isConnected());
        assertTrue(bobSession.isConnected());

        // Step 5: Setup message receivers
        BlockingQueue<ChatMessage> aliceMessages = new LinkedBlockingQueue<>();
        BlockingQueue<ChatMessage> bobMessages = new LinkedBlockingQueue<>();

        aliceSession.subscribe("/user/alice/queue/messages", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return ChatMessage.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                aliceMessages.offer((ChatMessage) payload);
            }
        });

        bobSession.subscribe("/user/bob/queue/messages", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return ChatMessage.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                bobMessages.offer((ChatMessage) payload);
            }
        });

        // Step 6: Alice sends message to Bob
        ChatMessage aliceToBob = new ChatMessage();
        aliceToBob.setSenderUsername("alice");
        aliceToBob.setReceiverUsername("bob");
        aliceToBob.setContent("Hello Bob!");
        aliceToBob.setType(MessageType.CHAT);

        aliceSession.send("/app/chat.sendMessage", aliceToBob);

        // Step 7: Verify Bob receives the message
        ChatMessage receivedByBob = bobMessages.poll(5, TimeUnit.SECONDS);
        assertNotNull(receivedByBob);
        assertEquals("Hello Bob!", receivedByBob.getContent());
        assertEquals("alice", receivedByBob.getSenderUsername());
        assertEquals("bob", receivedByBob.getReceiverUsername());

        // Step 8: Bob replies to Alice
        ChatMessage bobToAlice = new ChatMessage();
        bobToAlice.setSenderUsername("bob");
        bobToAlice.setReceiverUsername("alice");
        bobToAlice.setContent("Hi Alice!");
        bobToAlice.setType(MessageType.CHAT);

        bobSession.send("/app/chat.sendMessage", bobToAlice);

        // Step 9: Verify Alice receives the reply
        ChatMessage receivedByAlice = aliceMessages.poll(5, TimeUnit.SECONDS);
        assertNotNull(receivedByAlice);
        assertEquals("Hi Alice!", receivedByAlice.getContent());
        assertEquals("bob", receivedByAlice.getSenderUsername());
        assertEquals("alice", receivedByAlice.getReceiverUsername());

        // Step 10: Verify message history via REST API
        Thread.sleep(1000); // Allow time for database persistence

        // Get Alice's user ID for message history
        Long aliceId = userRepository.findByUsername("alice").get().getId();
        Long bobId = userRepository.findByUsername("bob").get().getId();

        mockMvc.perform(get("/api/messages/{userId}", bobId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()", is(2)))
                .andExpect(jsonPath("$[0].content", is("Hello Bob!")))
                .andExpect(jsonPath("$[1].content", is("Hi Alice!")));

        // Step 11: Test group messaging
        String groupId = "test-group";
        BlockingQueue<ChatMessage> groupMessages = new LinkedBlockingQueue<>();

        // Both users subscribe to group
        aliceSession.subscribe("/topic/group/" + groupId, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return ChatMessage.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                groupMessages.offer((ChatMessage) payload);
            }
        });

        bobSession.subscribe("/topic/group/" + groupId, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return ChatMessage.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                groupMessages.offer((ChatMessage) payload);
            }
        });

        // Alice sends group message
        ChatMessage groupMessage = new ChatMessage();
        groupMessage.setSenderUsername("alice");
        groupMessage.setContent("Hello everyone!");
        groupMessage.setType(MessageType.CHAT);
        groupMessage.setGroupId(groupId);

        aliceSession.send("/app/chat.sendGroupMessage", groupMessage);

        // Verify group message is received
        ChatMessage receivedGroupMessage = groupMessages.poll(5, TimeUnit.SECONDS);
        assertNotNull(receivedGroupMessage);
        assertEquals("Hello everyone!", receivedGroupMessage.getContent());
        assertEquals("alice", receivedGroupMessage.getSenderUsername());
        assertEquals(groupId, receivedGroupMessage.getGroupId());

        // Step 12: Verify group message history
        Thread.sleep(1000);
        mockMvc.perform(get("/api/messages/group/{groupId}", groupId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()", is(1)))
                .andExpect(jsonPath("$[0].content", is("Hello everyone!")));

        // Step 13: Disconnect users
        aliceSession.disconnect();
        bobSession.disconnect();

        // Step 14: Logout users
        mockMvc.perform(post("/api/logout")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Verify test completed successfully
        assertTrue(true, "Complete user journey test passed");
    }
}