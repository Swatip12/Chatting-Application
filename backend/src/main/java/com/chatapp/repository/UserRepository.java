package com.chatapp.repository;

import com.chatapp.entity.User;
import com.chatapp.entity.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for User entity operations
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * Find user by username
     * @param username the username to search for
     * @return Optional containing the user if found
     */
    Optional<User> findByUsername(String username);
    
    /**
     * Check if username exists
     * @param username the username to check
     * @return true if username exists, false otherwise
     */
    boolean existsByUsername(String username);
    
    /**
     * Find all users with a specific status
     * @param status the user status to filter by
     * @return list of users with the specified status
     */
    List<User> findByStatus(UserStatus status);
    
    /**
     * Find all online users
     * @return list of online users
     */
    @Query("SELECT u FROM User u WHERE u.status = 'ONLINE' ORDER BY u.username ASC")
    List<User> findOnlineUsers();
    
    /**
     * Find all users except the specified user
     * @param userId the user ID to exclude
     * @return list of users excluding the specified user
     */
    @Query("SELECT u FROM User u WHERE u.id != :userId ORDER BY u.status DESC, u.username ASC")
    List<User> findAllExceptUser(@Param("userId") Long userId);
    
    /**
     * Find online users except the specified user
     * @param userId the user ID to exclude
     * @return list of online users excluding the specified user
     */
    @Query("SELECT u FROM User u WHERE u.id != :userId AND u.status = 'ONLINE' ORDER BY u.username ASC")
    List<User> findOnlineUsersExceptUser(@Param("userId") Long userId);
}