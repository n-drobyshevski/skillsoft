package app.skillsoft.assessmentbackend;

import app.skillsoft.assessmentbackend.config.TestJacksonConfig;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        Map<String, Object> standardCodes = new HashMap<>();
        Map<String, Object> escoMapping = new HashMap<>();
        escoMapping.put("code", "S7.1.1");
        escoMapping.put("name", "develop organisational strategies");
        escoMapping.put("confidence", "HIGH");
        standardCodes.put("ESCO", escoMapping);

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
        assertThat(saved.getStandardCodes().get("ESCO")).isNotNull();

        @SuppressWarnings("unchecked")
        Map<String, Object> escoData = (Map<String, Object>) saved.getStandardCodes().get("ESCO");
        assertThat(escoData.get("code")).isEqualTo("S7.1.1");
        assertThat(escoData.get("confidence")).isEqualTo("HIGH");
    }

    @Test
    void shouldHandleEmptyStandardCodes() {
        // Given
        Competency competency = new Competency();
        competency.setName("Simple Competency");
        competency.setDescription("A competency without standard codes");
        competency.setCategory(CompetencyCategory.COGNITIVE);
        competency.setLevel(ProficiencyLevel.NOVICE);
        competency.setStandardCodes(new HashMap<>());
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
        assertThat(saved.getStandardCodes()).isEmpty();
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
        Map<String, Object> complexStandardCodes = new HashMap<>();
        
        Map<String, Object> esco = new HashMap<>();
        esco.put("code", "S2.1.1");
        esco.put("name", "communicate with others");
        esco.put("confidence", "HIGH");
        esco.put("lastUpdated", "2024-01-15");
        complexStandardCodes.put("ESCO", esco);
        
        Map<String, Object> onet = new HashMap<>();
        onet.put("code", "2.A.1.b");
        onet.put("name", "Oral Comprehension");
        onet.put("confidence", "VERIFIED");
        onet.put("category", "Abilities");
        complexStandardCodes.put("ONET", onet);

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
        assertThat(saved.getStandardCodes()).hasSize(2);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> savedEsco = (Map<String, Object>) saved.getStandardCodes().get("ESCO");
        assertThat(savedEsco.get("code")).isEqualTo("S2.1.1");
        assertThat(savedEsco.get("lastUpdated")).isEqualTo("2024-01-15");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> savedOnet = (Map<String, Object>) saved.getStandardCodes().get("ONET");
        assertThat(savedOnet.get("category")).isEqualTo("Abilities");
    }

    @Test
    void shouldHandleRussianContent() {
        // Given
        Map<String, Object> standardCodes = new HashMap<>();
        Map<String, Object> escoMapping = new HashMap<>();
        escoMapping.put("code", "S7.1.1");
        escoMapping.put("name", "развитие организационных стратегий");
        escoMapping.put("confidence", "HIGH");
        standardCodes.put("ESCO", escoMapping);

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
        
        @SuppressWarnings("unchecked")
        Map<String, Object> escoData = (Map<String, Object>) saved.getStandardCodes().get("ESCO");
        assertThat(escoData.get("name")).isEqualTo("развитие организационных стратегий");
    }
}