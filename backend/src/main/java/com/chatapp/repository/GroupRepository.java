package com.chatapp.repository;

import com.chatapp.entity.Group;
import com.chatapp.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Group entity operations
 */
@Repository
public interface GroupRepository extends JpaRepository<Group, String> {
    
    /**
     * Find a group by its name
     * @param name the group name
     * @return optional group
     */
    Optional<Group> findByName(String name);
    
    /**
     * Find all groups created by a specific user
     * @param createdBy the user who created the groups
     * @param pageable pagination information
     * @return list of groups created by the user
     */
    List<Group> findByCreatedByOrderByCreatedAtDesc(User createdBy, Pageable pageable);
    
    /**
     * Find all groups where a user is a member
     * @param userId the user ID
     * @param pageable pagination information
     * @return list of groups where the user is a member
     */
    @Query("SELECT g FROM Group g JOIN g.members m WHERE m.id = :userId ORDER BY g.updatedAt DESC")
    List<Group> findGroupsByMemberId(@Param("userId") Long userId, Pageable pageable);
    
    /**
     * Find all groups where a user is a member (by username)
     * @param username the username
     * @param pageable pagination information
     * @return list of groups where the user is a member
     */
    @Query("SELECT g FROM Group g JOIN g.members m WHERE m.username = :username ORDER BY g.updatedAt DESC")
    List<Group> findGroupsByMemberUsername(@Param("username") String username, Pageable pageable);
    
    /**
     * Check if a user is a member of a specific group
     * @param groupId the group ID
     * @param userId the user ID
     * @return true if user is a member, false otherwise
     */
    @Query("SELECT COUNT(g) > 0 FROM Group g JOIN g.members m WHERE g.id = :groupId AND m.id = :userId")
    boolean isUserMemberOfGroup(@Param("groupId") String groupId, @Param("userId") Long userId);
    
    /**
     * Check if a user is a member of a specific group (by username)
     * @param groupId the group ID
     * @param username the username
     * @return true if user is a member, false otherwise
     */
    @Query("SELECT COUNT(g) > 0 FROM Group g JOIN g.members m WHERE g.id = :groupId AND m.username = :username")
    boolean isUserMemberOfGroupByUsername(@Param("groupId") String groupId, @Param("username") String username);
    
    /**
     * Find all public groups (for discovery)
     * @param pageable pagination information
     * @return list of all groups
     */
    @Query("SELECT g FROM Group g ORDER BY g.createdAt DESC")
    List<Group> findAllGroups(Pageable pageable);
    
    /**
     * Count total members in a group
     * @param groupId the group ID
     * @return number of members in the group
     */
    @Query("SELECT COUNT(m) FROM Group g JOIN g.members m WHERE g.id = :groupId")
    Long countMembersByGroupId(@Param("groupId") String groupId);
    
    /**
     * Find groups by name containing a search term (case insensitive)
     * @param searchTerm the search term
     * @param pageable pagination information
     * @return list of groups matching the search term
     */
    @Query("SELECT g FROM Group g WHERE LOWER(g.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) ORDER BY g.name")
    List<Group> findByNameContainingIgnoreCase(@Param("searchTerm") String searchTerm, Pageable pageable);
    
    /**
     * Check if a group name already exists
     * @param name the group name
     * @return true if name exists, false otherwise
     */
    boolean existsByName(String name);
}