package app.skillsoft.assessmentbackend.services.impl;

import app.skillsoft.assessmentbackend.domain.entities.User;
import app.skillsoft.assessmentbackend.domain.entities.UserRole;
import app.skillsoft.assessmentbackend.repository.UserRepository;
import app.skillsoft.assessmentbackend.services.UserService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of UserService for user management and Clerk.js integration.
 */
@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserSyncHelper userSyncHelper;

    @Autowired
    public UserServiceImpl(UserRepository userRepository, UserSyncHelper userSyncHelper) {
        this.userRepository = userRepository;
        this.userSyncHelper = userSyncHelper;
    }

    @Override
    public User createUser(String clerkId, String email, String firstName, String lastName, UserRole role) {
        // Check if user already exists
        if (userRepository.existsByClerkId(clerkId)) {
            throw new IllegalArgumentException("User with Clerk ID " + clerkId + " already exists");
        }

        // Default role to USER if not specified
        UserRole userRole = (role != null) ? role : UserRole.USER;

        User user = new User(clerkId, email, firstName, lastName, userRole);
        return userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByClerkId(String clerkId) {
        return userRepository.findByClerkId(clerkId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findById(UUID id) {
        return userRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findAllUsers() {
        return userRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findActiveUsers() {
        return userRepository.findByIsActiveTrue();
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findUsersByRole(UserRole role) {
        return userRepository.findByRoleAndIsActiveTrue(role);
    }

    @Override
    public User updateUser(User user) {
        if (user.getId() == null) {
            throw new IllegalArgumentException("User ID is required for update");
        }

        if (!userRepository.existsById(user.getId())) {
            throw new EntityNotFoundException("User not found with ID: " + user.getId());
        }

        return userRepository.save(user);
    }

    @Override
    public User updateUserRole(String clerkId, UserRole newRole) {
        User user = userRepository.findByClerkId(clerkId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with Clerk ID: " + clerkId));

        user.setRole(newRole);
        return userRepository.save(user);
    }

    @Override
    public boolean updateLastLogin(String clerkId, LocalDateTime loginTime) {
        int updatedCount = userRepository.updateLastLoginByClerkId(clerkId, loginTime);
        return updatedCount > 0;
    }

    @Override
    public boolean deactivateUser(String clerkId) {
        int updatedCount = userRepository.deactivateByClerkId(clerkId);
        return updatedCount > 0;
    }

    @Override
    public boolean reactivateUser(String clerkId) {
        User user = userRepository.findByClerkId(clerkId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with Clerk ID: " + clerkId));

        user.setActive(true);
        userRepository.save(user);
        return true;
    }

    @Override
    public boolean deleteUser(String clerkId) {
        Optional<User> userOptional = userRepository.findByClerkId(clerkId);
        if (userOptional.isPresent()) {
            userRepository.delete(userOptional.get());
            return true;
        }
        return false;
    }

    @Override
    public User syncUserFromClerk(String clerkId, String email, String firstName, String lastName) {
        Optional<User> userOptional = userRepository.findByClerkId(clerkId);

        if (userOptional.isPresent()) {
            // Update existing user
            User user = userOptional.get();
            user.setEmail(email);
            user.setFirstName(firstName);
            user.setLastName(lastName);
            return userRepository.save(user);
        } else {
            // Create new user with default USER role
            return createUser(clerkId, email, firstName, lastName, UserRole.USER);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByClerkId(String clerkId) {
        return userRepository.existsByClerkId(clerkId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> searchUsers(String query) {
        if (query == null || query.trim().isEmpty()) {
            return findActiveUsers();
        }

        String trimmedQuery = query.trim();

        // Try to find by email first
        List<User> emailMatches = userRepository.findByEmailContainingIgnoreCase(trimmedQuery);
        if (!emailMatches.isEmpty()) {
            return emailMatches;
        }

        // Then try by name
        return userRepository.findByNameContaining(trimmedQuery);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Object[]> getUserStatsByRole() {
        return userRepository.countActiveUsersByRole();
    }

    @Override
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    public SyncResult bulkSyncFromClerk(List<ClerkUserData> clerkUsers) {
        int created = 0;
        int updated = 0;
        int failed = 0;
        List<String> errors = new java.util.ArrayList<>();

        for (ClerkUserData clerkUser : clerkUsers) {
            try {
                // Use helper bean for proper transaction proxy handling
                UserSyncHelper.SyncUserResult result = userSyncHelper.syncSingleUser(clerkUser);
                if (result.created()) {
                    created++;
                } else {
                    updated++;
                }
            } catch (Exception e) {
                failed++;
                errors.add("Failed to sync user " + clerkUser.clerkId() + ": " + e.getMessage());
            }
        }

        return new SyncResult(created, updated, failed, clerkUsers.size(), errors);
    }
}