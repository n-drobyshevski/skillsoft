package app.skillsoft.assessmentbackend.services.export;

import app.skillsoft.assessmentbackend.domain.entities.ExportStatus;
import app.skillsoft.assessmentbackend.domain.entities.PdfExportJob;
import app.skillsoft.assessmentbackend.domain.entities.TestResult;
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
            TestResult result = resultRepository.findById(job.getResultId())
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
        vars.put("title", "Manager Summary");
        vars.put("candidateName", result.getClerkUserId() != null ? result.getClerkUserId() : "Anonymous");
        vars.put("templateName", "Assessment");
        vars.put("completedAt", result.getCompletedAt() != null
                ? result.getCompletedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : "N/A");
        vars.put("passed", result.getOverallPercentage() != null && result.getOverallPercentage() >= 70.0);

        String svgCircle = ScoreCircleSvg.render(
                result.getOverallPercentage() != null ? result.getOverallPercentage() : 0.0,
                100.0, 120
        );
        vars.put("scoreCircleSvg", svgCircle);

        List<String> strengths = new ArrayList<>();
        List<String> developmentAreas = new ArrayList<>();
        if (result.getCompetencyScores() != null) {
            result.getCompetencyScores().forEach(cs -> {
                String name = cs.getCompetencyName() != null ? cs.getCompetencyName() : "Unknown";
                if (cs.getPercentage() != null && cs.getPercentage() >= 70.0) {
                    strengths.add(name + " (" + Math.round(cs.getPercentage()) + "%)");
                } else {
                    developmentAreas.add(name + " (" + (cs.getPercentage() != null
                            ? Math.round(cs.getPercentage()) : 0) + "%)");
                }
            });
        }
        vars.put("strengths", strengths);
        vars.put("developmentAreas", developmentAreas);
        vars.put("recommendation", "");
        vars.put("generatedAt", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));

        return vars;
    }
}
