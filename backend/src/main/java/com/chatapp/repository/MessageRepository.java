package com.chatapp.repository;

import com.chatapp.entity.Message;
import com.chatapp.entity.MessageType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for Message entity operations
 */
@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    
    /**
     * Find message history between two users (private conversation)
     * @param userId1 first user ID
     * @param userId2 second user ID
     * @param pageable pagination information
     * @return list of messages between the two users
     */
    @Query("SELECT m FROM Message m WHERE " +
           "((m.sender.id = :userId1 AND m.receiver.id = :userId2) OR " +
           "(m.sender.id = :userId2 AND m.receiver.id = :userId1)) AND " +
           "m.groupId IS NULL " +
           "ORDER BY m.timestamp DESC")
    List<Message> findPrivateMessageHistory(@Param("userId1") Long userId1, 
                                          @Param("userId2") Long userId2, 
                                          Pageable pageable);
    
    /**
     * Find all messages sent by a specific user
     * @param senderId the sender's user ID
     * @param pageable pagination information
     * @return list of messages sent by the user
     */
    @Query("SELECT m FROM Message m WHERE m.sender.id = :senderId ORDER BY m.timestamp DESC")
    List<Message> findBySenderId(@Param("senderId") Long senderId, Pageable pageable);
    
    /**
     * Find all messages received by a specific user
     * @param receiverId the receiver's user ID
     * @param pageable pagination information
     * @return list of messages received by the user
     */
    @Query("SELECT m FROM Message m WHERE m.receiver.id = :receiverId ORDER BY m.timestamp DESC")
    List<Message> findByReceiverId(@Param("receiverId") Long receiverId, Pageable pageable);
    
    /**
     * Find group message history
     * @param groupId the group ID
     * @param pageable pagination information
     * @return list of messages in the group
     */
    @Query("SELECT m FROM Message m WHERE m.groupId = :groupId ORDER BY m.timestamp DESC")
    List<Message> findGroupMessageHistory(@Param("groupId") String groupId, Pageable pageable);
    
    /**
     * Find all conversations for a user (both sent and received messages)
     * @param userId the user ID
     * @param pageable pagination information
     * @return list of messages involving the user
     */
    @Query("SELECT m FROM Message m WHERE " +
           "(m.sender.id = :userId OR m.receiver.id = :userId) AND " +
           "m.groupId IS NULL " +
           "ORDER BY m.timestamp DESC")
    List<Message> findUserConversations(@Param("userId") Long userId, Pageable pageable);
    
    /**
     * Find recent messages by type
     * @param messageType the message type to filter by
     * @param pageable pagination information
     * @return list of messages of the specified type
     */
    List<Message> findByTypeOrderByTimestampDesc(MessageType messageType, Pageable pageable);
    
    /**
     * Find all group messages for a specific user
     * @param userId the user ID
     * @param pageable pagination information
     * @return list of group messages where user is involved
     */
    @Query("SELECT m FROM Message m WHERE " +
           "m.groupId IS NOT NULL AND " +
           "(m.sender.id = :userId OR " +
           "EXISTS (SELECT 1 FROM Message m2 WHERE m2.groupId = m.groupId AND m2.sender.id = :userId)) " +
           "ORDER BY m.timestamp DESC")
    List<Message> findUserGroupMessages(@Param("userId") Long userId, Pageable pageable);
    
    /**
     * Count messages in a group
     * @param groupId the group ID
     * @return number of messages in the group
     */
    @Query("SELECT COUNT(m) FROM Message m WHERE m.groupId = :groupId")
    Long countByGroupId(@Param("groupId") String groupId);
    
    /**
     * Count private messages between two users
     * @param userId1 first user ID
     * @param userId2 second user ID
     * @return number of messages between the two users
     */
    @Query("SELECT COUNT(m) FROM Message m WHERE " +
           "((m.sender.id = :userId1 AND m.receiver.id = :userId2) OR " +
           "(m.sender.id = :userId2 AND m.receiver.id = :userId1)) AND " +
           "m.groupId IS NULL")
    Long countPrivateMessages(@Param("userId1") Long userId1, @Param("userId2") Long userId2);
    
    /**
     * Find messages between two users with limit (for MessageService)
     * @param userId1 first user ID
     * @param userId2 second user ID
     * @param limit maximum number of messages to return
     * @return list of messages between the two users, limited and ordered by timestamp
     */
    @Query(value = "SELECT * FROM messages m WHERE " +
           "((m.sender_id = :userId1 AND m.receiver_id = :userId2) OR " +
           "(m.sender_id = :userId2 AND m.receiver_id = :userId1)) AND " +
           "m.group_id IS NULL " +
           "ORDER BY m.timestamp ASC " +
           "LIMIT :limit", nativeQuery = true)
    List<Message> findMessagesBetweenUsers(@Param("userId1") Long userId1, 
                                         @Param("userId2") Long userId2, 
                                         @Param("limit") int limit);
    
    /**
     * Find group messages with limit (for MessageService)
     * @param groupId the group ID
     * @param limit maximum number of messages to return
     * @return list of group messages, limited and ordered by timestamp
     */
    @Query(value = "SELECT * FROM messages m WHERE " +
           "m.group_id = :groupId " +
           "ORDER BY m.timestamp ASC " +
           "LIMIT :limit", nativeQuery = true)
    List<Message> findGroupMessages(@Param("groupId") String groupId, 
                                  @Param("limit") int limit);
}