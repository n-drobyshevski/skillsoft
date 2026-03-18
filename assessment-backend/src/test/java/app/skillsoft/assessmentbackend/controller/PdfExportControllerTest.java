package app.skillsoft.assessmentbackend.controller;

import app.skillsoft.assessmentbackend.domain.dto.export.PdfExportCreatedResponse;
import app.skillsoft.assessmentbackend.domain.dto.export.PdfExportRequest;
import app.skillsoft.assessmentbackend.domain.dto.export.PdfExportStatusResponse;
import app.skillsoft.assessmentbackend.domain.entities.ExportFormat;
import app.skillsoft.assessmentbackend.domain.entities.ExportStatus;
import app.skillsoft.assessmentbackend.security.SessionSecurityService;
import app.skillsoft.assessmentbackend.services.export.PdfExportService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for PdfExportController using @WebMvcTest.
 *
 * Tests cover:
 * - POST /api/v1/exports/pdf returns 202 Accepted
 * - GET /api/v1/exports/{id}/status returns 200 with status payload
 * - GET /api/v1/exports/{id}/download returns 200 with PDF bytes
 */
@WebMvcTest(PdfExportController.class)
@DisplayName("PdfExport Controller Tests")
class PdfExportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PdfExportService exportService;

    @MockBean
    private SessionSecurityService sessionSecurity;

    private UUID exportId;
    private UUID resultId;
    private String clerkUserId;
    private PdfExportCreatedResponse createdResponse;
    private PdfExportStatusResponse statusResponse;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        exportId = UUID.randomUUID();
        resultId = UUID.randomUUID();
        clerkUserId = "user_test123";
        now = LocalDateTime.now();

        createdResponse = new PdfExportCreatedResponse(exportId, ExportStatus.QUEUED);

        statusResponse = new PdfExportStatusResponse(
                exportId,
                ExportStatus.COMPLETED,
                204800L,
                now
        );

        when(sessionSecurity.getAuthenticatedUserId()).thenReturn(clerkUserId);
    }

    @Nested
    @DisplayName("POST /api/v1/exports/pdf - Create Export Tests")
    class CreateExportTests {

        @Test
        @WithMockUser
        @DisplayName("Should return 202 Accepted with exportId and QUEUED status")
        void shouldReturn202WithCreatedResponse() throws Exception {
            // Given
            PdfExportRequest request = new PdfExportRequest(
                    resultId, ExportFormat.FULL_REPORT, "en", List.of("summary", "competencies"));

            when(exportService.requestExport(any(PdfExportRequest.class), eq(clerkUserId)))
                    .thenReturn(createdResponse);

            // When & Then
            mockMvc.perform(post("/api/v1/exports/pdf")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isAccepted())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.exportId").value(exportId.toString()))
                    .andExpect(jsonPath("$.status").value("QUEUED"));

            verify(exportService).requestExport(any(PdfExportRequest.class), eq(clerkUserId));
            verify(sessionSecurity).getAuthenticatedUserId();
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 400 when resultId is missing")
        void shouldReturn400WhenResultIdMissing() throws Exception {
            // Given - request body without resultId
            String invalidBody = "{\"format\":\"FULL_REPORT\"}";

            // When & Then
            mockMvc.perform(post("/api/v1/exports/pdf")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidBody))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 400 when format is missing")
        void shouldReturn400WhenFormatMissing() throws Exception {
            // Given - request body without format
            String invalidBody = "{\"resultId\":\"" + resultId + "\"}";

            // When & Then
            mockMvc.perform(post("/api/v1/exports/pdf")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidBody))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 401 when not authenticated")
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            // Given
            PdfExportRequest request = new PdfExportRequest(
                    resultId, ExportFormat.FULL_REPORT, "en", null);

            // When & Then
            mockMvc.perform(post("/api/v1/exports/pdf")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/exports/{exportId}/status - Get Status Tests")
    class GetStatusTests {

        @Test
        @WithMockUser
        @DisplayName("Should return 200 with status payload")
        void shouldReturn200WithStatusResponse() throws Exception {
            // Given
            when(exportService.getStatus(exportId)).thenReturn(statusResponse);

            // When & Then
            mockMvc.perform(get("/api/v1/exports/{exportId}/status", exportId))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.exportId").value(exportId.toString()))
                    .andExpect(jsonPath("$.status").value("COMPLETED"))
                    .andExpect(jsonPath("$.fileSizeBytes").value(204800));

            verify(exportService).getStatus(exportId);
        }

        @Test
        @DisplayName("Should return 401 when not authenticated")
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/v1/exports/{exportId}/status", exportId))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/exports/{exportId}/download - Download Tests")
    class DownloadTests {

        @Test
        @WithMockUser
        @DisplayName("Should return 200 with PDF bytes and correct headers")
        void shouldReturn200WithPdfBytes() throws Exception {
            // Given
            byte[] pdfBytes = "%PDF-1.4 test content".getBytes();
            when(exportService.download(exportId)).thenReturn(pdfBytes);

            // When & Then
            mockMvc.perform(get("/api/v1/exports/{exportId}/download", exportId))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                    .andExpect(header().string("Content-Disposition",
                            "attachment; filename=\"export-" + exportId + ".pdf\""))
                    .andExpect(content().bytes(pdfBytes));

            verify(exportService).download(exportId);
        }

        @Test
        @DisplayName("Should return 401 when not authenticated")
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/v1/exports/{exportId}/download", exportId))
                    .andExpect(status().isUnauthorized());
        }
    }
}
