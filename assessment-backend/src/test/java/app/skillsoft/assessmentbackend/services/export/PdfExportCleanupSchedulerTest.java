package app.skillsoft.assessmentbackend.services.export;

import app.skillsoft.assessmentbackend.domain.entities.ExportStatus;
import app.skillsoft.assessmentbackend.domain.entities.PdfExportJob;
import app.skillsoft.assessmentbackend.repository.PdfExportJobRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PDF Export Cleanup Scheduler")
class PdfExportCleanupSchedulerTest {

    @Mock private PdfExportJobRepository jobRepository;
    @InjectMocks private PdfExportCleanupScheduler scheduler;

    @Test
    @DisplayName("Should mark expired COMPLETED jobs as EXPIRED and delete files")
    void shouldCleanupExpiredJobs(@TempDir Path tempDir) throws Exception {
        Path fakePdf = tempDir.resolve("test.pdf");
        Files.write(fakePdf, "fake pdf".getBytes());

        PdfExportJob expiredJob = new PdfExportJob();
        expiredJob.setId(UUID.randomUUID());
        expiredJob.setStatus(ExportStatus.COMPLETED);
        expiredJob.setFilePath(fakePdf.toString());

        when(jobRepository.findByStatusAndExpiresAtBefore(eq(ExportStatus.COMPLETED), any()))
            .thenReturn(List.of(expiredJob));
        when(jobRepository.findStuckGeneratingJobs(any()))
            .thenReturn(List.of());

        scheduler.cleanupExpiredExports();

        verify(jobRepository).save(argThat(job -> job.getStatus() == ExportStatus.EXPIRED));
        assertThat(fakePdf).doesNotExist();
    }

    @Test
    @DisplayName("Should fail stuck GENERATING jobs older than 5 minutes")
    void shouldFailStuckJobs() {
        PdfExportJob stuckJob = new PdfExportJob();
        stuckJob.setId(UUID.randomUUID());
        stuckJob.setStatus(ExportStatus.GENERATING);

        when(jobRepository.findByStatusAndExpiresAtBefore(eq(ExportStatus.COMPLETED), any()))
            .thenReturn(List.of());
        when(jobRepository.findStuckGeneratingJobs(any()))
            .thenReturn(List.of(stuckJob));

        scheduler.cleanupExpiredExports();

        verify(jobRepository).save(argThat(job ->
            job.getStatus() == ExportStatus.FAILED &&
            job.getErrorMessage() != null &&
            job.getErrorMessage().contains("timed out")
        ));
    }

    @Test
    @DisplayName("Should handle no expired or stuck jobs gracefully")
    void shouldHandleNoJobs() {
        when(jobRepository.findByStatusAndExpiresAtBefore(eq(ExportStatus.COMPLETED), any()))
            .thenReturn(List.of());
        when(jobRepository.findStuckGeneratingJobs(any()))
            .thenReturn(List.of());

        scheduler.cleanupExpiredExports();

        verify(jobRepository, never()).save(any());
    }
}
