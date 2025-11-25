package app.skillsoft.assessmentbackend.domain.dto;

import app.skillsoft.assessmentbackend.domain.entities.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Data Transfer Object for User entity.
 * Used for API requests and responses.
 */
public record UserDto(
        UUID id,
        
        @NotBlank(message = "Clerk ID is required")
        @Size(max = 100, message = "Clerk ID must not exceed 100 characters")
        String clerkId,
        
        @Email(message = "Email must be valid")
        @Size(max = 255, message = "Email must not exceed 255 characters")
        String email,
        
        @Size(max = 100, message = "Username must not exceed 100 characters")
        String username,
        
        @Size(max = 100, message = "First name must not exceed 100 characters")
        String firstName,
        
        @Size(max = 100, message = "Last name must not exceed 100 characters")
        String lastName,
        
        @Size(max = 500, message = "Image URL must not exceed 500 characters")
        String imageUrl,
        
        boolean hasImage,
        
        @NotNull(message = "User role is required")
        UserRole role,
        
        boolean isActive,
        boolean banned,
        boolean locked,
        String preferences,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime lastLogin,
        LocalDateTime clerkCreatedAt,
        LocalDateTime lastSignInAt
) {
    /**
     * Create a DTO for user creation (without timestamps).
     */
    public static UserDto forCreation(String clerkId, String email, String username, 
                                      String firstName, String lastName, String imageUrl, UserRole role) {
        return new UserDto(null, clerkId, email, username, firstName, lastName, imageUrl, 
                          imageUrl != null && !imageUrl.isBlank(), role, true, false, false, 
                          null, null, null, null, null, null);
    }

    /**
     * Create a DTO for user updates (keeping existing ID and timestamps).
     */
    public static UserDto forUpdate(UUID id, String email, String username, String firstName, String lastName, 
                                   String imageUrl, UserRole role, boolean isActive, boolean banned, boolean locked,
                                   String preferences, LocalDateTime createdAt, LocalDateTime updatedAt, 
                                   LocalDateTime lastLogin, LocalDateTime clerkCreatedAt, LocalDateTime lastSignInAt) {
        return new UserDto(id, null, email, username, firstName, lastName, imageUrl, 
                          imageUrl != null && !imageUrl.isBlank(), role, isActive, banned, locked,
                          preferences, createdAt, updatedAt, lastLogin, clerkCreatedAt, lastSignInAt);
    }

    /**
     * Get full name combining first and last names.
     */
    public String getFullName() {
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        } else if (firstName != null) {
            return firstName;
        } else if (lastName != null) {
            return lastName;
        } else if (username != null) {
            return username;
        } else if (email != null) {
            return email;
        }
        return clerkId; // Fallback to clerk ID
    }

    /**
     * Check if user can access the system.
     */
    public boolean canAccess() {
        return isActive && !banned && !locked;
    }
}