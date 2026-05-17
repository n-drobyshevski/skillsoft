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
        vars.put("goal", humanizeGoal(goal));
        vars.put("completedAt", result.getCompletedAt() != null
                ? result.getCompletedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : "N/A");

        double overallPct = result.getOverallPercentage() != null ? result.getOverallPercentage() : 0.0;
        boolean passed = result.getPassed() != null ? result.getPassed() : overallPct >= 70.0;
        vars.put("passed", passed);
        vars.put("overallPercentage", Math.round(overallPct));

        // Score display — Flying Saucer has limited SVG support, use HTML-based score
        vars.put("scoreCircleSvg", buildScoreHtml(overallPct, passed));

        // Stats
        int answered = result.getQuestionsAnswered() != null ? result.getQuestionsAnswered() : 0;
        int skipped = result.getQuestionsSkipped() != null ? result.getQuestionsSkipped() : 0;
        vars.put("questionsAnswered", answered);
        vars.put("totalQuestions", answered + skipped);
        vars.put("totalTimeSeconds", result.getTotalTimeSeconds() != null ? result.getTotalTimeSeconds() : 0);
        vars.put("duration", formatDuration(result.getTotalTimeSeconds()));

        // All competencies sorted by percentage (descending) for the ranked table
        List<CompetencyScoreDto> scores = result.getCompetencyScores() != null
                ? result.getCompetencyScores() : List.of();

        List<CompetencyScoreDto> sorted = new ArrayList<>(scores);
        sorted.sort(Comparator.comparingDouble((CompetencyScoreDto cs) ->
                cs.getPercentage() != null ? cs.getPercentage() : 0.0).reversed());

        // Build competency table rows with name, percentage, and proficiency label
        List<Map<String, Object>> competencyRows = new ArrayList<>();
        for (CompetencyScoreDto cs : sorted) {
            Map<String, Object> row = new HashMap<>();
            row.put("name", cs.getCompetencyName() != null && !cs.getCompetencyName().isBlank()
                    ? cs.getCompetencyName() : "Competency");
            double pct = cs.getPercentage() != null ? cs.getPercentage() : 0.0;
            row.put("percentage", Math.round(pct));
            row.put("proficiency", cs.getProficiencyLabel() != null ? cs.getProficiencyLabel() : "");
            row.put("isStrength", pct >= 70.0);
            competencyRows.add(row);
        }
        vars.put("competencyRows", competencyRows);
        vars.put("hasCompetencies", !competencyRows.isEmpty());

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

    private String humanizeGoal(AssessmentGoal goal) {
        if (goal == null) return "";
        return switch (goal) {
            case JOB_FIT -> "Job Fit Assessment";
            case TEAM_FIT -> "Team Fit Assessment";
            case OVERVIEW -> "Competency Overview";
        };
    }

    private String buildScoreHtml(double percentage, boolean passed) {
        String color = passed ? "#166534" : "#92400e";
        String bgColor = passed ? "#dcfce7" : "#fef3c7";
        return String.format(
            "<div style=\"text-align: center; padding: 6mm 0;\">" +
            "<div style=\"display: inline-block; width: 28mm; height: 28mm; border: 3px solid %s; " +
            "border-radius: 50%%; background-color: %s; text-align: center; line-height: 28mm;\">" +
            "<span style=\"font-size: 22pt; font-weight: 700; color: %s;\">%d%%</span>" +
            "</div></div>",
            color, bgColor, color, Math.round(percentage)
        );
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
