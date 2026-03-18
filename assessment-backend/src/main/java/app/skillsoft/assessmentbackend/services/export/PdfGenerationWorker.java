package app.skillsoft.assessmentbackend.services.export;

import app.skillsoft.assessmentbackend.domain.dto.CompetencyScoreDto;
import app.skillsoft.assessmentbackend.domain.entities.AssessmentGoal;
import app.skillsoft.assessmentbackend.domain.entities.ExportStatus;
import app.skillsoft.assessmentbackend.domain.entities.PdfExportJob;
import app.skillsoft.assessmentbackend.domain.entities.TestResult;
import app.skillsoft.assessmentbackend.domain.entities.TestTemplate;
import app.skillsoft.assessmentbackend.repository.PdfExportJobRepository;
import app.skillsoft.assessmentbackend.repository.TestResultRepository;
import app.skillsoft.assessmentbackend.services.export.svg.ScoreCircleSvg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Component
public class PdfGenerationWorker {

    private static final Logger logger = LoggerFactory.getLogger(PdfGenerationWorker.class);

    private final PdfExportJobRepository jobRepository;
    private final TestResultRepository resultRepository;
    private final PdfTemplateEngine templateEngine;

    @Value("${skillsoft.export.pdf.storage-path:data/pdf-exports}")
    private String storagePath;

    public PdfGenerationWorker(PdfExportJobRepository jobRepository,
                               TestResultRepository resultRepository,
                               PdfTemplateEngine templateEngine) {
        this.jobRepository = jobRepository;
        this.resultRepository = resultRepository;
        this.templateEngine = templateEngine;
    }

    @Async("pdfExportExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void generateAsync(UUID jobId) {
        PdfExportJob job = jobRepository.findById(jobId).orElse(null);
        if (job == null || job.getStatus() == ExportStatus.CANCELLED) {
            return;
        }

        job.setStatus(ExportStatus.GENERATING);
        job.setStartedAt(LocalDateTime.now());
        jobRepository.save(job);

        try {
            TestResult result = resultRepository.findByIdWithSessionAndTemplate(job.getResultId())
                    .orElseThrow(() -> new RuntimeException("Result not found: " + job.getResultId()));

            Map<String, Object> variables = buildTemplateVariables(result, job);
            Locale locale = Locale.forLanguageTag(job.getLocale());

            byte[] pdfBytes = templateEngine.render("manager-summary", variables, locale);

            Path outputDir = Path.of(storagePath);
            Files.createDirectories(outputDir);
            String filename = job.getId() + ".pdf";
            Path outputFile = outputDir.resolve(filename);
            Files.write(outputFile, pdfBytes);

            job.setStatus(ExportStatus.COMPLETED);
            job.setCompletedAt(LocalDateTime.now());
            job.setFilePath(outputFile.toString());
            job.setFileSizeBytes((long) pdfBytes.length);
            jobRepository.save(job);

            logger.info("PDF export {} completed: {} bytes", jobId, pdfBytes.length);
        } catch (Exception e) {
            logger.error("PDF export {} failed: {}", jobId, e.getMessage(), e);
            job.setStatus(ExportStatus.FAILED);
            job.setFailedAt(LocalDateTime.now());
            String msg = e.getMessage() != null ? e.getMessage() : "Unknown error";
            job.setErrorMessage(msg.substring(0, Math.min(msg.length(), 2000)));
            jobRepository.save(job);
        }
    }

    private Map<String, Object> buildTemplateVariables(TestResult result, PdfExportJob job) {
        Map<String, Object> vars = new HashMap<>();

        // Resolve template name and goal from the joined session → template
        TestTemplate template = result.getSession() != null && result.getSession().getTemplate() != null
                ? result.getSession().getTemplate() : null;
        String templateName = template != null ? template.getName() : "Assessment";
        AssessmentGoal goal = template != null ? template.getGoal() : null;

        vars.put("title", "Manager Summary");
        vars.put("candidateName", formatCandidateId(result.getClerkUserId()));
        vars.put("templateName", templateName);
        vars.put("goal", goal != null ? goal.name() : "");
        vars.put("completedAt", result.getCompletedAt() != null
                ? result.getCompletedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : "N/A");

        double overallPct = result.getOverallPercentage() != null ? result.getOverallPercentage() : 0.0;
        boolean passed = result.getPassed() != null ? result.getPassed() : overallPct >= 70.0;
        vars.put("passed", passed);
        vars.put("overallPercentage", Math.round(overallPct));

        // Score circle SVG
        String svgCircle = ScoreCircleSvg.render(overallPct, 100.0, 120);
        vars.put("scoreCircleSvg", svgCircle);

        // Stats
        int answered = result.getQuestionsAnswered() != null ? result.getQuestionsAnswered() : 0;
        int skipped = result.getQuestionsSkipped() != null ? result.getQuestionsSkipped() : 0;
        vars.put("questionsAnswered", answered);
        vars.put("totalQuestions", answered + skipped);
        vars.put("totalTimeSeconds", result.getTotalTimeSeconds() != null ? result.getTotalTimeSeconds() : 0);
        vars.put("duration", formatDuration(result.getTotalTimeSeconds()));

        // Top 3 strengths and bottom 3 development areas (sorted by percentage)
        List<CompetencyScoreDto> scores = result.getCompetencyScores() != null
                ? result.getCompetencyScores() : List.of();

        List<CompetencyScoreDto> sorted = new ArrayList<>(scores);
        sorted.sort(Comparator.comparingDouble((CompetencyScoreDto cs) ->
                cs.getPercentage() != null ? cs.getPercentage() : 0.0).reversed());

        List<String> strengths = new ArrayList<>();
        for (int i = 0; i < Math.min(3, sorted.size()); i++) {
            CompetencyScoreDto cs = sorted.get(i);
            String name = cs.getCompetencyName() != null ? cs.getCompetencyName() : "Unknown";
            double pct = cs.getPercentage() != null ? cs.getPercentage() : 0.0;
            strengths.add(name + " — " + Math.round(pct) + "%");
        }

        List<String> developmentAreas = new ArrayList<>();
        for (int i = sorted.size() - 1; i >= Math.max(0, sorted.size() - 3); i--) {
            CompetencyScoreDto cs = sorted.get(i);
            // Don't duplicate: skip if already in strengths (when <= 3 competencies total)
            String name = cs.getCompetencyName() != null ? cs.getCompetencyName() : "Unknown";
            double pct = cs.getPercentage() != null ? cs.getPercentage() : 0.0;
            String entry = name + " — " + Math.round(pct) + "%";
            if (!strengths.contains(entry)) {
                developmentAreas.add(entry);
            }
        }

        vars.put("strengths", strengths);
        vars.put("developmentAreas", developmentAreas);

        // Recommendation based on goal and pass status (mirrors ManagerSummary.tsx logic)
        vars.put("recommendation", buildRecommendation(goal, passed, overallPct));

        vars.put("generatedAt", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));

        return vars;
    }

    private String formatCandidateId(String clerkUserId) {
        if (clerkUserId == null) return "Anonymous";
        // Show shortened ID: "user_2abc...xyz"
        if (clerkUserId.length() > 20) {
            return clerkUserId.substring(0, 16) + "...";
        }
        return clerkUserId;
    }

    private String formatDuration(Integer totalSeconds) {
        if (totalSeconds == null || totalSeconds == 0) return "N/A";
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        if (minutes == 0) return seconds + "s";
        return minutes + "m " + seconds + "s";
    }

    private String buildRecommendation(AssessmentGoal goal, boolean passed, double overallPct) {
        if (goal == null) return "";

        if (goal == AssessmentGoal.OVERVIEW) {
            if (overallPct >= 80) return "Strong competency profile. Well-suited for a wide range of roles.";
            if (overallPct >= 60) return "Moderate competency profile. Suitable with targeted development.";
            return "Developing competency profile. Significant development recommended.";
        }

        // JOB_FIT / TEAM_FIT
        if (passed) {
            if (overallPct >= 90) return "Strongly recommended. Exceptional match for this role.";
            return "Recommended. Meets role requirements with solid competency alignment.";
        }

        if (overallPct >= 60) return "Conditional consideration. Close to threshold — targeted development could bridge the gap.";
        return "Not recommended at this time. Significant gaps identified.";
    }
}
