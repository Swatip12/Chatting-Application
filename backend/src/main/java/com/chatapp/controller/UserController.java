package com.chatapp.controller;

import com.chatapp.dto.ApiResponse;
import com.chatapp.dto.UserResponse;
import com.chatapp.entity.UserStatus;
import com.chatapp.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.List;

/**
 * Controller for user management operations
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class UserController {
    
    private final UserService userService;
    
    /**
     * Get all users
     * @return API response with list of all users
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
        log.info("Request received to get all users");
        
        List<UserResponse> users = userService.getAllUsers();
        
        return ResponseEntity.ok(ApiResponse.success("Users retrieved successfully", users));
    }
    
    /**
     * Get all online users
     * @return API response with list of online users
     */
    @GetMapping("/online")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getOnlineUsers() {
        log.info("Request received to get online users");
        
        List<UserResponse> onlineUsers = userService.getOnlineUsers();
        
        return ResponseEntity.ok(ApiResponse.success("Online users retrieved successfully", onlineUsers));
    }
    
    /**
     * Update user status
     * @param status new user status
     * @param session HTTP session
     * @return API response with updated user data
     */
    @PutMapping("/status")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserStatus(
            @RequestParam UserStatus status,
            HttpSession session) {
        
        Long userId = (Long) session.getAttribute("userId");
        String username = (String) session.getAttribute("username");
        
        if (userId == null) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("No active session"));
        }
        
        log.info("Request received to update status for user: {} to {}", username, status);
        
        UserResponse updatedUser = userService.updateUserStatus(userId, status);
        
        return ResponseEntity.ok(ApiResponse.success("User status updated successfully", updatedUser));
    }
    
    /**
     * Get user by ID
     * @param userId user ID
     * @return API response with user data
     */
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long userId) {
        log.info("Request received to get user by ID: {}", userId);
        
        UserResponse user = userService.findById(userId);
        
        return ResponseEntity.ok(ApiResponse.success("User retrieved successfully", user));
    }
    
    /**
     * Get user by username
     * @param username username
     * @return API response with user data
     */
    @GetMapping("/username/{username}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserByUsername(@PathVariable String username) {
        log.info("Request received to get user by username: {}", username);
        
        UserResponse user = userService.findByUsername(username);
        
        return ResponseEntity.ok(ApiResponse.success("User retrieved successfully", user));
    }
}