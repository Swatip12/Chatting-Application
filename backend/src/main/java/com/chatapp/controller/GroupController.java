package com.chatapp.controller;

import com.chatapp.dto.*;
import com.chatapp.service.GroupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.security.Principal;
import java.util.List;

/**
 * REST controller for group management operations
 */
@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class GroupController {

    private final GroupService groupService;

    /**
     * Create a new group
     * POST /api/groups
     */
    @PostMapping
    public ResponseEntity<ApiResponse<GroupResponse>> createGroup(
            @Valid @RequestBody GroupRequest groupRequest,
            Principal principal) {
        try {
            log.info("Creating group '{}' by user '{}'", groupRequest.getName(), principal.getName());
            
            GroupResponse groupResponse = groupService.createGroup(groupRequest, principal.getName());
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Group created successfully", groupResponse));
                    
        } catch (Exception e) {
            log.error("Error creating group: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Get group information by ID
     * GET /api/groups/{groupId}
     */
    @GetMapping("/{groupId}")
    public ResponseEntity<ApiResponse<GroupResponse>> getGroup(
            @PathVariable String groupId,
            @RequestParam(defaultValue = "false") boolean includeMembers) {
        try {
            log.info("Retrieving group information for ID: {}", groupId);
            
            GroupResponse groupResponse = groupService.getGroup(groupId, includeMembers);
            
            return ResponseEntity.ok(ApiResponse.success("Group retrieved successfully", groupResponse));
                    
        } catch (Exception e) {
            log.error("Error retrieving group: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Get all groups where the current user is a member
     * GET /api/groups/my
     */
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<GroupResponse>>> getUserGroups(
            @RequestParam(defaultValue = "20") int limit,
            Principal principal) {
        try {
            log.info("Retrieving groups for user: {}", principal.getName());
            
            List<GroupResponse> groups = groupService.getUserGroups(principal.getName(), limit);
            
            return ResponseEntity.ok(ApiResponse.success("User groups retrieved successfully", groups));
                    
        } catch (Exception e) {
            log.error("Error retrieving user groups: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Get all available groups (for discovery)
     * GET /api/groups
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<GroupResponse>>> getAllGroups(
            @RequestParam(defaultValue = "50") int limit) {
        try {
            log.info("Retrieving all groups (limit: {})", limit);
            
            List<GroupResponse> groups = groupService.getAllGroups(limit);
            
            return ResponseEntity.ok(ApiResponse.success("Groups retrieved successfully", groups));
                    
        } catch (Exception e) {
            log.error("Error retrieving all groups: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Search groups by name
     * GET /api/groups/search
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<GroupResponse>>> searchGroups(
            @RequestParam String q,
            @RequestParam(defaultValue = "20") int limit) {
        try {
            log.info("Searching groups with term: '{}'", q);
            
            List<GroupResponse> groups = groupService.searchGroups(q, limit);
            
            return ResponseEntity.ok(ApiResponse.success("Groups search completed", groups));
                    
        } catch (Exception e) {
            log.error("Error searching groups: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Add a member to a group
     * POST /api/groups/{groupId}/members
     */
    @PostMapping("/{groupId}/members")
    public ResponseEntity<ApiResponse<GroupResponse>> addMember(
            @PathVariable String groupId,
            @Valid @RequestBody GroupMemberRequest memberRequest,
            Principal principal) {
        try {
            log.info("Adding member '{}' to group '{}' by user '{}'", 
                    memberRequest.getUsername(), groupId, principal.getName());
            
            GroupResponse groupResponse = groupService.addMember(groupId, memberRequest, principal.getName());
            
            return ResponseEntity.ok(ApiResponse.success("Member added successfully", groupResponse));
                    
        } catch (Exception e) {
            log.error("Error adding member to group: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Remove a member from a group
     * DELETE /api/groups/{groupId}/members
     */
    @DeleteMapping("/{groupId}/members")
    public ResponseEntity<ApiResponse<GroupResponse>> removeMember(
            @PathVariable String groupId,
            @Valid @RequestBody GroupMemberRequest memberRequest,
            Principal principal) {
        try {
            log.info("Removing member '{}' from group '{}' by user '{}'", 
                    memberRequest.getUsername(), groupId, principal.getName());
            
            GroupResponse groupResponse = groupService.removeMember(groupId, memberRequest, principal.getName());
            
            return ResponseEntity.ok(ApiResponse.success("Member removed successfully", groupResponse));
                    
        } catch (Exception e) {
            log.error("Error removing member from group: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Check if current user is a member of a group
     * GET /api/groups/{groupId}/membership
     */
    @GetMapping("/{groupId}/membership")
    public ResponseEntity<ApiResponse<Boolean>> checkMembership(
            @PathVariable String groupId,
            Principal principal) {
        try {
            log.info("Checking membership for user '{}' in group '{}'", principal.getName(), groupId);
            
            boolean isMember = groupService.isUserMemberOfGroup(groupId, principal.getName());
            
            return ResponseEntity.ok(ApiResponse.success("Membership status retrieved", isMember));
                    
        } catch (Exception e) {
            log.error("Error checking group membership: {}", e.getMessage(), e);
            throw e;
        }
    }
}