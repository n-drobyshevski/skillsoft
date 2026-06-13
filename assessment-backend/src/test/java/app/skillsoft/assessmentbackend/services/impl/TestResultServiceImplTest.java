package app.skillsoft.assessmentbackend.services.impl;

import app.skillsoft.assessmentbackend.domain.dto.TestResultDto;
import app.skillsoft.assessmentbackend.domain.dto.TestResultSummaryDto;
import app.skillsoft.assessmentbackend.domain.entities.AssessmentGoal;
import app.skillsoft.assessmentbackend.domain.entities.TestResult;
import app.skillsoft.assessmentbackend.domain.entities.TestSession;
import app.skillsoft.assessmentbackend.domain.entities.TestTemplate;
import app.skillsoft.assessmentbackend.repository.TestResultRepository;
import app.skillsoft.assessmentbackend.testutils.TestDataFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TestResultServiceImpl} mapping behavior.
 *
 * <p>Regression coverage for the bug where every result was categorized as
 * OVERVIEW in the UI because the template's {@link AssessmentGoal} was never
 * surfaced through {@link TestResultDto}. The "Recent results" tabs
 * (Overview / Job fit / Team fit) rely on this field.</p>
 */
@ExtendWith(MockitoExtension.class)
class TestResultServiceImplTest {

    @Mock
    private TestResultRepository resultRepository;

    @InjectMocks
    private TestResultServiceImpl service;

    private static final String USER = "user_123";

    private TestResult resultForGoal(AssessmentGoal goal) {
        TestTemplate template = TestDataFactory.createTestTemplate(goal);
        TestSession session = TestDataFactory.createTestSession(template, USER);

        TestResult result = new TestResult(session, USER);
        result.setId(UUID.randomUUID());
        result.setOverallScore(80.0);
        result.setOverallPercentage(80.0);
        result.setPassed(true);
        result.setTotalTimeSeconds(600);
        result.setQuestionsAnswered(10);
        result.setQuestionsSkipped(0);
        result.setCompletedAt(LocalDateTime.now());
        return result;
    }

    @Test
    void findByUserDetailed_surfacesJobFitGoalFromTemplate() {
        Pageable pageable = PageRequest.of(0, 5);
        Page<TestResult> page = new PageImpl<>(List.of(resultForGoal(AssessmentGoal.JOB_FIT)));
        when(resultRepository.findByClerkUserIdWithSessionAndTemplate(eq(USER), any(Pageable.class)))
                .thenReturn(page);

        Page<TestResultDto> dtos = service.findByUserDetailed(USER, pageable);

        assertThat(dtos.getContent()).hasSize(1);
        assertThat(dtos.getContent().get(0).goal()).isEqualTo(AssessmentGoal.JOB_FIT);
    }

    @Test
    void findByUserDetailed_surfacesTeamFitGoalFromTemplate() {
        Pageable pageable = PageRequest.of(0, 5);
        Page<TestResult> page = new PageImpl<>(List.of(resultForGoal(AssessmentGoal.TEAM_FIT)));
        when(resultRepository.findByClerkUserIdWithSessionAndTemplate(eq(USER), any(Pageable.class)))
                .thenReturn(page);

        Page<TestResultDto> dtos = service.findByUserDetailed(USER, pageable);

        assertThat(dtos.getContent().get(0).goal()).isEqualTo(AssessmentGoal.TEAM_FIT);
    }

    @Test
    void findByUser_summarySurfacesGoalFromTemplate() {
        // The profile "Recent results" tabs are driven by the summary endpoint,
        // so the lightweight summary DTO must also carry the template goal.
        Pageable pageable = PageRequest.of(0, 5);
        Page<TestResult> page = new PageImpl<>(List.of(resultForGoal(AssessmentGoal.TEAM_FIT)));
        when(resultRepository.findByClerkUserIdWithSessionAndTemplate(eq(USER), any(Pageable.class)))
                .thenReturn(page);

        Page<TestResultSummaryDto> dtos = service.findByUser(USER, pageable);

        assertThat(dtos.getContent()).hasSize(1);
        assertThat(dtos.getContent().get(0).goal()).isEqualTo(AssessmentGoal.TEAM_FIT);
    }

    @Test
    void findById_surfacesGoalFromTemplate() {
        TestResult result = resultForGoal(AssessmentGoal.OVERVIEW);
        when(resultRepository.findByIdWithSessionAndTemplate(result.getId()))
                .thenReturn(java.util.Optional.of(result));

        TestResultDto dto = service.findById(result.getId()).orElseThrow();

        assertThat(dto.goal()).isEqualTo(AssessmentGoal.OVERVIEW);
    }
}
