package com.chatapp.service;

import com.chatapp.dto.UserLoginRequest;
import com.chatapp.dto.UserRegistrationRequest;
import com.chatapp.dto.UserResponse;
import com.chatapp.entity.User;
import com.chatapp.entity.UserStatus;
import com.chatapp.exception.DuplicateUsernameException;
import com.chatapp.exception.InvalidCredentialsException;
import com.chatapp.exception.UserNotFoundException;
import com.chatapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service class for user management operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    /**
     * Register a new user
     * @param request user registration request
     * @return registered user response
     * @throws DuplicateUsernameException if username already exists
     */
    public UserResponse registerUser(UserRegistrationRequest request) {
        log.info("Attempting to register user with username: {}", request.getUsername());
        
        // Check if username already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            log.warn("Registration failed: Username '{}' already exists", request.getUsername());
            throw new DuplicateUsernameException("Username '" + request.getUsername() + "' is already taken");
        }
        
        // Create new user with encrypted password
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setStatus(UserStatus.OFFLINE);
        
        // Save user to database
        User savedUser = userRepository.save(user);
        log.info("User registered successfully with ID: {}", savedUser.getId());
        
        return new UserResponse(savedUser);
    }
    
    /**
     * Authenticate user login
     * @param request user login request
     * @return authenticated user response
     * @throws InvalidCredentialsException if credentials are invalid
     */
    public UserResponse authenticateUser(UserLoginRequest request) {
        log.info("Attempting to authenticate user: {}", request.getUsername());
        
        // Find user by username
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> {
                    log.warn("Authentication failed: User '{}' not found", request.getUsername());
                    return new InvalidCredentialsException("Invalid username or password");
                });
        
        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Authentication failed: Invalid password for user '{}'", request.getUsername());
            throw new InvalidCredentialsException("Invalid username or password");
        }
        
        // Update user status to online
        user.setStatus(UserStatus.ONLINE);
        User updatedUser = userRepository.save(user);
        
        log.info("User '{}' authenticated successfully", request.getUsername());
        return new UserResponse(updatedUser);
    }
    
    /**
     * Get all users
     * @return list of all users
     */
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        log.info("Retrieving all users");
        return userRepository.findAll().stream()
                .map(UserResponse::new)
                .collect(Collectors.toList());
    }
    
    /**
     * Get all online users
     * @return list of online users
     */
    @Transactional(readOnly = true)
    public List<UserResponse> getOnlineUsers() {
        log.info("Retrieving online users");
        return userRepository.findOnlineUsers().stream()
                .map(UserResponse::new)
                .collect(Collectors.toList());
    }
    
    /**
     * Update user status
     * @param userId user ID
     * @param status new status
     * @return updated user response
     * @throws UserNotFoundException if user not found
     */
    public UserResponse updateUserStatus(Long userId, UserStatus status) {
        log.info("Updating status for user ID {} to {}", userId, status);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found with ID: {}", userId);
                    return new UserNotFoundException("User not found with ID: " + userId);
                });
        
        user.setStatus(status);
        User updatedUser = userRepository.save(user);
        
        log.info("User status updated successfully for user ID: {}", userId);
        return new UserResponse(updatedUser);
    }
    
    /**
     * Update user status by username
     * @param username username
     * @param status new status
     * @return updated user response
     * @throws UserNotFoundException if user not found
     */
    public UserResponse updateUserStatus(String username, UserStatus status) {
        log.info("Updating status for user '{}' to {}", username, status);
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("User not found with username: {}", username);
                    return new UserNotFoundException("User not found with username: " + username);
                });
        
        user.setStatus(status);
        User updatedUser = userRepository.save(user);
        
        log.info("User status updated successfully for user: {}", username);
        return new UserResponse(updatedUser);
    }
    
    /**
     * Logout user (set status to offline)
     * @param userId user ID
     * @return updated user response
     */
    public UserResponse logoutUser(Long userId) {
        log.info("Logging out user with ID: {}", userId);
        return updateUserStatus(userId, UserStatus.OFFLINE);
    }
    
    /**
     * Find user by username
     * @param username username to search for
     * @return user response
     * @throws UserNotFoundException if user not found
     */
    @Transactional(readOnly = true)
    public UserResponse findByUsername(String username) {
        log.info("Finding user by username: {}", username);
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("User not found with username: {}", username);
                    return new UserNotFoundException("User not found with username: " + username);
                });
        
        return new UserResponse(user);
    }
    
    /**
     * Find user by ID
     * @param userId user ID
     * @return user response
     * @throws UserNotFoundException if user not found
     */
    @Transactional(readOnly = true)
    public UserResponse findById(Long userId) {
        log.info("Finding user by ID: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found with ID: {}", userId);
                    return new UserNotFoundException("User not found with ID: " + userId);
                });
        
        return new UserResponse(user);
    }
}