package app.skillsoft.assessmentbackend.testutils;

import app.skillsoft.assessmentbackend.config.TestHibernateConfig;
import app.skillsoft.assessmentbackend.config.TestJacksonConfig;
import app.skillsoft.assessmentbackend.config.TestSecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Base class for integration tests providing common Spring Boot configuration.
 *
 * Usage:
 * <pre>
 * class MyControllerIntegrationTest extends BaseIntegrationTest {
 *
 *     @Autowired
 *     private MyRepository repository;
 *
 *     @Test
 *     @WithMockUser(roles = "ADMIN")
 *     void shouldReturnAllEntities() throws Exception {
 *         // Given
 *         var entity = TestDataFactory.createCompetency();
 *         repository.save(entity);
 *
 *         // When & Then
 *         mockMvc.perform(get("/api/v1/competencies"))
 *             .andExpect(status().isOk())
 *             .andExpect(jsonPath("$").isArray());
 *     }
 * }
 * </pre>
 *
 * Key features:
 * - @SpringBootTest: Full application context
 * - @AutoConfigureMockMvc: MockMvc for HTTP testing
 * - @Transactional: Automatic rollback after each test
 * - @ActiveProfiles("test"): Uses H2 database configuration
 * - Test configs: Jackson, Hibernate, Security configurations
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@Import({TestJacksonConfig.class, TestHibernateConfig.class, TestSecurityConfig.class})
public abstract class BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    /**
     * Helper method to create and persist a test admin user.
     * Note: Call this when you need a user in the database for your test.
     *
     * @param userRepository The repository to save the user
     * @return The persisted User entity
     */
    protected app.skillsoft.assessmentbackend.domain.entities.User createAndSaveAdminUser(
            app.skillsoft.assessmentbackend.repository.UserRepository userRepository) {
        var user = TestDataFactory.createUser(
            app.skillsoft.assessmentbackend.domain.entities.UserRole.ADMIN
        );
        return userRepository.save(user);
    }

    /**
     * Helper method to create and persist a test editor user.
     *
     * @param userRepository The repository to save the user
     * @return The persisted User entity
     */
    protected app.skillsoft.assessmentbackend.domain.entities.User createAndSaveEditorUser(
            app.skillsoft.assessmentbackend.repository.UserRepository userRepository) {
        var user = TestDataFactory.createUser(
            app.skillsoft.assessmentbackend.domain.entities.UserRole.EDITOR
        );
        return userRepository.save(user);
    }

    /**
     * Helper method to create and persist a test basic user.
     *
     * @param userRepository The repository to save the user
     * @return The persisted User entity
     */
    protected app.skillsoft.assessmentbackend.domain.entities.User createAndSaveBasicUser(
            app.skillsoft.assessmentbackend.repository.UserRepository userRepository) {
        var user = TestDataFactory.createUser(
            app.skillsoft.assessmentbackend.domain.entities.UserRole.USER
        );
        return userRepository.save(user);
    }
}
