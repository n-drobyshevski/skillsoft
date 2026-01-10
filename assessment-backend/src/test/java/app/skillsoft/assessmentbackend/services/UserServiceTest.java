package app.skillsoft.assessmentbackend.services;

import app.skillsoft.assessmentbackend.domain.entities.User;
import app.skillsoft.assessmentbackend.domain.entities.UserRole;
import app.skillsoft.assessmentbackend.repository.UserRepository;
import app.skillsoft.assessmentbackend.services.UserService.ClerkUserData;
import app.skillsoft.assessmentbackend.services.UserService.SyncResult;
import app.skillsoft.assessmentbackend.services.impl.UserServiceImpl;
import app.skillsoft.assessmentbackend.services.impl.UserSyncHelper;
import app.skillsoft.assessmentbackend.testutils.BaseUnitTest;
import app.skillsoft.assessmentbackend.testutils.TestDataFactory;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserService implementation.
 *
 * Tests cover:
 * - User creation with validation
 * - User lookup methods (by clerkId, email, id)
 * - User update operations
 * - User role management
 * - User status management (activate/deactivate)
 * - Clerk synchronization (single and bulk)
 * - User search functionality
 */
@DisplayName("UserService Tests")
class UserServiceTest extends BaseUnitTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserSyncHelper userSyncHelper;

    private UserServiceImpl userService;

    private User testUser;
    private String testClerkId;
    private UUID testUserId;
    private String testEmail;

    @BeforeEach
    void setUp() {
        TestDataFactory.resetCounter();

        userService = new UserServiceImpl(userRepository, userSyncHelper);

        testClerkId = "clerk_test_user_123";
        testUserId = UUID.randomUUID();
        testEmail = "test@skillsoft.app";

        testUser = TestDataFactory.createUser(UserRole.USER);
        testUser.setId(testUserId);
        testUser.setClerkId(testClerkId);
        testUser.setEmail(testEmail);
    }

    @Nested
    @DisplayName("createUser")
    class CreateUserTests {

        @Test
        @DisplayName("Should create user with all fields provided")
        void shouldCreateUserWithAllFields() {
            // Given
            when(userRepository.existsByClerkId(testClerkId)).thenReturn(false);
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User saved = invocation.getArgument(0);
                saved.setId(testUserId);
                return saved;
            });

            // When
            User result = userService.createUser(
                    testClerkId,
                    testEmail,
                    "John",
                    "Doe",
                    UserRole.EDITOR
            );

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getClerkId()).isEqualTo(testClerkId);
            assertThat(result.getEmail()).isEqualTo(testEmail);
            assertThat(result.getFirstName()).isEqualTo("John");
            assertThat(result.getLastName()).isEqualTo("Doe");
            assertThat(result.getRole()).isEqualTo(UserRole.EDITOR);

            verify(userRepository).existsByClerkId(testClerkId);
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Should throw exception when clerkId already exists")
        void shouldThrowWhenClerkIdAlreadyExists() {
            // Given
            when(userRepository.existsByClerkId(testClerkId)).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> userService.createUser(
                    testClerkId, testEmail, "John", "Doe", UserRole.USER))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("already exists");

            verify(userRepository).existsByClerkId(testClerkId);
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Should default role to USER when null")
        void shouldDefaultRoleToUserWhenNull() {
            // Given
            when(userRepository.existsByClerkId(testClerkId)).thenReturn(false);
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            when(userRepository.save(userCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            User result = userService.createUser(testClerkId, testEmail, "John", "Doe", null);

            // Then
            assertThat(result.getRole()).isEqualTo(UserRole.USER);
            assertThat(userCaptor.getValue().getRole()).isEqualTo(UserRole.USER);
        }

        @Test
        @DisplayName("Should create user with null optional fields")
        void shouldCreateUserWithNullOptionalFields() {
            // Given
            when(userRepository.existsByClerkId(testClerkId)).thenReturn(false);
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            User result = userService.createUser(testClerkId, testEmail, null, null, UserRole.USER);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getFirstName()).isNull();
            assertThat(result.getLastName()).isNull();
        }
    }

    @Nested
    @DisplayName("findByMethods")
    class FindByTests {

        @Test
        @DisplayName("Should find user by clerkId when exists")
        void shouldFindByClerkIdWhenExists() {
            // Given
            when(userRepository.findByClerkId(testClerkId)).thenReturn(Optional.of(testUser));

            // When
            Optional<User> result = userService.findByClerkId(testClerkId);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getClerkId()).isEqualTo(testClerkId);
            verify(userRepository).findByClerkId(testClerkId);
        }

        @Test
        @DisplayName("Should return empty when clerkId not found")
        void shouldReturnEmptyWhenClerkIdNotFound() {
            // Given
            when(userRepository.findByClerkId("nonexistent")).thenReturn(Optional.empty());

            // When
            Optional<User> result = userService.findByClerkId("nonexistent");

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should find user by id when exists")
        void shouldFindByIdWhenExists() {
            // Given
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));

            // When
            Optional<User> result = userService.findById(testUserId);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(testUserId);
            verify(userRepository).findById(testUserId);
        }

        @Test
        @DisplayName("Should return empty when id not found")
        void shouldReturnEmptyWhenIdNotFound() {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            when(userRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // When
            Optional<User> result = userService.findById(nonExistentId);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should find user by email when exists")
        void shouldFindByEmailWhenExists() {
            // Given
            when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(testUser));

            // When
            Optional<User> result = userService.findByEmail(testEmail);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getEmail()).isEqualTo(testEmail);
            verify(userRepository).findByEmail(testEmail);
        }

        @Test
        @DisplayName("Should return empty when email not found")
        void shouldReturnEmptyWhenEmailNotFound() {
            // Given
            when(userRepository.findByEmail("nonexistent@test.com")).thenReturn(Optional.empty());

            // When
            Optional<User> result = userService.findByEmail("nonexistent@test.com");

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should return all users")
        void shouldReturnAllUsers() {
            // Given
            User user2 = TestDataFactory.createUser(UserRole.ADMIN);
            when(userRepository.findAll()).thenReturn(List.of(testUser, user2));

            // When
            List<User> result = userService.findAllUsers();

            // Then
            assertThat(result).hasSize(2);
            verify(userRepository).findAll();
        }

        @Test
        @DisplayName("Should return active users only")
        void shouldReturnActiveUsers() {
            // Given
            when(userRepository.findByIsActiveTrue()).thenReturn(List.of(testUser));

            // When
            List<User> result = userService.findActiveUsers();

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).isActive()).isTrue();
            verify(userRepository).findByIsActiveTrue();
        }

        @Test
        @DisplayName("Should return users by role")
        void shouldReturnUsersByRole() {
            // Given
            User adminUser = TestDataFactory.createUser(UserRole.ADMIN);
            when(userRepository.findByRoleAndIsActiveTrue(UserRole.ADMIN)).thenReturn(List.of(adminUser));

            // When
            List<User> result = userService.findUsersByRole(UserRole.ADMIN);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getRole()).isEqualTo(UserRole.ADMIN);
            verify(userRepository).findByRoleAndIsActiveTrue(UserRole.ADMIN);
        }
    }

    @Nested
    @DisplayName("updateUser")
    class UpdateUserTests {

        @Test
        @DisplayName("Should update user successfully")
        void shouldUpdateUserSuccessfully() {
            // Given
            testUser.setFirstName("Updated Name");
            when(userRepository.existsById(testUserId)).thenReturn(true);
            when(userRepository.save(testUser)).thenReturn(testUser);

            // When
            User result = userService.updateUser(testUser);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getFirstName()).isEqualTo("Updated Name");
            verify(userRepository).existsById(testUserId);
            verify(userRepository).save(testUser);
        }

        @Test
        @DisplayName("Should throw exception when user.id is null")
        void shouldThrowWhenUserIdIsNull() {
            // Given
            User userWithNullId = new User();
            userWithNullId.setClerkId("test");

            // When & Then
            assertThatThrownBy(() -> userService.updateUser(userWithNullId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("User ID is required");

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when user not found")
        void shouldThrowEntityNotFoundWhenUserNotFound() {
            // Given
            when(userRepository.existsById(testUserId)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> userService.updateUser(testUser))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("User not found");

            verify(userRepository, never()).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("updateUserRole")
    class UpdateUserRoleTests {

        @Test
        @DisplayName("Should update user role successfully")
        void shouldUpdateUserRoleSuccessfully() {
            // Given
            when(userRepository.findByClerkId(testClerkId)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            User result = userService.updateUserRole(testClerkId, UserRole.ADMIN);

            // Then
            assertThat(result.getRole()).isEqualTo(UserRole.ADMIN);
            verify(userRepository).save(argThat(user -> user.getRole() == UserRole.ADMIN));
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when user not found")
        void shouldThrowWhenUserNotFoundForRoleUpdate() {
            // Given
            when(userRepository.findByClerkId("nonexistent")).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> userService.updateUserRole("nonexistent", UserRole.ADMIN))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("User not found");

            verify(userRepository, never()).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("updateLastLogin")
    class UpdateLastLoginTests {

        @Test
        @DisplayName("Should update last login successfully")
        void shouldUpdateLastLoginSuccessfully() {
            // Given
            LocalDateTime loginTime = LocalDateTime.now();
            when(userRepository.updateLastLoginByClerkId(testClerkId, loginTime)).thenReturn(1);

            // When
            boolean result = userService.updateLastLogin(testClerkId, loginTime);

            // Then
            assertThat(result).isTrue();
            verify(userRepository).updateLastLoginByClerkId(testClerkId, loginTime);
        }

        @Test
        @DisplayName("Should return false when user not found")
        void shouldReturnFalseWhenUserNotFound() {
            // Given
            LocalDateTime loginTime = LocalDateTime.now();
            when(userRepository.updateLastLoginByClerkId("nonexistent", loginTime)).thenReturn(0);

            // When
            boolean result = userService.updateLastLogin("nonexistent", loginTime);

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("userStatusManagement")
    class UserStatusTests {

        @Test
        @DisplayName("Should deactivate user successfully")
        void shouldDeactivateUserSuccessfully() {
            // Given
            when(userRepository.deactivateByClerkId(testClerkId)).thenReturn(1);

            // When
            boolean result = userService.deactivateUser(testClerkId);

            // Then
            assertThat(result).isTrue();
            verify(userRepository).deactivateByClerkId(testClerkId);
        }

        @Test
        @DisplayName("Should return false when deactivating nonexistent user")
        void shouldReturnFalseWhenDeactivatingNonexistentUser() {
            // Given
            when(userRepository.deactivateByClerkId("nonexistent")).thenReturn(0);

            // When
            boolean result = userService.deactivateUser("nonexistent");

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should reactivate user successfully")
        void shouldReactivateUserSuccessfully() {
            // Given
            testUser.setActive(false);
            when(userRepository.findByClerkId(testClerkId)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            boolean result = userService.reactivateUser(testClerkId);

            // Then
            assertThat(result).isTrue();
            verify(userRepository).save(argThat(User::isActive));
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when reactivating nonexistent user")
        void shouldThrowWhenReactivatingNonexistentUser() {
            // Given
            when(userRepository.findByClerkId("nonexistent")).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> userService.reactivateUser("nonexistent"))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("User not found");
        }

        @Test
        @DisplayName("Should delete user (hard delete) when exists")
        void shouldDeleteUserWhenExists() {
            // Given
            when(userRepository.findByClerkId(testClerkId)).thenReturn(Optional.of(testUser));
            doNothing().when(userRepository).delete(testUser);

            // When
            boolean result = userService.deleteUser(testClerkId);

            // Then
            assertThat(result).isTrue();
            verify(userRepository).delete(testUser);
        }

        @Test
        @DisplayName("Should return false when deleting nonexistent user")
        void shouldReturnFalseWhenDeletingNonexistentUser() {
            // Given
            when(userRepository.findByClerkId("nonexistent")).thenReturn(Optional.empty());

            // When
            boolean result = userService.deleteUser("nonexistent");

            // Then
            assertThat(result).isFalse();
            verify(userRepository, never()).delete(any(User.class));
        }
    }

    @Nested
    @DisplayName("syncUserFromClerk")
    class SyncUserTests {

        @Test
        @DisplayName("Should create new user with default USER role when not exists")
        void shouldCreateNewUserWithDefaultRole() {
            // Given
            String newClerkId = "clerk_new_user";
            when(userRepository.findByClerkId(newClerkId)).thenReturn(Optional.empty());
            when(userRepository.existsByClerkId(newClerkId)).thenReturn(false);
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User saved = invocation.getArgument(0);
                saved.setId(UUID.randomUUID());
                return saved;
            });

            // When
            User result = userService.syncUserFromClerk(newClerkId, "new@test.com", "New", "User");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getClerkId()).isEqualTo(newClerkId);
            assertThat(result.getRole()).isEqualTo(UserRole.USER);
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Should update existing user fields")
        void shouldUpdateExistingUserFields() {
            // Given
            when(userRepository.findByClerkId(testClerkId)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            User result = userService.syncUserFromClerk(testClerkId, "updated@test.com", "Updated", "Name");

            // Then
            assertThat(result.getEmail()).isEqualTo("updated@test.com");
            assertThat(result.getFirstName()).isEqualTo("Updated");
            assertThat(result.getLastName()).isEqualTo("Name");
            verify(userRepository).save(testUser);
        }

        @Test
        @DisplayName("Should preserve existing role when updating")
        void shouldPreserveExistingRoleWhenUpdating() {
            // Given
            testUser.setRole(UserRole.ADMIN);
            when(userRepository.findByClerkId(testClerkId)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            User result = userService.syncUserFromClerk(testClerkId, "updated@test.com", "First", "Last");

            // Then
            assertThat(result.getRole()).isEqualTo(UserRole.ADMIN);
        }
    }

    @Nested
    @DisplayName("bulkSyncFromClerk")
    class BulkSyncTests {

        @Test
        @DisplayName("Should count created/updated/failed correctly")
        void shouldCountCreatedUpdatedFailedCorrectly() {
            // Given
            ClerkUserData newUser = createClerkUserData("clerk_new", "new@test.com", "USER");
            ClerkUserData existingUser = createClerkUserData("clerk_existing", "existing@test.com", "EDITOR");

            when(userSyncHelper.syncSingleUser(newUser))
                    .thenReturn(new UserSyncHelper.SyncUserResult(true));
            when(userSyncHelper.syncSingleUser(existingUser))
                    .thenReturn(new UserSyncHelper.SyncUserResult(false));

            // When
            SyncResult result = userService.bulkSyncFromClerk(List.of(newUser, existingUser));

            // Then
            assertThat(result.created()).isEqualTo(1);
            assertThat(result.updated()).isEqualTo(1);
            assertThat(result.failed()).isEqualTo(0);
            assertThat(result.total()).isEqualTo(2);
            assertThat(result.errors()).isEmpty();
        }

        @Test
        @DisplayName("Should continue after single user failure")
        void shouldContinueAfterSingleUserFailure() {
            // Given
            ClerkUserData failingUser = createClerkUserData("clerk_fail", "fail@test.com", "USER");
            ClerkUserData successUser = createClerkUserData("clerk_success", "success@test.com", "USER");

            when(userSyncHelper.syncSingleUser(failingUser))
                    .thenThrow(new RuntimeException("Database error"));
            when(userSyncHelper.syncSingleUser(successUser))
                    .thenReturn(new UserSyncHelper.SyncUserResult(true));

            // When
            SyncResult result = userService.bulkSyncFromClerk(List.of(failingUser, successUser));

            // Then
            assertThat(result.created()).isEqualTo(1);
            assertThat(result.updated()).isEqualTo(0);
            assertThat(result.failed()).isEqualTo(1);
            assertThat(result.total()).isEqualTo(2);
            assertThat(result.errors()).hasSize(1);
            assertThat(result.errors().get(0)).contains("clerk_fail");
        }

        @Test
        @DisplayName("Should handle empty list")
        void shouldHandleEmptyList() {
            // When
            SyncResult result = userService.bulkSyncFromClerk(Collections.emptyList());

            // Then
            assertThat(result.created()).isEqualTo(0);
            assertThat(result.updated()).isEqualTo(0);
            assertThat(result.failed()).isEqualTo(0);
            assertThat(result.total()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should handle all users failing")
        void shouldHandleAllUsersFailing() {
            // Given
            ClerkUserData user1 = createClerkUserData("clerk_1", "user1@test.com", "USER");
            ClerkUserData user2 = createClerkUserData("clerk_2", "user2@test.com", "USER");

            when(userSyncHelper.syncSingleUser(any()))
                    .thenThrow(new RuntimeException("Sync failed"));

            // When
            SyncResult result = userService.bulkSyncFromClerk(List.of(user1, user2));

            // Then
            assertThat(result.created()).isEqualTo(0);
            assertThat(result.updated()).isEqualTo(0);
            assertThat(result.failed()).isEqualTo(2);
            assertThat(result.errors()).hasSize(2);
        }

        private ClerkUserData createClerkUserData(String clerkId, String email, String role) {
            return new ClerkUserData(
                    clerkId,
                    email,
                    null, // username
                    "First",
                    "Last",
                    null, // imageUrl
                    false, // hasImage
                    false, // banned
                    false, // locked
                    System.currentTimeMillis(), // clerkCreatedAt
                    System.currentTimeMillis(), // lastSignInAt
                    role
            );
        }
    }

    @Nested
    @DisplayName("searchUsers")
    class SearchUsersTests {

        @Test
        @DisplayName("Should return active users when query is empty")
        void shouldReturnActiveUsersWhenQueryEmpty() {
            // Given
            when(userRepository.findByIsActiveTrue()).thenReturn(List.of(testUser));

            // When
            List<User> result = userService.searchUsers("");

            // Then
            assertThat(result).hasSize(1);
            verify(userRepository).findByIsActiveTrue();
            verify(userRepository, never()).findByEmailContainingIgnoreCase(anyString());
            verify(userRepository, never()).findByNameContaining(anyString());
        }

        @Test
        @DisplayName("Should return active users when query is null")
        void shouldReturnActiveUsersWhenQueryNull() {
            // Given
            when(userRepository.findByIsActiveTrue()).thenReturn(List.of(testUser));

            // When
            List<User> result = userService.searchUsers(null);

            // Then
            assertThat(result).hasSize(1);
            verify(userRepository).findByIsActiveTrue();
        }

        @Test
        @DisplayName("Should return active users when query is whitespace only")
        void shouldReturnActiveUsersWhenQueryWhitespaceOnly() {
            // Given
            when(userRepository.findByIsActiveTrue()).thenReturn(List.of(testUser));

            // When
            List<User> result = userService.searchUsers("   ");

            // Then
            assertThat(result).hasSize(1);
            verify(userRepository).findByIsActiveTrue();
        }

        @Test
        @DisplayName("Should search by email first and return results")
        void shouldSearchByEmailFirstAndReturnResults() {
            // Given
            when(userRepository.findByEmailContainingIgnoreCase("test@")).thenReturn(List.of(testUser));

            // When
            List<User> result = userService.searchUsers("test@");

            // Then
            assertThat(result).hasSize(1);
            verify(userRepository).findByEmailContainingIgnoreCase("test@");
            verify(userRepository, never()).findByNameContaining(anyString());
        }

        @Test
        @DisplayName("Should fall back to name search when email search returns empty")
        void shouldFallBackToNameSearchWhenEmailSearchEmpty() {
            // Given
            when(userRepository.findByEmailContainingIgnoreCase("John")).thenReturn(Collections.emptyList());
            when(userRepository.findByNameContaining("John")).thenReturn(List.of(testUser));

            // When
            List<User> result = userService.searchUsers("John");

            // Then
            assertThat(result).hasSize(1);
            verify(userRepository).findByEmailContainingIgnoreCase("John");
            verify(userRepository).findByNameContaining("John");
        }

        @Test
        @DisplayName("Should return empty list when no matches found")
        void shouldReturnEmptyListWhenNoMatchesFound() {
            // Given
            when(userRepository.findByEmailContainingIgnoreCase("xyz")).thenReturn(Collections.emptyList());
            when(userRepository.findByNameContaining("xyz")).thenReturn(Collections.emptyList());

            // When
            List<User> result = userService.searchUsers("xyz");

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should trim query before searching")
        void shouldTrimQueryBeforeSearching() {
            // Given
            when(userRepository.findByEmailContainingIgnoreCase("test")).thenReturn(List.of(testUser));

            // When
            List<User> result = userService.searchUsers("  test  ");

            // Then
            assertThat(result).hasSize(1);
            verify(userRepository).findByEmailContainingIgnoreCase("test");
        }
    }

    @Nested
    @DisplayName("existsByClerkId")
    class ExistsByClerkIdTests {

        @Test
        @DisplayName("Should return true when user exists")
        void shouldReturnTrueWhenUserExists() {
            // Given
            when(userRepository.existsByClerkId(testClerkId)).thenReturn(true);

            // When
            boolean result = userService.existsByClerkId(testClerkId);

            // Then
            assertThat(result).isTrue();
            verify(userRepository).existsByClerkId(testClerkId);
        }

        @Test
        @DisplayName("Should return false when user does not exist")
        void shouldReturnFalseWhenUserDoesNotExist() {
            // Given
            when(userRepository.existsByClerkId("nonexistent")).thenReturn(false);

            // When
            boolean result = userService.existsByClerkId("nonexistent");

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("getUserStatsByRole")
    class GetUserStatsByRoleTests {

        @Test
        @DisplayName("Should return user statistics by role")
        void shouldReturnUserStatsByRole() {
            // Given
            List<Object[]> stats = List.of(
                    new Object[]{UserRole.USER, 10L},
                    new Object[]{UserRole.ADMIN, 2L},
                    new Object[]{UserRole.EDITOR, 5L}
            );
            when(userRepository.countActiveUsersByRole()).thenReturn(stats);

            // When
            List<Object[]> result = userService.getUserStatsByRole();

            // Then
            assertThat(result).hasSize(3);
            verify(userRepository).countActiveUsersByRole();
        }

        @Test
        @DisplayName("Should return empty list when no users")
        void shouldReturnEmptyListWhenNoUsers() {
            // Given
            when(userRepository.countActiveUsersByRole()).thenReturn(Collections.emptyList());

            // When
            List<Object[]> result = userService.getUserStatsByRole();

            // Then
            assertThat(result).isEmpty();
        }
    }
}
