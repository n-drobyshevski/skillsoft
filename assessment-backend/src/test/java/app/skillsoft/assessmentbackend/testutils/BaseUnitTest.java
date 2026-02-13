package app.skillsoft.assessmentbackend.testutils;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Base class for unit tests providing common Mockito configuration.
 *
 * Usage:
 * <pre>
 * class MyServiceTest extends BaseUnitTest {
 *     @Mock
 *     private MyRepository repository;
 *
 *     @InjectMocks
 *     private MyServiceImpl service;
 *
 *     @Test
 *     void shouldDoSomething() {
 *         // Given - use TestDataFactory for test data
 *         var entity = TestDataFactory.createCompetency();
 *         when(repository.findById(any())).thenReturn(Optional.of(entity));
 *
 *         // When
 *         var result = service.findById(entity.getId());
 *
 *         // Then
 *         assertThat(result).isPresent();
 *     }
 * }
 * </pre>
 *
 * Key features:
 * - MockitoExtension for automatic mock initialization
 * - Access to TestDataFactory for bilingual test entities
 * - Recommended pattern: Given-When-Then
 */
@ExtendWith(MockitoExtension.class)
public abstract class BaseUnitTest {

    /**
     * Helper method to create a test user with ADMIN role.
     * @return A new User entity with admin privileges
     */
    protected app.skillsoft.assessmentbackend.domain.entities.User createAdminUser() {
        return TestDataFactory.createUser(app.skillsoft.assessmentbackend.domain.entities.UserRole.ADMIN);
    }

    /**
     * Helper method to create a test user with EDITOR role.
     * @return A new User entity with editor privileges
     */
    protected app.skillsoft.assessmentbackend.domain.entities.User createEditorUser() {
        return TestDataFactory.createUser(app.skillsoft.assessmentbackend.domain.entities.UserRole.EDITOR);
    }

    /**
     * Helper method to create a test user with USER role.
     * @return A new User entity with basic user privileges
     */
    protected app.skillsoft.assessmentbackend.domain.entities.User createBasicUser() {
        return TestDataFactory.createUser(app.skillsoft.assessmentbackend.domain.entities.UserRole.USER);
    }
}
