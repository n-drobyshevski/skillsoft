package app.skillsoft.assessmentbackend.services;

import app.skillsoft.assessmentbackend.domain.dto.CompetencyScoreDto;
import app.skillsoft.assessmentbackend.domain.dto.blueprint.TeamFitBlueprint;
import app.skillsoft.assessmentbackend.domain.dto.comparison.*;
import app.skillsoft.assessmentbackend.domain.entities.*;
import app.skillsoft.assessmentbackend.repository.TestResultRepository;
import app.skillsoft.assessmentbackend.services.external.TeamService;
import app.skillsoft.assessmentbackend.services.external.TeamService.TeamMemberProfile;
import app.skillsoft.assessmentbackend.services.external.TeamService.TeamProfile;
import app.skillsoft.assessmentbackend.services.impl.CandidateComparisonServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CandidateComparisonService implementation.
 *
 * Tests cover:
 * - Input validation (count, template match, goal type)
 * - Candidate ranking across dimensions
 * - Gap coverage identification
 * - Pairwise complementarity scoring
 * - Graceful fallback when team profile is unavailable
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CandidateComparison Service Tests")
class CandidateComparisonServiceTest {

    @Mock
    private TestResultRepository testResultRepository;

    @Mock
    private TeamService teamService;

    private CandidateComparisonServiceImpl service;

    private UUID templateId;
    private UUID teamId;
    private UUID competencyId1;
    private UUID competencyId2;
    private UUID competencyId3;
    private TestTemplate template;

    @BeforeEach
    void setUp() {
        service = new CandidateComparisonServiceImpl(testResultRepository, teamService);

        templateId = UUID.randomUUID();
        teamId = UUID.randomUUID();
        competencyId1 = UUID.randomUUID();
        competencyId2 = UUID.randomUUID();
        competencyId3 = UUID.randomUUID();

        template = new TestTemplate();
        template.setId(templateId);
        template.setName("Team Fit Assessment");
        template.setGoal(AssessmentGoal.TEAM_FIT);

        TeamFitBlueprint blueprint = new TeamFitBlueprint();
        blueprint.setTeamId(teamId);
        blueprint.setTargetRole("Backend Developer");
        blueprint.setSaturationThreshold(0.75);
        template.setTypedBlueprint(blueprint);
    }

    // ==================== HELPERS ====================

    private TestResult createResult(UUID resultId, String displayName, Double overallPercentage,
                                     Boolean passed, Map<String, Double> bigFive,
                                     Map<String, Object> extendedMetrics,
                                     List<CompetencyScoreDto> competencyScores) {
        TestSession session = new TestSession();
        session.setId(UUID.randomUUID());
        session.setTemplate(template);
        session.setClerkUserId(displayName);

        TestResult result = new TestResult();
        result.setId(resultId);
        result.setSession(session);
        result.setClerkUserId(displayName);
        result.setOverallPercentage(overallPercentage);
        result.setPassed(passed);
        result.setBigFiveProfile(bigFive);
        result.setExtendedMetrics(extendedMetrics);
        result.setCompetencyScores(competencyScores);
        result.setCompletedAt(LocalDateTime.now());
        return result;
    }

    private Map<String, Object> buildExtendedMetrics(Double diversityRatio, Double saturationRatio,
                                                      Double teamFitMultiplier, Double personalityCompatibility) {
        Map<String, Object> metrics = new HashMap<>();
        if (diversityRatio != null) metrics.put("diversityRatio", diversityRatio);
        if (saturationRatio != null) metrics.put("saturationRatio", saturationRatio);
        if (teamFitMultiplier != null) metrics.put("teamFitMultiplier", teamFitMultiplier);
        if (personalityCompatibility != null) metrics.put("personalityCompatibility", personalityCompatibility);
        return metrics;
    }

    private CompetencyScoreDto buildCompetencyScore(UUID competencyId, String name, Double percentage) {
        return new CompetencyScoreDto(competencyId, name, percentage * 0.6, 60.0, percentage);
    }

    private TeamProfile buildTeamProfile(Map<UUID, Double> saturation) {
        TeamMemberProfile member = new TeamMemberProfile(
            UUID.randomUUID(), "Team Member", "Developer",
            Map.of(competencyId1, 3.5), Map.of("OPENNESS", 70.0)
        );
        return new TeamProfile(
            teamId, "Alpha Team", List.of(member, member, member),
            saturation,
            Map.of("OPENNESS", 65.0, "CONSCIENTIOUSNESS", 72.0),
            List.of(),
            Map.of()
        );
    }

    private List<TestResult> buildTwoCandidates() {
        UUID resultId1 = UUID.randomUUID();
        UUID resultId2 = UUID.randomUUID();

        TestResult result1 = createResult(resultId1, "Alice",
            85.0, true,
            Map.of("OPENNESS", 80.0, "CONSCIENTIOUSNESS", 70.0),
            buildExtendedMetrics(0.8, 0.6, 1.2, 0.85),
            List.of(
                buildCompetencyScore(competencyId1, "Leadership", 90.0),
                buildCompetencyScore(competencyId2, "Communication", 80.0),
                buildCompetencyScore(competencyId3, "Technical Skills", 85.0)
            ));

        TestResult result2 = createResult(resultId2, "Bob",
            72.0, true,
            Map.of("OPENNESS", 60.0, "CONSCIENTIOUSNESS", 85.0),
            buildExtendedMetrics(0.6, 0.7, 1.1, 0.75),
            List.of(
                buildCompetencyScore(competencyId1, "Leadership", 60.0),
                buildCompetencyScore(competencyId2, "Communication", 85.0),
                buildCompetencyScore(competencyId3, "Technical Skills", 70.0)
            ));

        return List.of(result1, result2);
    }

    // ==================== VALIDATION TESTS ====================

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should throw when only one result is provided")
        void compareResults_withOnlyOneResult_throwsIllegalArgument() {
            UUID singleId = UUID.randomUUID();

            assertThatThrownBy(() -> service.compareResults(List.of(singleId), templateId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("At least 2");
        }

        @Test
        @DisplayName("Should throw when six results are provided")
        void compareResults_withSixResults_throwsIllegalArgument() {
            List<UUID> sixIds = List.of(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()
            );

            assertThatThrownBy(() -> service.compareResults(sixIds, templateId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("At most 5");
        }

        @Test
        @DisplayName("Should throw when a result is not found in the database")
        void compareResults_withMissingResult_throwsIllegalArgument() {
            UUID existingId = UUID.randomUUID();
            UUID missingId = UUID.randomUUID();
            List<UUID> ids = List.of(existingId, missingId);

            TestResult existing = createResult(existingId, "Alice", 80.0, true,
                Map.of(), Map.of(), List.of());

            when(testResultRepository.findAllByIdInWithSessionAndTemplate(ids))
                .thenReturn(List.of(existing));

            assertThatThrownBy(() -> service.compareResults(ids, templateId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Results not found");
        }

        @Test
        @DisplayName("Should throw when results belong to different templates")
        void compareResults_withMismatchedTemplates_throwsIllegalArgument() {
            UUID resultId1 = UUID.randomUUID();
            UUID resultId2 = UUID.randomUUID();
            UUID otherTemplateId = UUID.randomUUID();
            List<UUID> ids = List.of(resultId1, resultId2);

            // Result 1 belongs to the expected template
            TestResult result1 = createResult(resultId1, "Alice", 80.0, true,
                Map.of(), Map.of(), List.of());

            // Result 2 belongs to a different template
            TestTemplate otherTemplate = new TestTemplate();
            otherTemplate.setId(otherTemplateId);
            otherTemplate.setName("Other Assessment");
            otherTemplate.setGoal(AssessmentGoal.TEAM_FIT);

            TestSession session2 = new TestSession();
            session2.setId(UUID.randomUUID());
            session2.setTemplate(otherTemplate);
            session2.setClerkUserId("Bob");

            TestResult result2 = new TestResult();
            result2.setId(resultId2);
            result2.setSession(session2);
            result2.setClerkUserId("Bob");
            result2.setOverallPercentage(75.0);
            result2.setCompletedAt(LocalDateTime.now());

            when(testResultRepository.findAllByIdInWithSessionAndTemplate(ids))
                .thenReturn(List.of(result1, result2));

            assertThatThrownBy(() -> service.compareResults(ids, templateId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("belongs to template");
        }

        @Test
        @DisplayName("Should throw when template goal is not TEAM_FIT")
        void compareResults_withNonTeamFitGoal_throwsIllegalArgument() {
            UUID resultId1 = UUID.randomUUID();
            UUID resultId2 = UUID.randomUUID();
            UUID overviewTemplateId = UUID.randomUUID();
            List<UUID> ids = List.of(resultId1, resultId2);

            TestTemplate overviewTemplate = new TestTemplate();
            overviewTemplate.setId(overviewTemplateId);
            overviewTemplate.setName("Overview Assessment");
            overviewTemplate.setGoal(AssessmentGoal.OVERVIEW);

            TestSession session1 = new TestSession();
            session1.setId(UUID.randomUUID());
            session1.setTemplate(overviewTemplate);
            TestResult result1 = new TestResult();
            result1.setId(resultId1);
            result1.setSession(session1);
            result1.setCompletedAt(LocalDateTime.now());

            TestSession session2 = new TestSession();
            session2.setId(UUID.randomUUID());
            session2.setTemplate(overviewTemplate);
            TestResult result2 = new TestResult();
            result2.setId(resultId2);
            result2.setSession(session2);
            result2.setCompletedAt(LocalDateTime.now());

            when(testResultRepository.findAllByIdInWithSessionAndTemplate(ids))
                .thenReturn(List.of(result1, result2));

            assertThatThrownBy(() -> service.compareResults(ids, overviewTemplateId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("TEAM_FIT");
        }
    }

    // ==================== HAPPY PATH TESTS ====================

    @Nested
    @DisplayName("Happy Path Tests")
    class HappyPathTests {

        @Test
        @DisplayName("Should return ranked comparison for two candidates")
        void compareResults_withTwoCandidates_returnsRankedComparison() {
            List<TestResult> results = buildTwoCandidates();
            List<UUID> ids = results.stream().map(TestResult::getId).toList();

            Map<UUID, Double> saturation = Map.of(
                competencyId1, 0.7,
                competencyId2, 0.3,
                competencyId3, 0.8
            );

            when(testResultRepository.findAllByIdInWithSessionAndTemplate(ids)).thenReturn(results);
            when(teamService.getTeamProfile(teamId)).thenReturn(Optional.of(buildTeamProfile(saturation)));

            CandidateComparisonDto comparison = service.compareResults(ids, templateId);

            assertThat(comparison).isNotNull();
            assertThat(comparison.templateId()).isEqualTo(templateId);
            assertThat(comparison.templateName()).isEqualTo("Team Fit Assessment");
            assertThat(comparison.teamId()).isEqualTo(teamId);
            assertThat(comparison.targetRole()).isEqualTo("Backend Developer");
            assertThat(comparison.candidates()).hasSize(2);
            assertThat(comparison.competencyComparison()).hasSize(3);
            assertThat(comparison.teamCompetencySaturation()).isNotEmpty();
        }

        @Test
        @DisplayName("Should rank candidates correctly by overall percentage")
        void compareResults_ranksCorrectlyByOverallPercentage() {
            List<TestResult> results = buildTwoCandidates();
            List<UUID> ids = results.stream().map(TestResult::getId).toList();

            when(testResultRepository.findAllByIdInWithSessionAndTemplate(ids)).thenReturn(results);
            when(teamService.getTeamProfile(teamId)).thenReturn(Optional.of(
                buildTeamProfile(Map.of(competencyId1, 0.7, competencyId2, 0.8, competencyId3, 0.9))
            ));

            CandidateComparisonDto comparison = service.compareResults(ids, templateId);

            // Alice has 85%, Bob has 72% -> Alice should be rank 1
            CandidateSummaryDto alice = comparison.candidates().stream()
                .filter(c -> "Alice".equals(c.displayName()))
                .findFirst().orElseThrow();
            CandidateSummaryDto bob = comparison.candidates().stream()
                .filter(c -> "Bob".equals(c.displayName()))
                .findFirst().orElseThrow();

            assertThat(alice.overallRank()).isEqualTo(1);
            assertThat(bob.overallRank()).isEqualTo(2);

            // Alice has diversityRatio 0.8, Bob has 0.6 -> Alice diversityRank 1
            assertThat(alice.diversityRank()).isEqualTo(1);
            assertThat(bob.diversityRank()).isEqualTo(2);

            // Alice has personalityCompatibility 0.85, Bob has 0.75 -> Alice personalityRank 1
            assertThat(alice.personalityRank()).isEqualTo(1);
            assertThat(bob.personalityRank()).isEqualTo(2);
        }
    }

    // ==================== GAP COVERAGE TESTS ====================

    @Nested
    @DisplayName("Gap Coverage Tests")
    class GapCoverageTests {

        @Test
        @DisplayName("Should identify gaps and coverage correctly")
        void compareResults_gapCoverageIdentifiesCorrectGaps() {
            List<TestResult> results = buildTwoCandidates();
            List<UUID> ids = results.stream().map(TestResult::getId).toList();

            // competencyId2 (Communication) has saturation 0.3 (below 0.5 threshold) -> is a gap
            // competencyId1 (Leadership) has saturation 0.4 -> is a gap
            // competencyId3 (Technical Skills) has saturation 0.8 -> NOT a gap
            Map<UUID, Double> saturation = Map.of(
                competencyId1, 0.4,
                competencyId2, 0.3,
                competencyId3, 0.8
            );

            when(testResultRepository.findAllByIdInWithSessionAndTemplate(ids)).thenReturn(results);
            when(teamService.getTeamProfile(teamId)).thenReturn(Optional.of(buildTeamProfile(saturation)));

            CandidateComparisonDto comparison = service.compareResults(ids, templateId);

            // Two competencies should be flagged as team gaps
            List<CompetencyComparisonDto> gaps = comparison.competencyComparison().stream()
                .filter(CompetencyComparisonDto::isTeamGap)
                .toList();
            assertThat(gaps).hasSize(2);

            // Gap coverage matrix should match
            assertThat(comparison.gapCoverageMatrix()).hasSize(2);

            // For Leadership gap: Alice has 90%, Bob has 60% -> both cover (>= 50%), Alice is best
            GapCoverageEntryDto leadershipGap = comparison.gapCoverageMatrix().stream()
                .filter(g -> g.competencyId().equals(competencyId1))
                .findFirst().orElseThrow();
            assertThat(leadershipGap.candidateCoverage()).hasSize(2);
            assertThat(leadershipGap.bestCandidateId()).isEqualTo(results.get(0).getId()); // Alice

            // For Communication gap: Alice has 80%, Bob has 85% -> both cover, Bob is best
            GapCoverageEntryDto commGap = comparison.gapCoverageMatrix().stream()
                .filter(g -> g.competencyId().equals(competencyId2))
                .findFirst().orElseThrow();
            assertThat(commGap.bestCandidateId()).isEqualTo(results.get(1).getId()); // Bob
        }
    }

    // ==================== COMPLEMENTARITY TESTS ====================

    @Nested
    @DisplayName("Complementarity Tests")
    class ComplementarityTests {

        @Test
        @DisplayName("Should compute pairwise complementarity scores")
        void compareResults_complementarityComputesPairScores() {
            List<TestResult> results = buildTwoCandidates();
            List<UUID> ids = results.stream().map(TestResult::getId).toList();

            // Two gaps: competencyId1 and competencyId2
            Map<UUID, Double> saturation = Map.of(
                competencyId1, 0.4,
                competencyId2, 0.3,
                competencyId3, 0.8
            );

            when(testResultRepository.findAllByIdInWithSessionAndTemplate(ids)).thenReturn(results);
            when(teamService.getTeamProfile(teamId)).thenReturn(Optional.of(buildTeamProfile(saturation)));

            CandidateComparisonDto comparison = service.compareResults(ids, templateId);

            // With 2 candidates, there should be exactly 1 pair
            assertThat(comparison.complementarityPairs()).hasSize(1);

            CandidatePairComplementarityDto pair = comparison.complementarityPairs().get(0);
            assertThat(pair.totalTeamGaps()).isEqualTo(2);

            // Both Alice and Bob cover both gaps (all scores >= 50%)
            // So combined they cover 2/2 gaps = 100% complementarity
            assertThat(pair.combinedGapsCovered()).isEqualTo(2);
            assertThat(pair.complementarityScore()).isEqualTo(100.0);
        }

        @Test
        @DisplayName("Should generate correct number of pairs for multiple candidates")
        void compareResults_withThreeCandidates_generatesThreePairs() {
            UUID resultId1 = UUID.randomUUID();
            UUID resultId2 = UUID.randomUUID();
            UUID resultId3 = UUID.randomUUID();

            TestResult result1 = createResult(resultId1, "Alice", 85.0, true,
                Map.of(), buildExtendedMetrics(0.8, 0.6, 1.2, 0.85),
                List.of(buildCompetencyScore(competencyId1, "Leadership", 90.0)));
            TestResult result2 = createResult(resultId2, "Bob", 72.0, true,
                Map.of(), buildExtendedMetrics(0.6, 0.7, 1.1, 0.75),
                List.of(buildCompetencyScore(competencyId1, "Leadership", 60.0)));
            TestResult result3 = createResult(resultId3, "Carol", 78.0, true,
                Map.of(), buildExtendedMetrics(0.7, 0.5, 1.0, 0.80),
                List.of(buildCompetencyScore(competencyId1, "Leadership", 75.0)));

            List<TestResult> results = List.of(result1, result2, result3);
            List<UUID> ids = List.of(resultId1, resultId2, resultId3);

            Map<UUID, Double> saturation = Map.of(competencyId1, 0.3);

            when(testResultRepository.findAllByIdInWithSessionAndTemplate(ids)).thenReturn(results);
            when(teamService.getTeamProfile(teamId)).thenReturn(Optional.of(buildTeamProfile(saturation)));

            CandidateComparisonDto comparison = service.compareResults(ids, templateId);

            // 3 candidates => C(3,2) = 3 pairs
            assertThat(comparison.complementarityPairs()).hasSize(3);
        }
    }

    // ==================== TEAM PROFILE FALLBACK TESTS ====================

    @Nested
    @DisplayName("Team Profile Fallback Tests")
    class TeamProfileFallbackTests {

        @Test
        @DisplayName("Should fall back gracefully when team profile is unavailable")
        void compareResults_withNoTeamProfile_fallsBackGracefully() {
            List<TestResult> results = buildTwoCandidates();
            List<UUID> ids = results.stream().map(TestResult::getId).toList();

            when(testResultRepository.findAllByIdInWithSessionAndTemplate(ids)).thenReturn(results);
            when(teamService.getTeamProfile(teamId)).thenReturn(Optional.empty());

            CandidateComparisonDto comparison = service.compareResults(ids, templateId);

            assertThat(comparison).isNotNull();
            assertThat(comparison.teamSize()).isEqualTo(0);
            assertThat(comparison.teamCompetencySaturation()).isEmpty();

            // Without team saturation data, no competencies are flagged as gaps
            assertThat(comparison.competencyComparison()).hasSize(3);
            long gapCount = comparison.competencyComparison().stream()
                .filter(CompetencyComparisonDto::isTeamGap).count();
            assertThat(gapCount).isZero();

            // No gaps means no gap coverage entries and no complementarity pairs
            assertThat(comparison.gapCoverageMatrix()).isEmpty();
            assertThat(comparison.complementarityPairs()).isEmpty();

            // Candidates should still be ranked
            assertThat(comparison.candidates()).hasSize(2);
            assertThat(comparison.candidates().get(0).overallRank()).isIn(1, 2);
        }
    }
}
