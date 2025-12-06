package app.skillsoft.assessmentbackend;

import app.skillsoft.assessmentbackend.config.TestJacksonConfig;
import app.skillsoft.assessmentbackend.domain.dto.StandardCodesDto;
import app.skillsoft.assessmentbackend.domain.entities.ApprovalStatus;
import app.skillsoft.assessmentbackend.domain.entities.Competency;
import app.skillsoft.assessmentbackend.domain.entities.CompetencyCategory;
import app.skillsoft.assessmentbackend.domain.entities.ProficiencyLevel;
import app.skillsoft.assessmentbackend.repository.CompetencyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Simplified Competency Repository JSONB tests following the working pattern
 */
@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestJacksonConfig.class)
public class CompetencyRepositoryJsonbTest {

    @Autowired
    private CompetencyRepository competencyRepository;

    @BeforeEach
    void setup() {
        competencyRepository.deleteAll();
    }

    @Test
    void shouldSaveAndRetrieveCompetencyWithStandardCodes() {
        // Given
        StandardCodesDto standardCodes = StandardCodesDto.builder()
                .escoRef("http://data.europa.eu/esco/skill/abc123-def456-789",
                        "develop organisational strategies", "skill")
                .bigFive("CONSCIENTIOUSNESS")
                .build();

        Competency competency = new Competency();
        competency.setName("Strategic Leadership");
        competency.setDescription("Ability to lead teams towards strategic goals");
        competency.setCategory(CompetencyCategory.LEADERSHIP);
        competency.setLevel(ProficiencyLevel.ADVANCED);
        competency.setStandardCodes(standardCodes);
        competency.setActive(true);
        competency.setApprovalStatus(ApprovalStatus.APPROVED);
        competency.setVersion(1);
        competency.setCreatedAt(LocalDateTime.now());
        competency.setLastModified(LocalDateTime.now());

        // When
        Competency saved = competencyRepository.save(competency);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getStandardCodes()).isNotNull();
        assertThat(saved.getStandardCodes().hasEscoMapping()).isTrue();
        assertThat(saved.getStandardCodes().escoRef()).isNotNull();
        assertThat(saved.getStandardCodes().escoRef().title()).isEqualTo("develop organisational strategies");
    }

    @Test
    void shouldHandleEmptyStandardCodes() {
        // Given
        Competency competency = new Competency();
        competency.setName("Simple Competency");
        competency.setDescription("A competency without standard codes");
        competency.setCategory(CompetencyCategory.COGNITIVE);
        competency.setLevel(ProficiencyLevel.NOVICE);
        competency.setStandardCodes(new StandardCodesDto()); // Empty DTO with all null fields
        competency.setActive(true);
        competency.setApprovalStatus(ApprovalStatus.DRAFT);
        competency.setVersion(1);
        competency.setCreatedAt(LocalDateTime.now());
        competency.setLastModified(LocalDateTime.now());

        // When
        Competency saved = competencyRepository.save(competency);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getStandardCodes()).isNotNull();
        assertThat(saved.getStandardCodes().hasAnyMapping()).isFalse();
    }

    @Test
    void shouldHandleNullStandardCodes() {
        // Given
        Competency competency = new Competency();
        competency.setName("Simple Competency");
        competency.setDescription("A competency without standard codes");
        competency.setCategory(CompetencyCategory.COGNITIVE);
        competency.setLevel(ProficiencyLevel.NOVICE);
        competency.setStandardCodes(null);
        competency.setActive(true);
        competency.setApprovalStatus(ApprovalStatus.DRAFT);
        competency.setVersion(1);
        competency.setCreatedAt(LocalDateTime.now());
        competency.setLastModified(LocalDateTime.now());

        // When
        Competency saved = competencyRepository.save(competency);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getStandardCodes()).isNull();
    }

    @Test
    void shouldHandleComplexStandardCodesStructure() {
        // Given
        StandardCodesDto complexStandardCodes = StandardCodesDto.builder()
                .escoRef("http://data.europa.eu/esco/skill/abc123-def456-789",
                        "communicate with others", "skill")
                .onetRef("2.A.1.b", "Oral Comprehension", "ability")
                .bigFive("EXTRAVERSION")
                .build();

        Competency competency = new Competency();
        competency.setName("Communication Skills");
        competency.setDescription("Advanced communication competency");
        competency.setCategory(CompetencyCategory.COMMUNICATION);
        competency.setLevel(ProficiencyLevel.EXPERT);
        competency.setStandardCodes(complexStandardCodes);
        competency.setActive(true);
        competency.setApprovalStatus(ApprovalStatus.APPROVED);
        competency.setVersion(1);
        competency.setCreatedAt(LocalDateTime.now());
        competency.setLastModified(LocalDateTime.now());

        // When
        Competency saved = competencyRepository.save(competency);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getStandardCodes().hasEscoMapping()).isTrue();
        assertThat(saved.getStandardCodes().hasOnetMapping()).isTrue();
        assertThat(saved.getStandardCodes().escoRef().title()).isEqualTo("communicate with others");
        assertThat(saved.getStandardCodes().onetRef().code()).isEqualTo("2.A.1.b");
        assertThat(saved.getStandardCodes().onetRef().title()).isEqualTo("Oral Comprehension");
    }

    @Test
    void shouldHandleRussianContent() {
        // Given
        StandardCodesDto standardCodes = StandardCodesDto.builder()
                .escoRef("http://data.europa.eu/esco/skill/abc123-def456-789",
                        "развитие организационных стратегий", "skill")
                .bigFive("CONSCIENTIOUSNESS")
                .build();

        Competency competency = new Competency();
        competency.setName("Стратегическое лидерство");
        competency.setDescription("Способность вести команду к достижению стратегических целей");
        competency.setCategory(CompetencyCategory.LEADERSHIP);
        competency.setLevel(ProficiencyLevel.ADVANCED);
        competency.setStandardCodes(standardCodes);
        competency.setActive(true);
        competency.setApprovalStatus(ApprovalStatus.APPROVED);
        competency.setVersion(1);
        competency.setCreatedAt(LocalDateTime.now());
        competency.setLastModified(LocalDateTime.now());

        // When
        Competency saved = competencyRepository.save(competency);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Стратегическое лидерство");
        assertThat(saved.getDescription()).contains("команду");
        assertThat(saved.getStandardCodes()).isNotNull();
        assertThat(saved.getStandardCodes().escoRef().title()).isEqualTo("развитие организационных стратегий");
    }
}