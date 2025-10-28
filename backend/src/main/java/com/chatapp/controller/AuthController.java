package com.chatapp.controller;

import com.chatapp.dto.ApiResponse;
import com.chatapp.dto.UserLoginRequest;
import com.chatapp.dto.UserRegistrationRequest;
import com.chatapp.dto.UserResponse;
import com.chatapp.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;

/**
 * Controller for authentication operations
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class AuthController {
    
    private final UserService userService;
    
    /**
     * Register a new user
     * @param request user registration request
     * @return API response with user data
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody UserRegistrationRequest request) {
        log.info("Registration request received for username: {}", request.getUsername());
        
        UserResponse userResponse = userService.registerUser(request);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User registered successfully", userResponse));
    }
    
    /**
     * Authenticate user login
     * @param request user login request
     * @param session HTTP session
     * @return API response with user data
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<UserResponse>> login(
            @Valid @RequestBody UserLoginRequest request,
            HttpSession session) {
        log.info("Login request received for username: {}", request.getUsername());
        
        UserResponse userResponse = userService.authenticateUser(request);
        
        // Store user ID in session
        session.setAttribute("userId", userResponse.getId());
        session.setAttribute("username", userResponse.getUsername());
        
        return ResponseEntity.ok(ApiResponse.success("Login successful", userResponse));
    }
    
    /**
     * Logout user
     * @param session HTTP session
     * @return API response
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Object>> logout(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        String username = (String) session.getAttribute("username");
        
        if (userId != null) {
            log.info("Logout request received for user: {}", username);
            userService.logoutUser(userId);
            session.invalidate();
            return ResponseEntity.ok(ApiResponse.success("Logout successful"));
        }
        
        return ResponseEntity.ok(ApiResponse.success("No active session found"));
    }
    
    /**
     * Get current user session info
     * @param session HTTP session
     * @return API response with current user data
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("No active session"));
        }
        
        UserResponse userResponse = userService.findById(userId);
        return ResponseEntity.ok(ApiResponse.success(userResponse));
    }
}