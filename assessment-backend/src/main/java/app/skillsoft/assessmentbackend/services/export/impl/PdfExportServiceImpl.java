package app.skillsoft.assessmentbackend.services.export.impl;

import app.skillsoft.assessmentbackend.domain.dto.export.PdfExportCreatedResponse;
import app.skillsoft.assessmentbackend.domain.dto.export.PdfExportRequest;
import app.skillsoft.assessmentbackend.domain.dto.export.PdfExportStatusResponse;
import app.skillsoft.assessmentbackend.domain.entities.ExportStatus;
import app.skillsoft.assessmentbackend.domain.entities.PdfExportJob;
import app.skillsoft.assessmentbackend.domain.entities.TestResult;
import app.skillsoft.assessmentbackend.repository.PdfExportJobRepository;
import app.skillsoft.assessmentbackend.repository.TestResultRepository;
import app.skillsoft.assessmentbackend.services.export.PdfExportService;
import app.skillsoft.assessmentbackend.services.export.PdfGenerationWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PdfExportServiceImpl implements PdfExportService {

    private static final Logger logger = LoggerFactory.getLogger(PdfExportServiceImpl.class);
    private static final int MAX_CONCURRENT_EXPORTS = 5;

    private final PdfExportJobRepository jobRepository;
    private final TestResultRepository resultRepository;
    private final PdfGenerationWorker generationWorker;

    public PdfExportServiceImpl(PdfExportJobRepository jobRepository,
                                TestResultRepository resultRepository,
                                PdfGenerationWorker generationWorker) {
        this.jobRepository = jobRepository;
        this.resultRepository = resultRepository;
        this.generationWorker = generationWorker;
    }

    @Override
    @Transactional
    public PdfExportCreatedResponse requestExport(PdfExportRequest request, String clerkUserId) {
        TestResult result = resultRepository.findById(request.resultId())
                .orElseThrow(() -> new RuntimeException("Result not found: " + request.resultId()));

        if (result.getClerkUserId() == null) {
            throw new RuntimeException("PDF export not available for anonymous results");
        }

        String contentHash = computeContentHash(request, result);

        Optional<PdfExportJob> cached = jobRepository.findByContentHashAndStatus(
                contentHash, ExportStatus.COMPLETED);
        if (cached.isPresent()) {
            PdfExportJob cachedJob = cached.get();
            if (cachedJob.getFilePath() != null && Files.exists(Path.of(cachedJob.getFilePath()))) {
                logger.info("Cache hit for export: {}", cachedJob.getId());
                return new PdfExportCreatedResponse(cachedJob.getId(), ExportStatus.COMPLETED);
            }
        }

        List<PdfExportJob> activeJobs = jobRepository.findByClerkUserIdAndStatusIn(
                clerkUserId, List.of(ExportStatus.QUEUED, ExportStatus.GENERATING));
        if (activeJobs.size() >= MAX_CONCURRENT_EXPORTS) {
            throw new RuntimeException(
                    "Rate limit exceeded: max " + MAX_CONCURRENT_EXPORTS + " concurrent exports");
        }

        PdfExportJob job = new PdfExportJob();
        job.setResultId(request.resultId());
        job.setClerkUserId(clerkUserId);
        job.setFormat(request.format());
        job.setLocale(request.locale());
        job.setSections(normalizeSections(request.sections()));
        job.setContentHash(contentHash);
        job.setStatus(ExportStatus.QUEUED);

        job = jobRepository.save(job);
        logger.info("Created PDF export job: {}", job.getId());

        final UUID jobId = job.getId();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    generationWorker.generateAsync(jobId);
                }
            });
        } else {
            generationWorker.generateAsync(jobId);
        }

        return new PdfExportCreatedResponse(job.getId(), ExportStatus.QUEUED);
    }

    @Override
    @Transactional(readOnly = true)
    public PdfExportStatusResponse getStatus(UUID exportId) {
        PdfExportJob job = jobRepository.findById(exportId)
                .orElseThrow(() -> new RuntimeException("Export not found: " + exportId));

        return new PdfExportStatusResponse(
                job.getId(), job.getStatus(), job.getFileSizeBytes(), job.getCreatedAt());
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] download(UUID exportId) {
        PdfExportJob job = jobRepository.findById(exportId)
                .orElseThrow(() -> new RuntimeException("Export not found: " + exportId));

        if (job.getStatus() != ExportStatus.COMPLETED) {
            throw new RuntimeException("Export not ready: status=" + job.getStatus());
        }

        if (job.getFilePath() == null || !Files.exists(Path.of(job.getFilePath()))) {
            throw new RuntimeException("Export file missing: " + job.getFilePath());
        }

        try {
            return Files.readAllBytes(Path.of(job.getFilePath()));
        } catch (IOException e) {
            throw new RuntimeException("Failed to read PDF file: " + e.getMessage(), e);
        }
    }

    private String computeContentHash(PdfExportRequest request, TestResult result) {
        String raw = request.resultId().toString()
                + "|" + request.format().name()
                + "|" + request.locale()
                + "|" + normalizeSections(request.sections())
                + "|" + (result.getCompletedAt() != null ? result.getCompletedAt().toString() : "");
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    private String normalizeSections(List<String> sections) {
        if (sections == null || sections.isEmpty()) {
            return "";
        }
        return sections.stream().sorted().collect(Collectors.joining(","));
    }
}
