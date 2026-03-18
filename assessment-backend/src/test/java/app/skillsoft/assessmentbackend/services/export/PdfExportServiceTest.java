package app.skillsoft.assessmentbackend.services.export;

import app.skillsoft.assessmentbackend.domain.dto.export.PdfExportCreatedResponse;
import app.skillsoft.assessmentbackend.domain.dto.export.PdfExportRequest;
import app.skillsoft.assessmentbackend.domain.dto.export.PdfExportStatusResponse;
import app.skillsoft.assessmentbackend.domain.entities.ExportFormat;
import app.skillsoft.assessmentbackend.domain.entities.ExportStatus;
import app.skillsoft.assessmentbackend.domain.entities.PdfExportJob;
import app.skillsoft.assessmentbackend.domain.entities.ResultStatus;
import app.skillsoft.assessmentbackend.domain.entities.TestResult;
import app.skillsoft.assessmentbackend.repository.PdfExportJobRepository;
import app.skillsoft.assessmentbackend.repository.TestResultRepository;
import app.skillsoft.assessmentbackend.services.export.impl.PdfExportServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PdfExportService")
class PdfExportServiceTest {

    @Mock
    private PdfExportJobRepository jobRepository;

    @Mock
    private TestResultRepository resultRepository;

    @Mock
    private PdfGenerationWorker generationWorker;

    @InjectMocks
    private PdfExportServiceImpl exportService;

    @Nested
    @DisplayName("requestExport")
    class RequestExport {

        @Test
        @DisplayName("should create new job and return QUEUED status")
        void shouldCreateJobAndReturnQueued() {
            UUID resultId = UUID.randomUUID();
            var request = new PdfExportRequest(resultId, ExportFormat.MANAGER_SUMMARY, "en", null);

            TestResult mockResult = new TestResult();
            mockResult.setClerkUserId("user_123");
            mockResult.setStatus(ResultStatus.COMPLETED);

            when(resultRepository.findById(resultId)).thenReturn(Optional.of(mockResult));
            when(jobRepository.findFirstByContentHashAndStatusOrderByCreatedAtDesc(anyString(), eq(ExportStatus.COMPLETED)))
                    .thenReturn(Optional.empty());
            when(jobRepository.findByClerkUserIdAndStatusIn(eq("user_123"), anyList()))
                    .thenReturn(List.of());
            when(jobRepository.save(any(PdfExportJob.class))).thenAnswer(inv -> {
                PdfExportJob job = inv.getArgument(0);
                if (job.getId() == null) {
                    job.setId(UUID.randomUUID());
                }
                return job;
            });

            PdfExportCreatedResponse response = exportService.requestExport(request, "user_123");

            assertThat(response.status()).isEqualTo(ExportStatus.QUEUED);
            assertThat(response.exportId()).isNotNull();
            verify(jobRepository).save(any(PdfExportJob.class));
        }

        @Test
        @DisplayName("should reject when result not found")
        void shouldRejectWhenResultNotFound() {
            UUID resultId = UUID.randomUUID();
            var request = new PdfExportRequest(resultId, ExportFormat.MANAGER_SUMMARY, "en", null);

            when(resultRepository.findById(resultId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> exportService.requestExport(request, "user_123"))
                    .hasMessageContaining("not found");
        }

        @Test
        @DisplayName("should reject anonymous results")
        void shouldRejectAnonymousResults() {
            UUID resultId = UUID.randomUUID();
            var request = new PdfExportRequest(resultId, ExportFormat.MANAGER_SUMMARY, "en", null);

            TestResult anonymousResult = new TestResult();
            anonymousResult.setClerkUserId(null);

            when(resultRepository.findById(resultId)).thenReturn(Optional.of(anonymousResult));

            assertThatThrownBy(() -> exportService.requestExport(request, "user_123"))
                    .hasMessageContaining("anonymous");
        }

        @Test
        @DisplayName("should reject when rate limit exceeded")
        void shouldRejectWhenRateLimitExceeded() {
            UUID resultId = UUID.randomUUID();
            var request = new PdfExportRequest(resultId, ExportFormat.MANAGER_SUMMARY, "en", null);

            TestResult mockResult = new TestResult();
            mockResult.setClerkUserId("user_123");
            mockResult.setStatus(ResultStatus.COMPLETED);

            when(resultRepository.findById(resultId)).thenReturn(Optional.of(mockResult));
            when(jobRepository.findFirstByContentHashAndStatusOrderByCreatedAtDesc(anyString(), eq(ExportStatus.COMPLETED)))
                    .thenReturn(Optional.empty());
            List<PdfExportJob> activeJobs = List.of(
                    new PdfExportJob(), new PdfExportJob(), new PdfExportJob(),
                    new PdfExportJob(), new PdfExportJob()
            );
            when(jobRepository.findByClerkUserIdAndStatusIn(eq("user_123"), anyList()))
                    .thenReturn(activeJobs);

            assertThatThrownBy(() -> exportService.requestExport(request, "user_123"))
                    .hasMessageContaining("Rate limit");
        }

        @Test
        @DisplayName("should return cached job when COMPLETED with existing file")
        void shouldReturnCachedJob() throws Exception {
            UUID resultId = UUID.randomUUID();
            UUID cachedJobId = UUID.randomUUID();
            var request = new PdfExportRequest(resultId, ExportFormat.MANAGER_SUMMARY, "en", null);

            TestResult mockResult = new TestResult();
            mockResult.setClerkUserId("user_123");
            mockResult.setStatus(ResultStatus.COMPLETED);

            // Create a temp file so Files.exists returns true
            java.nio.file.Path tmpFile = java.nio.file.Files.createTempFile("pdf-test-", ".pdf");
            tmpFile.toFile().deleteOnExit();

            PdfExportJob cachedJob = new PdfExportJob();
            cachedJob.setId(cachedJobId);
            cachedJob.setStatus(ExportStatus.COMPLETED);
            cachedJob.setFilePath(tmpFile.toString());

            when(resultRepository.findById(resultId)).thenReturn(Optional.of(mockResult));
            when(jobRepository.findFirstByContentHashAndStatusOrderByCreatedAtDesc(anyString(), eq(ExportStatus.COMPLETED)))
                    .thenReturn(Optional.of(cachedJob));

            PdfExportCreatedResponse response = exportService.requestExport(request, "user_123");

            assertThat(response.exportId()).isEqualTo(cachedJobId);
            assertThat(response.status()).isEqualTo(ExportStatus.COMPLETED);
        }
    }

    @Nested
    @DisplayName("getStatus")
    class GetStatus {

        @Test
        @DisplayName("should return status for existing job")
        void shouldReturnStatus() {
            UUID exportId = UUID.randomUUID();
            PdfExportJob job = new PdfExportJob();
            job.setId(exportId);
            job.setStatus(ExportStatus.GENERATING);

            when(jobRepository.findById(exportId)).thenReturn(Optional.of(job));

            PdfExportStatusResponse response = exportService.getStatus(exportId);

            assertThat(response.exportId()).isEqualTo(exportId);
            assertThat(response.status()).isEqualTo(ExportStatus.GENERATING);
        }

        @Test
        @DisplayName("should throw when export not found")
        void shouldThrowWhenNotFound() {
            UUID exportId = UUID.randomUUID();
            when(jobRepository.findById(exportId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> exportService.getStatus(exportId))
                    .hasMessageContaining("not found");
        }
    }

    @Nested
    @DisplayName("download")
    class Download {

        @Test
        @DisplayName("should throw when export not completed")
        void shouldThrowWhenNotCompleted() {
            UUID exportId = UUID.randomUUID();
            PdfExportJob job = new PdfExportJob();
            job.setId(exportId);
            job.setStatus(ExportStatus.GENERATING);

            when(jobRepository.findById(exportId)).thenReturn(Optional.of(job));

            assertThatThrownBy(() -> exportService.download(exportId))
                    .hasMessageContaining("not ready");
        }

        @Test
        @DisplayName("should return bytes for completed export")
        void shouldReturnBytesForCompleted() throws Exception {
            UUID exportId = UUID.randomUUID();
            byte[] content = "fake-pdf-content".getBytes();
            java.nio.file.Path tmpFile = java.nio.file.Files.createTempFile("pdf-dl-", ".pdf");
            java.nio.file.Files.write(tmpFile, content);
            tmpFile.toFile().deleteOnExit();

            PdfExportJob job = new PdfExportJob();
            job.setId(exportId);
            job.setStatus(ExportStatus.COMPLETED);
            job.setFilePath(tmpFile.toString());

            when(jobRepository.findById(exportId)).thenReturn(Optional.of(job));

            byte[] result = exportService.download(exportId);

            assertThat(result).isEqualTo(content);
        }
    }
}
