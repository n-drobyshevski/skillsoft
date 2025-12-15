package app.skillsoft.assessmentbackend.controller;

import app.skillsoft.assessmentbackend.domain.dto.UserDto;
import app.skillsoft.assessmentbackend.domain.entities.User;
import app.skillsoft.assessmentbackend.domain.entities.UserRole;
import app.skillsoft.assessmentbackend.domain.mapper.UserMapper;
import app.skillsoft.assessmentbackend.services.UserService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * REST Controller for User management operations.
 * Provides endpoints for user CRUD operations and Clerk.js integration.
 * 
 * Security:
 * - All endpoints require ADMIN role
 * - Uses @PreAuthorize for method-level security
 */
@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;

    @Autowired
    public UserController(UserService userService, UserMapper userMapper) {
        this.userService = userService;
        this.userMapper = userMapper;
    }

    /**
     * Create a new user.
     * Used primarily for Clerk webhook integration.
     */
    @PostMapping
    public ResponseEntity<UserDto> createUser(@Valid @RequestBody UserDto userDto) {
        try {
            User user = userService.createUser(
                    userDto.clerkId(),
                    userDto.email(),
                    userDto.firstName(),
                    userDto.lastName(),
                    userDto.role()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(userMapper.toDto(user));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    /**
     * Get all users.
     */
    @GetMapping
    public ResponseEntity<List<UserDto>> getAllUsers() {
        List<User> users = userService.findAllUsers();
        List<UserDto> userDtos = users.stream()
                .map(userMapper::toDto)
                .toList();
        return ResponseEntity.ok(userDtos);
    }

    /**
     * Get all active users.
     */
    @GetMapping("/active")
    public ResponseEntity<List<UserDto>> getActiveUsers() {
        List<User> users = userService.findActiveUsers();
        List<UserDto> userDtos = users.stream()
                .map(userMapper::toDto)
                .toList();
        return ResponseEntity.ok(userDtos);
    }

    /**
     * Get user by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUserById(@PathVariable UUID id) {
        Optional<User> userOptional = userService.findById(id);
        return userOptional
                .map(user -> ResponseEntity.ok(userMapper.toDto(user)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get user by Clerk ID.
     */
    @GetMapping("/clerk/{clerkId}")
    public ResponseEntity<UserDto> getUserByClerkId(@PathVariable String clerkId) {
        Optional<User> userOptional = userService.findByClerkId(clerkId);
        return userOptional
                .map(user -> ResponseEntity.ok(userMapper.toDto(user)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get user by email.
     */
    @GetMapping("/email/{email}")
    public ResponseEntity<UserDto> getUserByEmail(@PathVariable String email) {
        Optional<User> userOptional = userService.findByEmail(email);
        return userOptional
                .map(user -> ResponseEntity.ok(userMapper.toDto(user)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get users by role.
     */
    @GetMapping("/role/{role}")
    public ResponseEntity<List<UserDto>> getUsersByRole(@PathVariable UserRole role) {
        List<User> users = userService.findUsersByRole(role);
        List<UserDto> userDtos = users.stream()
                .map(userMapper::toDto)
                .toList();
        return ResponseEntity.ok(userDtos);
    }

    /**
     * Update user information.
     */
    @PutMapping("/{id}")
    public ResponseEntity<UserDto> updateUser(@PathVariable UUID id, @Valid @RequestBody UserDto userDto) {
        try {
            User user = userMapper.fromDto(userDto);
            user.setId(id); // Ensure ID matches path parameter
            User updatedUser = userService.updateUser(user);
            return ResponseEntity.ok(userMapper.toDto(updatedUser));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Update user role by Clerk ID.
     */
    @PatchMapping("/clerk/{clerkId}/role")
    public ResponseEntity<UserDto> updateUserRole(@PathVariable String clerkId, @RequestBody Map<String, String> roleUpdate) {
        try {
            UserRole newRole = UserRole.valueOf(roleUpdate.get("role"));
            User updatedUser = userService.updateUserRole(clerkId, newRole);
            return ResponseEntity.ok(userMapper.toDto(updatedUser));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Update user's last login timestamp.
     */
    @PatchMapping("/clerk/{clerkId}/login")
    public ResponseEntity<Void> updateLastLogin(@PathVariable String clerkId) {
        boolean updated = userService.updateLastLogin(clerkId, LocalDateTime.now());
        return updated ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    /**
     * Deactivate user (soft delete).
     */
    @PatchMapping("/clerk/{clerkId}/deactivate")
    public ResponseEntity<Void> deactivateUser(@PathVariable String clerkId) {
        boolean deactivated = userService.deactivateUser(clerkId);
        return deactivated ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    /**
     * Reactivate user.
     */
    @PatchMapping("/clerk/{clerkId}/reactivate")
    public ResponseEntity<Void> reactivateUser(@PathVariable String clerkId) {
        try {
            boolean reactivated = userService.reactivateUser(clerkId);
            return reactivated ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Delete user permanently.
     */
    @DeleteMapping("/clerk/{clerkId}")
    public ResponseEntity<Void> deleteUser(@PathVariable String clerkId) {
        boolean deleted = userService.deleteUser(clerkId);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    /**
     * Sync user data from Clerk (for webhook integration).
     */
    @PostMapping("/clerk/sync")
    public ResponseEntity<UserDto> syncUserFromClerk(@RequestBody Map<String, String> clerkData) {
        try {
            User user = userService.syncUserFromClerk(
                    clerkData.get("clerkId"),
                    clerkData.get("email"),
                    clerkData.get("firstName"),
                    clerkData.get("lastName")
            );
            return ResponseEntity.ok(userMapper.toDto(user));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Search users by query (name or email).
     */
    @GetMapping("/search")
    public ResponseEntity<List<UserDto>> searchUsers(@RequestParam String query) {
        List<User> users = userService.searchUsers(query);
        List<UserDto> userDtos = users.stream()
                .map(userMapper::toDto)
                .toList();
        return ResponseEntity.ok(userDtos);
    }

    /**
     * Get comprehensive user statistics.
     * Returns total users, active users count, and breakdown by role.
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getUserStats() {
        List<User> allUsers = userService.findAllUsers();
        List<User> activeUsers = userService.findActiveUsers();
        List<Object[]> roleStats = userService.getUserStatsByRole();

        Map<String, Long> byRole = roleStats.stream()
                .collect(java.util.stream.Collectors.toMap(
                        row -> ((UserRole) row[0]).name(),
                        row -> (Long) row[1]
                ));

        Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("totalUsers", allUsers.size());
        stats.put("activeUsers", activeUsers.size());
        stats.put("byRole", byRole);

        return ResponseEntity.ok(stats);
    }

    /**
     * Get user statistics by role.
     */
    @GetMapping("/stats/roles")
    public ResponseEntity<Map<UserRole, Long>> getUserStatsByRole() {
        List<Object[]> stats = userService.getUserStatsByRole();
        Map<UserRole, Long> roleStats = stats.stream()
                .collect(java.util.stream.Collectors.toMap(
                        row -> (UserRole) row[0],
                        row -> (Long) row[1]
                ));
        return ResponseEntity.ok(roleStats);
    }

    /**
     * Check if user exists by Clerk ID.
     */
    @GetMapping("/clerk/{clerkId}/exists")
    public ResponseEntity<Map<String, Boolean>> checkUserExists(@PathVariable String clerkId) {
        boolean exists = userService.existsByClerkId(clerkId);
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    /**
     * Bulk sync users from Clerk.
     * Accepts an array of Clerk user data and syncs them to the database.
     * Creates new users or updates existing ones based on Clerk ID.
     */
    @PostMapping("/clerk/sync-all")
    public ResponseEntity<Map<String, Object>> syncAllUsersFromClerk(
            @RequestBody List<UserService.ClerkUserData> clerkUsers) {
        try {
            UserService.SyncResult result = userService.bulkSyncFromClerk(clerkUsers);
            
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("success", true);
            response.put("created", result.created());
            response.put("updated", result.updated());
            response.put("failed", result.failed());
            response.put("total", result.total());
            
            if (!result.errors().isEmpty()) {
                response.put("errors", result.errors());
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "error", "Failed to sync users: " + e.getMessage()
                    ));
        }
    }
}