package app.skillsoft.assessmentbackend.repository;

import app.skillsoft.assessmentbackend.config.TestJacksonConfig;
import app.skillsoft.assessmentbackend.config.TestHibernateConfig;
import app.skillsoft.assessmentbackend.domain.dto.StandardCodesDto;
import app.skillsoft.assessmentbackend.domain.entities.Competency;
import app.skillsoft.assessmentbackend.domain.entities.CompetencyCategory;
import app.skillsoft.assessmentbackend.domain.entities.ProficiencyLevel;
import app.skillsoft.assessmentbackend.domain.entities.ApprovalStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Simplified tests for CompetencyRepository focusing on critical JSONB functionality
 */
@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestJacksonConfig.class, TestHibernateConfig.class})
class CompetencyRepositorySimpleTest {

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void shouldSaveAndRetrieveCompetencyWithJsonbField() {
        // Given
        StandardCodesDto standardCodes = StandardCodesDto.builder()
                .onetRef("2.B.1.a", "Oral Comprehension", "ability")
                .escoRef("http://data.europa.eu/esco/skill/abc123-def456-789",
                        "Communication Skills", "skill")
                .build();

        Competency competency = new Competency();
        competency.setName("Test Competency");
        competency.setDescription("Test Description");
        competency.setCategory(CompetencyCategory.COGNITIVE);
        competency.setLevel(ProficiencyLevel.PROFICIENT);
        competency.setStandardCodes(standardCodes);
        competency.setApprovalStatus(ApprovalStatus.APPROVED);
        competency.setActive(true);
        competency.setVersion(1);
        competency.setCreatedAt(LocalDateTime.now());
        competency.setLastModified(LocalDateTime.now());

        // When
        Competency saved = entityManager.persistAndFlush(competency);
        
        // Use native query to verify JSON is stored properly in H2
        Object result = entityManager.getEntityManager().createNativeQuery(
            "SELECT name, standard_codes FROM competencies WHERE id = ?")
            .setParameter(1, saved.getId())
            .getSingleResult();
        
        Object[] row = (Object[]) result;
        String name = (String) row[0];
        // H2 may return JSON as byte array, convert to string
        String jsonData = row[1] != null ? 
            (row[1] instanceof String ? (String) row[1] : new String((byte[]) row[1])) : null;
        
        // Then
        assertThat(name).isEqualTo("Test Competency");
        assertThat(jsonData).contains("onet_ref");
        assertThat(jsonData).contains("2.B.1.a");
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getStandardCodes().hasOnetMapping()).isTrue();
        assertThat(saved.getStandardCodes().hasEscoMapping()).isTrue();
    }

    @Test
    void shouldHandleNullJsonbField() {
        // Given
        Competency competency = new Competency();
        competency.setName("Competency Without Standards");
        competency.setDescription("Test Description");
        competency.setCategory(CompetencyCategory.COMMUNICATION);
        competency.setLevel(ProficiencyLevel.NOVICE);
        competency.setApprovalStatus(ApprovalStatus.DRAFT);
        competency.setStandardCodes(null);
        competency.setActive(true);
        competency.setVersion(1);
        competency.setCreatedAt(LocalDateTime.now());
        competency.setLastModified(LocalDateTime.now());

        // When
        Competency saved = entityManager.persistAndFlush(competency);
        
        // Use native query to verify NULL is stored properly
        Object result = entityManager.getEntityManager().createNativeQuery(
            "SELECT name, standard_codes FROM competencies WHERE id = ?")
            .setParameter(1, saved.getId())
            .getSingleResult();
        
        Object[] row = (Object[]) result;
        String name = (String) row[0];
        String jsonData = (String) row[1];

        // Then
        assertThat(name).isEqualTo("Competency Without Standards");
        assertThat(jsonData).isNull();
        assertThat(saved.getId()).isNotNull();
    }

    @Test
    void shouldSaveCompetencyWithRussianText() {
        // Given
        StandardCodesDto standardCodes = StandardCodesDto.builder()
                .onetRef("2.A.1.a", "Устное понимание", "ability")
                .build();

        Competency competency = new Competency();
        competency.setName("Компетенция по программированию");
        competency.setDescription("Описание компетенции на русском языке");
        competency.setCategory(CompetencyCategory.CRITICAL_THINKING);
        competency.setLevel(ProficiencyLevel.ADVANCED);
        competency.setApprovalStatus(ApprovalStatus.APPROVED);
        competency.setStandardCodes(standardCodes);
        competency.setActive(true);
        competency.setVersion(1);
        competency.setCreatedAt(LocalDateTime.now());
        competency.setLastModified(LocalDateTime.now());

        // When
        Competency saved = entityManager.persistAndFlush(competency);
        
        // Use native query to verify Russian text and JSON are stored properly
        Object result = entityManager.getEntityManager().createNativeQuery(
            "SELECT name, description, standard_codes FROM competencies WHERE id = ?")
            .setParameter(1, saved.getId())
            .getSingleResult();
        
        Object[] row = (Object[]) result;
        
        String name = (String) row[0];
        String description = (String) row[1];
        // H2 may return JSON as byte array, convert to string
        String jsonData = row[2] != null ? 
            (row[2] instanceof String ? (String) row[2] : new String((byte[]) row[2])) : null;

        // Then
        assertThat(name).isEqualTo("Компетенция по программированию");
        assertThat(description).isEqualTo("Описание компетенции на русском языке");
        assertThat(jsonData).contains("onet_ref");
        assertThat(jsonData).contains("2.A.1.a");
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getStandardCodes().hasOnetMapping()).isTrue();
    }
}