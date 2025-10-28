package com.chatapp.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Group entity representing a chat group in the system
 */
@Entity
@Table(name = "groups", indexes = {
    @Index(name = "idx_group_name", columnList = "name"),
    @Index(name = "idx_group_created_by", columnList = "created_by_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Group {
    
    @Id
    @Column(length = 100)
    private String id;
    
    @Column(nullable = false, length = 100)
    @NotBlank(message = "Group name is required")
    @Size(min = 1, max = 100, message = "Group name must be between 1 and 100 characters")
    private String name;
    
    @Column(length = 500)
    @Size(max = 500, message = "Group description cannot exceed 500 characters")
    private String description;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", nullable = false)
    @NotNull(message = "Group creator is required")
    private User createdBy;
    
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "group_members",
        joinColumns = @JoinColumn(name = "group_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id"),
        indexes = {
            @Index(name = "idx_group_members_group", columnList = "group_id"),
            @Index(name = "idx_group_members_user", columnList = "user_id")
        }
    )
    private Set<User> members = new HashSet<>();
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    /**
     * Constructor for creating a new group
     */
    public Group(String id, String name, String description, User createdBy) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.createdBy = createdBy;
        this.members = new HashSet<>();
        // Add creator as a member
        this.members.add(createdBy);
    }
    
    /**
     * Add a member to the group
     */
    public void addMember(User user) {
        this.members.add(user);
    }
    
    /**
     * Remove a member from the group
     */
    public void removeMember(User user) {
        this.members.remove(user);
    }
    
    /**
     * Check if a user is a member of the group
     */
    public boolean isMember(User user) {
        return this.members.contains(user);
    }
    
    /**
     * Check if a user is the creator of the group
     */
    public boolean isCreator(User user) {
        return this.createdBy.equals(user);
    }
    
    /**
     * Get the number of members in the group
     */
    public int getMemberCount() {
        return this.members.size();
    }
}