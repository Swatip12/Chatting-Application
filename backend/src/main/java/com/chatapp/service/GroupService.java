package com.chatapp.service;

import com.chatapp.dto.*;
import com.chatapp.entity.Group;
import com.chatapp.entity.MessageType;
import com.chatapp.entity.User;
import com.chatapp.exception.GroupNotFoundException;
import com.chatapp.exception.UserNotFoundException;
import com.chatapp.exception.DuplicateGroupNameException;
import com.chatapp.exception.UnauthorizedOperationException;
import com.chatapp.repository.GroupRepository;
import com.chatapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service class for group management operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class GroupService {

    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Create a new group
     * @param groupRequest the group creation request
     * @param creatorUsername the username of the group creator
     * @return created group response
     */
    public GroupResponse createGroup(GroupRequest groupRequest, String creatorUsername) {
        log.info("Creating new group '{}' by user '{}'", groupRequest.getName(), creatorUsername);

        try {
            // Check if group name already exists
            if (groupRepository.existsByName(groupRequest.getName())) {
                throw new DuplicateGroupNameException("Group name '" + groupRequest.getName() + "' already exists");
            }

            // Find creator user
            User creator = userRepository.findByUsername(creatorUsername)
                    .orElseThrow(() -> new UserNotFoundException("Creator not found: " + creatorUsername));

            // Generate unique group ID
            String groupId = generateGroupId();

            // Create group entity
            Group group = new Group(groupId, groupRequest.getName(), groupRequest.getDescription(), creator);
            Group savedGroup = groupRepository.save(group);

            log.info("Group '{}' created successfully with ID: {}", savedGroup.getName(), savedGroup.getId());

            // Broadcast group creation notification
            broadcastGroupNotification(savedGroup.getId(), creatorUsername + " created group '" + savedGroup.getName() + "'", MessageType.JOIN);

            return convertToResponse(savedGroup, true);

        } catch (Exception e) {
            log.error("Error creating group: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Get group information by ID
     * @param groupId the group ID
     * @param includeMembers whether to include member details
     * @return group response
     */
    @Transactional(readOnly = true)
    public GroupResponse getGroup(String groupId, boolean includeMembers) {
        log.info("Retrieving group information for ID: {}", groupId);

        try {
            Group group = groupRepository.findById(groupId)
                    .orElseThrow(() -> new GroupNotFoundException("Group not found: " + groupId));

            return convertToResponse(group, includeMembers);

        } catch (Exception e) {
            log.error("Error retrieving group: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Get all groups where a user is a member
     * @param username the username
     * @param limit maximum number of groups to return
     * @return list of group responses
     */
    @Transactional(readOnly = true)
    public List<GroupResponse> getUserGroups(String username, int limit) {
        log.info("Retrieving groups for user: {} (limit: {})", username, limit);

        try {
            Pageable pageable = PageRequest.of(0, limit);
            List<Group> groups = groupRepository.findGroupsByMemberUsername(username, pageable);

            List<GroupResponse> groupResponses = groups.stream()
                    .map(group -> convertToResponse(group, false))
                    .collect(Collectors.toList());

            log.info("Retrieved {} groups for user: {}", groupResponses.size(), username);
            return groupResponses;

        } catch (Exception e) {
            log.error("Error retrieving user groups: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Add a member to a group
     * @param groupId the group ID
     * @param memberRequest the member addition request
     * @param requesterUsername the username of the user making the request
     * @return updated group response
     */
    public GroupResponse addMember(String groupId, GroupMemberRequest memberRequest, String requesterUsername) {
        log.info("Adding member '{}' to group '{}' by user '{}'", 
                memberRequest.getUsername(), groupId, requesterUsername);

        try {
            // Find group
            Group group = groupRepository.findById(groupId)
                    .orElseThrow(() -> new GroupNotFoundException("Group not found: " + groupId));

            // Find requester (must be group creator or existing member)
            User requester = userRepository.findByUsername(requesterUsername)
                    .orElseThrow(() -> new UserNotFoundException("Requester not found: " + requesterUsername));

            // Check if requester has permission (is creator or member)
            if (!group.isCreator(requester) && !group.isMember(requester)) {
                throw new UnauthorizedOperationException("Only group members can add new members");
            }

            // Find user to add
            User newMember = userRepository.findByUsername(memberRequest.getUsername())
                    .orElseThrow(() -> new UserNotFoundException("User not found: " + memberRequest.getUsername()));

            // Check if user is already a member
            if (group.isMember(newMember)) {
                log.warn("User '{}' is already a member of group '{}'", memberRequest.getUsername(), groupId);
                return convertToResponse(group, true);
            }

            // Add member to group
            group.addMember(newMember);
            Group savedGroup = groupRepository.save(group);

            log.info("Member '{}' added successfully to group '{}'", memberRequest.getUsername(), groupId);

            // Broadcast member join notification
            broadcastGroupNotification(groupId, memberRequest.getUsername() + " joined the group", MessageType.JOIN);

            return convertToResponse(savedGroup, true);

        } catch (Exception e) {
            log.error("Error adding member to group: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Remove a member from a group
     * @param groupId the group ID
     * @param memberRequest the member removal request
     * @param requesterUsername the username of the user making the request
     * @return updated group response
     */
    public GroupResponse removeMember(String groupId, GroupMemberRequest memberRequest, String requesterUsername) {
        log.info("Removing member '{}' from group '{}' by user '{}'", 
                memberRequest.getUsername(), groupId, requesterUsername);

        try {
            // Find group
            Group group = groupRepository.findById(groupId)
                    .orElseThrow(() -> new GroupNotFoundException("Group not found: " + groupId));

            // Find requester
            User requester = userRepository.findByUsername(requesterUsername)
                    .orElseThrow(() -> new UserNotFoundException("Requester not found: " + requesterUsername));

            // Find user to remove
            User memberToRemove = userRepository.findByUsername(memberRequest.getUsername())
                    .orElseThrow(() -> new UserNotFoundException("User not found: " + memberRequest.getUsername()));

            // Check permissions: creator can remove anyone, members can only remove themselves
            boolean canRemove = group.isCreator(requester) || 
                               (requesterUsername.equals(memberRequest.getUsername()));

            if (!canRemove) {
                throw new UnauthorizedOperationException("You can only remove yourself from the group");
            }

            // Check if user is actually a member
            if (!group.isMember(memberToRemove)) {
                log.warn("User '{}' is not a member of group '{}'", memberRequest.getUsername(), groupId);
                return convertToResponse(group, true);
            }

            // Cannot remove the creator
            if (group.isCreator(memberToRemove)) {
                throw new UnauthorizedOperationException("Cannot remove the group creator");
            }

            // Remove member from group
            group.removeMember(memberToRemove);
            Group savedGroup = groupRepository.save(group);

            log.info("Member '{}' removed successfully from group '{}'", memberRequest.getUsername(), groupId);

            // Broadcast member leave notification
            broadcastGroupNotification(groupId, memberRequest.getUsername() + " left the group", MessageType.LEAVE);

            return convertToResponse(savedGroup, true);

        } catch (Exception e) {
            log.error("Error removing member from group: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Check if a user is a member of a group
     * @param groupId the group ID
     * @param username the username
     * @return true if user is a member, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean isUserMemberOfGroup(String groupId, String username) {
        try {
            return groupRepository.isUserMemberOfGroupByUsername(groupId, username);
        } catch (Exception e) {
            log.error("Error checking group membership: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Get all available groups (for discovery)
     * @param limit maximum number of groups to return
     * @return list of group responses
     */
    @Transactional(readOnly = true)
    public List<GroupResponse> getAllGroups(int limit) {
        log.info("Retrieving all groups (limit: {})", limit);

        try {
            Pageable pageable = PageRequest.of(0, limit);
            List<Group> groups = groupRepository.findAllGroups(pageable);

            List<GroupResponse> groupResponses = groups.stream()
                    .map(group -> convertToResponse(group, false))
                    .collect(Collectors.toList());

            log.info("Retrieved {} groups", groupResponses.size());
            return groupResponses;

        } catch (Exception e) {
            log.error("Error retrieving all groups: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Search groups by name
     * @param searchTerm the search term
     * @param limit maximum number of groups to return
     * @return list of matching group responses
     */
    @Transactional(readOnly = true)
    public List<GroupResponse> searchGroups(String searchTerm, int limit) {
        log.info("Searching groups with term: '{}' (limit: {})", searchTerm, limit);

        try {
            Pageable pageable = PageRequest.of(0, limit);
            List<Group> groups = groupRepository.findByNameContainingIgnoreCase(searchTerm, pageable);

            List<GroupResponse> groupResponses = groups.stream()
                    .map(group -> convertToResponse(group, false))
                    .collect(Collectors.toList());

            log.info("Found {} groups matching search term: '{}'", groupResponses.size(), searchTerm);
            return groupResponses;

        } catch (Exception e) {
            log.error("Error searching groups: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Broadcast group notification to all group members
     * @param groupId the group ID
     * @param message the notification message
     * @param messageType the message type
     */
    private void broadcastGroupNotification(String groupId, String message, MessageType messageType) {
        try {
            ChatMessage notification = new ChatMessage();
            notification.setContent(message);
            notification.setType(messageType);
            notification.setGroupId(groupId);
            notification.setTimestamp(LocalDateTime.now());
            notification.setSenderUsername("System");

            // Broadcast to group topic
            messagingTemplate.convertAndSend("/topic/group/" + groupId, notification);

            log.info("Group notification broadcasted to group {}: {}", groupId, message);

        } catch (Exception e) {
            log.error("Error broadcasting group notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Convert Group entity to GroupResponse DTO
     * @param group the group entity
     * @param includeMembers whether to include member details
     * @return group response DTO
     */
    private GroupResponse convertToResponse(Group group, boolean includeMembers) {
        GroupResponse response = new GroupResponse();
        response.setId(group.getId());
        response.setName(group.getName());
        response.setDescription(group.getDescription());
        response.setCreatedBy(group.getCreatedBy().getUsername());
        response.setMemberCount(group.getMemberCount());
        response.setCreatedAt(group.getCreatedAt());
        response.setUpdatedAt(group.getUpdatedAt());

        if (includeMembers) {
            List<UserResponse> memberResponses = group.getMembers().stream()
                    .map(this::convertUserToResponse)
                    .collect(Collectors.toList());
            response.setMembers(memberResponses);
        }

        return response;
    }

    /**
     * Convert User entity to UserResponse DTO
     * @param user the user entity
     * @return user response DTO
     */
    private UserResponse convertUserToResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setStatus(user.getStatus());
        response.setLastSeen(user.getLastSeen());
        return response;
    }

    /**
     * Generate a unique group ID
     * @return unique group ID
     */
    private String generateGroupId() {
        return "group_" + UUID.randomUUID().toString().replace("-", "");
    }
}