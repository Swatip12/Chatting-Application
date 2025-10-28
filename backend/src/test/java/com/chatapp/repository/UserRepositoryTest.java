package com.chatapp.repository;

import com.chatapp.entity.User;
import com.chatapp.entity.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    private User testUser1;
    private User testUser2;

    @BeforeEach
    void setUp() {
        testUser1 = new User();
        testUser1.setUsername("user1");
        testUser1.setPassword("password1");
        testUser1.setStatus(UserStatus.ONLINE);

        testUser2 = new User();
        testUser2.setUsername("user2");
        testUser2.setPassword("password2");
        testUser2.setStatus(UserStatus.OFFLINE);

        entityManager.persistAndFlush(testUser1);
        entityManager.persistAndFlush(testUser2);
    }

    @Test
    void findByUsername_ExistingUser_ReturnsUser() {
        // When
        Optional<User> result = userRepository.findByUsername("user1");

        // Then
        assertTrue(result.isPresent());
        assertEquals("user1", result.get().getUsername());
        assertEquals(UserStatus.ONLINE, result.get().getStatus());
    }

    @Test
    void findByUsername_NonExistingUser_ReturnsEmpty() {
        // When
        Optional<User> result = userRepository.findByUsername("nonexistent");

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    void existsByUsername_ExistingUser_ReturnsTrue() {
        // When
        boolean result = userRepository.existsByUsername("user1");

        // Then
        assertTrue(result);
    }

    @Test
    void existsByUsername_NonExistingUser_ReturnsFalse() {
        // When
        boolean result = userRepository.existsByUsername("nonexistent");

        // Then
        assertFalse(result);
    }

    @Test
    void findByStatus_OnlineUsers_ReturnsOnlineUsers() {
        // When
        List<User> result = userRepository.findByStatus(UserStatus.ONLINE);

        // Then
        assertEquals(1, result.size());
        assertEquals("user1", result.get(0).getUsername());
        assertEquals(UserStatus.ONLINE, result.get(0).getStatus());
    }

    @Test
    void findByStatus_OfflineUsers_ReturnsOfflineUsers() {
        // When
        List<User> result = userRepository.findByStatus(UserStatus.OFFLINE);

        // Then
        assertEquals(1, result.size());
        assertEquals("user2", result.get(0).getUsername());
        assertEquals(UserStatus.OFFLINE, result.get(0).getStatus());
    }

    @Test
    void findOnlineUsers_ReturnsOnlineUsersOrderedByUsername() {
        // Given - Add another online user
        User user3 = new User();
        user3.setUsername("anotheruser");
        user3.setPassword("password3");
        user3.setStatus(UserStatus.ONLINE);
        entityManager.persistAndFlush(user3);

        // When
        List<User> result = userRepository.findOnlineUsers();

        // Then
        assertEquals(2, result.size());
        assertEquals("anotheruser", result.get(0).getUsername()); // Should be first alphabetically
        assertEquals("user1", result.get(1).getUsername());
        result.forEach(user -> assertEquals(UserStatus.ONLINE, user.getStatus()));
    }

    @Test
    void findAllExceptUser_ExcludesSpecifiedUser() {
        // When
        List<User> result = userRepository.findAllExceptUser(testUser1.getId());

        // Then
        assertEquals(1, result.size());
        assertEquals("user2", result.get(0).getUsername());
        assertNotEquals(testUser1.getId(), result.get(0).getId());
    }

    @Test
    void findOnlineUsersExceptUser_ExcludesSpecifiedUser() {
        // Given - Make user2 online as well
        testUser2.setStatus(UserStatus.ONLINE);
        entityManager.persistAndFlush(testUser2);

        // When
        List<User> result = userRepository.findOnlineUsersExceptUser(testUser1.getId());

        // Then
        assertEquals(1, result.size());
        assertEquals("user2", result.get(0).getUsername());
        assertEquals(UserStatus.ONLINE, result.get(0).getStatus());
        assertNotEquals(testUser1.getId(), result.get(0).getId());
    }

    @Test
    void findOnlineUsersExceptUser_NoOnlineUsersExceptSpecified_ReturnsEmpty() {
        // When (only user1 is online, and we exclude user1)
        List<User> result = userRepository.findOnlineUsersExceptUser(testUser1.getId());

        // Then
        assertTrue(result.isEmpty());
    }
}