package app.skillsoft.assessmentbackend.services.export;

import app.skillsoft.assessmentbackend.domain.entities.ExportStatus;
import app.skillsoft.assessmentbackend.domain.entities.PdfExportJob;
import app.skillsoft.assessmentbackend.repository.PdfExportJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class PdfExportCleanupScheduler {

    private static final Logger logger = LoggerFactory.getLogger(PdfExportCleanupScheduler.class);
    private final PdfExportJobRepository jobRepository;

    public PdfExportCleanupScheduler(PdfExportJobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    @Scheduled(fixedRate = 3600000)
    @Transactional
    public void cleanupExpiredExports() {
        LocalDateTime now = LocalDateTime.now();

        List<PdfExportJob> expired = jobRepository.findByStatusAndExpiresAtBefore(ExportStatus.COMPLETED, now);
        for (PdfExportJob job : expired) {
            deleteFile(job);
            job.setStatus(ExportStatus.EXPIRED);
            jobRepository.save(job);
            logger.info("Expired PDF export job: {}", job.getId());
        }

        LocalDateTime stuckCutoff = now.minusMinutes(5);
        List<PdfExportJob> stuck = jobRepository.findStuckGeneratingJobs(stuckCutoff);
        for (PdfExportJob job : stuck) {
            deleteFile(job);
            job.setStatus(ExportStatus.FAILED);
            job.setFailedAt(now);
            job.setErrorMessage("Generation timed out after 5 minutes");
            jobRepository.save(job);
            logger.warn("Failed stuck PDF export job: {}", job.getId());
        }

        if (!expired.isEmpty() || !stuck.isEmpty()) {
            logger.info("Cleanup complete: expired={}, stuck={}", expired.size(), stuck.size());
        }
    }

    private void deleteFile(PdfExportJob job) {
        if (job.getFilePath() != null) {
            try {
                Files.deleteIfExists(Path.of(job.getFilePath()));
            } catch (IOException e) {
                logger.warn("Could not delete PDF file {}: {}", job.getFilePath(), e.getMessage());
            }
        }
    }
}
