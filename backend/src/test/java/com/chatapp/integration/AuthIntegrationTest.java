package com.chatapp.integration;

import com.chatapp.dto.UserLoginRequest;
import com.chatapp.dto.UserRegistrationRequest;
import com.chatapp.entity.User;
import com.chatapp.entity.UserStatus;
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

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureTestMvc
@ActiveProfiles("test")
@Transactional
public class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void registerUser_Success() throws Exception {
        UserRegistrationRequest request = new UserRegistrationRequest("testuser", "password123");

        mockMvc.perform(post("/api/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.user.username", is("testuser")))
                .andExpect(jsonPath("$.user.status", is("OFFLINE")));

        // Verify user was saved to database
        User savedUser = userRepository.findByUsername("testuser").orElse(null);
        assert savedUser != null;
        assert savedUser.getUsername().equals("testuser");
        assert passwordEncoder.matches("password123", savedUser.getPassword());
    }

    @Test
    void registerUser_DuplicateUsername_ReturnsError() throws Exception {
        // Create existing user
        User existingUser = new User("testuser", passwordEncoder.encode("password123"));
        userRepository.save(existingUser);

        UserRegistrationRequest request = new UserRegistrationRequest("testuser", "password123");

        mockMvc.perform(post("/api/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void loginUser_Success() throws Exception {
        // Create user
        User user = new User("testuser", passwordEncoder.encode("password123"));
        user.setStatus(UserStatus.OFFLINE);
        userRepository.save(user);

        UserLoginRequest request = new UserLoginRequest("testuser", "password123");

        mockMvc.perform(post("/api/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpected(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.user.username", is("testuser")))
                .andExpect(jsonPath("$.user.status", is("ONLINE")));

        // Verify user status was updated in database
        User updatedUser = userRepository.findByUsername("testuser").orElse(null);
        assert updatedUser != null;
        assert updatedUser.getStatus() == UserStatus.ONLINE;
    }

    @Test
    void loginUser_InvalidCredentials_ReturnsError() throws Exception {
        UserLoginRequest request = new UserLoginRequest("nonexistent", "wrongpassword");

        mockMvc.perform(post("/api/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void loginUser_WrongPassword_ReturnsError() throws Exception {
        // Create user
        User user = new User("testuser", passwordEncoder.encode("password123"));
        userRepository.save(user);

        UserLoginRequest request = new UserLoginRequest("testuser", "wrongpassword");

        mockMvc.perform(post("/api/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void logoutUser_Success() throws Exception {
        // Create and login user
        User user = new User("testuser", passwordEncoder.encode("password123"));
        user.setStatus(UserStatus.ONLINE);
        userRepository.save(user);

        mockMvc.perform(post("/api/logout")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }
}