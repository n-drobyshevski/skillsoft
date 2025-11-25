package app.skillsoft.assessmentbackend.domain.entities;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for User entity model
 * 
 * Tests cover:
 * - Entity creation and validation
 * - Business methods
 * - Getter/setter functionality
 * - toString() and equals() methods
 * - Constructor variations
 */
@DisplayName("User Entity Tests")
class UserTest {

    private User user;
    private LocalDateTime now;
    private UUID userId;
    private String clerkId;
    private String email;
    private String firstName;
    private String lastName;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();
        userId = UUID.randomUUID();
        clerkId = "user_2NyYNJCnzYKYw8HsxVgDlmXpz2Q";
        email = "john.doe@example.com";
        firstName = "John";
        lastName = "Doe";
        
        user = new User();
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Default constructor creates empty user")
        void defaultConstructor() {
            User newUser = new User();
            
            assertThat(newUser.getId()).isNull();
            assertThat(newUser.getClerkId()).isNull();
            assertThat(newUser.getEmail()).isNull();
            assertThat(newUser.getFirstName()).isNull();
            assertThat(newUser.getLastName()).isNull();
            assertThat(newUser.getRole()).isNull();
            assertThat(newUser.isActive()).isTrue(); // Default value
            assertThat(newUser.getPreferences()).isNull();
        }

        @Test
        @DisplayName("Constructor with basic parameters")
        void basicConstructor() {
            User newUser = new User(clerkId, email, UserRole.USER);
            
            assertThat(newUser.getClerkId()).isEqualTo(clerkId);
            assertThat(newUser.getEmail()).isEqualTo(email);
            assertThat(newUser.getRole()).isEqualTo(UserRole.USER);
            assertThat(newUser.isActive()).isTrue();
        }

        @Test
        @DisplayName("Constructor with all name parameters")
        void fullConstructor() {
            User newUser = new User(clerkId, email, firstName, lastName, UserRole.EDITOR);
            
            assertThat(newUser.getClerkId()).isEqualTo(clerkId);
            assertThat(newUser.getEmail()).isEqualTo(email);
            assertThat(newUser.getFirstName()).isEqualTo(firstName);
            assertThat(newUser.getLastName()).isEqualTo(lastName);
            assertThat(newUser.getRole()).isEqualTo(UserRole.EDITOR);
            assertThat(newUser.isActive()).isTrue();
        }
    }

    @Nested
    @DisplayName("Setter/Getter Tests")
    class SetterGetterTests {

        @Test
        @DisplayName("Set and get all basic properties")
        void setGetBasicProperties() {
            user.setId(userId);
            user.setClerkId(clerkId);
            user.setEmail(email);
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setRole(UserRole.ADMIN);
            user.setActive(false);
            user.setPreferences("{\"theme\": \"dark\"}");
            user.setCreatedAt(now);
            user.setUpdatedAt(now);
            user.setLastLogin(now);
            
            assertThat(user.getId()).isEqualTo(userId);
            assertThat(user.getClerkId()).isEqualTo(clerkId);
            assertThat(user.getEmail()).isEqualTo(email);
            assertThat(user.getFirstName()).isEqualTo(firstName);
            assertThat(user.getLastName()).isEqualTo(lastName);
            assertThat(user.getRole()).isEqualTo(UserRole.ADMIN);
            assertThat(user.isActive()).isFalse();
            assertThat(user.getPreferences()).isEqualTo("{\"theme\": \"dark\"}");
            assertThat(user.getCreatedAt()).isEqualTo(now);
            assertThat(user.getUpdatedAt()).isEqualTo(now);
            assertThat(user.getLastLogin()).isEqualTo(now);
        }

        @Test
        @DisplayName("Role enum values work correctly")
        void roleEnumValues() {
            user.setRole(UserRole.USER);
            assertThat(user.getRole()).isEqualTo(UserRole.USER);
            
            user.setRole(UserRole.EDITOR);
            assertThat(user.getRole()).isEqualTo(UserRole.EDITOR);
            
            user.setRole(UserRole.ADMIN);
            assertThat(user.getRole()).isEqualTo(UserRole.ADMIN);
        }
    }

    @Nested
    @DisplayName("Business Logic Tests")
    class BusinessLogicTests {

        @Test
        @DisplayName("Get full name with first and last name")
        void getFullNameComplete() {
            user.setFirstName("John");
            user.setLastName("Doe");
            user.setEmail("john.doe@example.com");
            
            assertThat(user.getFullName()).isEqualTo("John Doe");
        }

        @Test
        @DisplayName("Get full name with only first name")
        void getFullNameFirstOnly() {
            user.setFirstName("John");
            user.setEmail("john.doe@example.com");
            
            assertThat(user.getFullName()).isEqualTo("John");
        }

        @Test
        @DisplayName("Get full name with only last name")
        void getFullNameLastOnly() {
            user.setLastName("Doe");
            user.setEmail("john.doe@example.com");
            
            assertThat(user.getFullName()).isEqualTo("Doe");
        }

        @Test
        @DisplayName("Get full name falls back to email when available")
        void getFullNameFallbackToEmail() {
            user.setClerkId("clerk_123");
            user.setEmail("john.doe@example.com");
            
            assertThat(user.getFullName()).isEqualTo("john.doe@example.com");
        }

        @Test
        @DisplayName("Get full name falls back to clerk ID when no email")
        void getFullNameFallbackToClerkId() {
            user.setClerkId("clerk_123");
            // No email set
            
            assertThat(user.getFullName()).isEqualTo("clerk_123");
        }

        @Test
        @DisplayName("Admin role check works correctly")
        void isAdminCheck() {
            user.setRole(UserRole.ADMIN);
            assertThat(user.isAdmin()).isTrue();
            
            user.setRole(UserRole.EDITOR);
            assertThat(user.isAdmin()).isFalse();
            
            user.setRole(UserRole.USER);
            assertThat(user.isAdmin()).isFalse();
        }

        @Test
        @DisplayName("Can edit content check works correctly")
        void canEditContentCheck() {
            user.setRole(UserRole.ADMIN);
            assertThat(user.canEditContent()).isTrue();
            
            user.setRole(UserRole.EDITOR);
            assertThat(user.canEditContent()).isTrue();
            
            user.setRole(UserRole.USER);
            assertThat(user.canEditContent()).isFalse();
        }

        @Test
        @DisplayName("Update last login sets current time")
        void updateLastLogin() {
            LocalDateTime beforeUpdate = LocalDateTime.now().minusSeconds(1);
            user.updateLastLogin();
            LocalDateTime afterUpdate = LocalDateTime.now().plusSeconds(1);
            
            assertThat(user.getLastLogin()).isBetween(beforeUpdate, afterUpdate);
        }
    }

    @Nested
    @DisplayName("Equality and HashCode Tests")
    class EqualityTests {

        @Test
        @DisplayName("Users with same ID and Clerk ID are equal")
        void usersWithSameIdAreEqual() {
            User user1 = new User();
            user1.setId(userId);
            user1.setClerkId(clerkId);
            
            User user2 = new User();
            user2.setId(userId);
            user2.setClerkId(clerkId);
            
            assertThat(user1).isEqualTo(user2);
            assertThat(user1.hashCode()).isEqualTo(user2.hashCode());
        }

        @Test
        @DisplayName("Users with different IDs are not equal")
        void usersWithDifferentIdsAreNotEqual() {
            User user1 = new User();
            user1.setId(userId);
            user1.setClerkId(clerkId);
            
            User user2 = new User();
            user2.setId(UUID.randomUUID());
            user2.setClerkId("different_clerk_id");
            
            assertThat(user1).isNotEqualTo(user2);
        }

        @Test
        @DisplayName("User equals null returns false")
        void userEqualsNull() {
            user.setId(userId);
            user.setClerkId(clerkId);
            
            assertThat(user).isNotEqualTo(null);
        }

        @Test
        @DisplayName("User equals different class returns false")
        void userEqualsDifferentClass() {
            user.setId(userId);
            user.setClerkId(clerkId);
            
            assertThat(user).isNotEqualTo("not a user");
        }
    }

    @Nested
    @DisplayName("ToString Tests")
    class ToStringTests {

        @Test
        @DisplayName("ToString includes all key fields")
        void toStringIncludesKeyFields() {
            user.setId(userId);
            user.setClerkId(clerkId);
            user.setEmail(email);
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setRole(UserRole.USER);
            user.setActive(true);
            
            String userString = user.toString();
            
            assertThat(userString).contains("User{");
            assertThat(userString).contains("id=" + userId);
            assertThat(userString).contains("clerkId='" + clerkId + "'");
            assertThat(userString).contains("email='" + email + "'");
            assertThat(userString).contains("firstName='" + firstName + "'");
            assertThat(userString).contains("lastName='" + lastName + "'");
            assertThat(userString).contains("role=" + UserRole.USER);
            assertThat(userString).contains("isActive=true");
        }
    }

    @Nested
    @DisplayName("UserRole Enum Tests")
    class UserRoleEnumTests {

        @Test
        @DisplayName("UserRole enum has correct values")
        void userRoleEnumValues() {
            assertThat(UserRole.values()).hasSize(3);
            assertThat(UserRole.valueOf("USER")).isEqualTo(UserRole.USER);
            assertThat(UserRole.valueOf("EDITOR")).isEqualTo(UserRole.EDITOR);
            assertThat(UserRole.valueOf("ADMIN")).isEqualTo(UserRole.ADMIN);
        }

        @Test
        @DisplayName("UserRole enum display names are correct")
        void userRoleDisplayNames() {
            assertThat(UserRole.USER.getDisplayName()).isEqualTo("User");
            assertThat(UserRole.EDITOR.getDisplayName()).isEqualTo("Editor");
            assertThat(UserRole.ADMIN.getDisplayName()).isEqualTo("Admin");
        }

        @Test
        @DisplayName("UserRole enum descriptions are correct")
        void userRoleDescriptions() {
            assertThat(UserRole.USER.getDescription()).isEqualTo("Basic user with assessment taking privileges");
            assertThat(UserRole.EDITOR.getDescription()).isEqualTo("Can create and modify assessment content");
            assertThat(UserRole.ADMIN.getDescription()).isEqualTo("Full administrative access to the system");
        }
    }
}