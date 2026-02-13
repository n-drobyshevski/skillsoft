package app.skillsoft.assessmentbackend.services.impl;

import app.skillsoft.assessmentbackend.domain.entities.User;
import app.skillsoft.assessmentbackend.domain.entities.UserRole;
import app.skillsoft.assessmentbackend.repository.UserRepository;
import app.skillsoft.assessmentbackend.services.UserService.ClerkUserData;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Helper component for user synchronization operations.
 * Extracted to allow Spring AOP to properly intercept transactional methods.
 */
@Component
public class UserSyncHelper {

    private final UserRepository userRepository;

    public UserSyncHelper(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Result of syncing a single user.
     */
    public record SyncUserResult(boolean created) {}

    /**
     * Sync a single user in its own transaction.
     * This allows other users to continue syncing even if one fails.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SyncUserResult syncSingleUser(ClerkUserData clerkUser) {
        Optional<User> existingUser = userRepository.findByClerkId(clerkUser.clerkId());
        
        // Normalize email - convert empty string to null to satisfy database constraint
        String email = (clerkUser.email() != null && !clerkUser.email().isBlank()) 
            ? clerkUser.email() 
            : null;
        
        // Convert timestamps from Unix milliseconds to LocalDateTime
        LocalDateTime clerkCreatedAt = clerkUser.clerkCreatedAt() != null 
            ? java.time.Instant.ofEpochMilli(clerkUser.clerkCreatedAt())
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDateTime()
            : null;
        LocalDateTime lastSignInAt = clerkUser.lastSignInAt() != null 
            ? java.time.Instant.ofEpochMilli(clerkUser.lastSignInAt())
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDateTime()
            : null;
        
        if (existingUser.isPresent()) {
            // Update existing user
            User user = existingUser.get();
            user.setEmail(email);
            user.setUsername(clerkUser.username());
            user.setFirstName(clerkUser.firstName());
            user.setLastName(clerkUser.lastName());
            user.setImageUrl(clerkUser.imageUrl());
            user.setHasImage(clerkUser.hasImage());
            user.setBanned(clerkUser.banned());
            user.setLocked(clerkUser.locked());
            user.setClerkCreatedAt(clerkCreatedAt);
            user.setLastSignInAt(lastSignInAt);
            
            // Update active status based on banned/locked state
            if (clerkUser.banned() || clerkUser.locked()) {
                user.setActive(false);
            }
            
            // Update role if provided
            if (clerkUser.role() != null && !clerkUser.role().isBlank()) {
                try {
                    UserRole newRole = UserRole.valueOf(clerkUser.role().toUpperCase());
                    user.setRole(newRole);
                } catch (IllegalArgumentException e) {
                    // Keep existing role if invalid role provided
                }
            }
            
            userRepository.save(user);
            return new SyncUserResult(false);
        } else {
            // Create new user
            UserRole role = UserRole.USER;
            if (clerkUser.role() != null && !clerkUser.role().isBlank()) {
                try {
                    role = UserRole.valueOf(clerkUser.role().toUpperCase());
                } catch (IllegalArgumentException e) {
                    // Default to USER if invalid role
                }
            }
            
            User newUser = new User(
                clerkUser.clerkId(),
                email,
                clerkUser.username(),
                clerkUser.firstName(),
                clerkUser.lastName(),
                clerkUser.imageUrl(),
                clerkUser.hasImage(),
                clerkUser.banned(),
                clerkUser.locked(),
                clerkCreatedAt,
                lastSignInAt,
                role
            );
            userRepository.save(newUser);
            return new SyncUserResult(true);
        }
    }
}
