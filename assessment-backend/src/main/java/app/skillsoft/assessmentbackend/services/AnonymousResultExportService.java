package app.skillsoft.assessmentbackend.services;

import app.skillsoft.assessmentbackend.domain.entities.AnonymousTakerInfo;
import app.skillsoft.assessmentbackend.domain.entities.TestResult;
import app.skillsoft.assessmentbackend.exception.ResourceNotFoundException;
import app.skillsoft.assessmentbackend.repository.TestResultRepository;
import app.skillsoft.assessmentbackend.repository.TestTemplateRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Service for exporting anonymous results as CSV.
 * Uses streaming output to avoid loading all results into memory.
 */
@Service
public class AnonymousResultExportService {

    private static final String[] CSV_HEADERS = {
            "Name", "Email", "Score (%)", "Passed", "Completed At",
            "Share Link", "Time (seconds)", "Questions Answered", "Questions Skipped"
    };

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int BATCH_SIZE = 200;

    private final TestResultRepository resultRepository;
    private final TestTemplateRepository templateRepository;

    public AnonymousResultExportService(
            TestResultRepository resultRepository,
            TestTemplateRepository templateRepository) {
        this.resultRepository = resultRepository;
        this.templateRepository = templateRepository;
    }

    /**
     * Stream anonymous results for a template as CSV to the given output stream.
     * Fetches results in batches to avoid OOM on large datasets.
     *
     * @param templateId The template to export results for
     * @param outputStream The stream to write CSV data to
     * @throws IOException if writing fails
     */
    @Transactional(readOnly = true)
    public void exportCsv(UUID templateId, OutputStream outputStream) throws IOException {
        if (!templateRepository.existsById(templateId)) {
            throw new ResourceNotFoundException("Template", templateId);
        }

        Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
        // Write UTF-8 BOM for Excel compatibility
        writer.write('\uFEFF');

        try (CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder()
                .setHeader(CSV_HEADERS)
                .build())) {

            int page = 0;
            Page<TestResult> batch;
            Sort sort = Sort.by(Sort.Direction.DESC, "completedAt");

            do {
                batch = resultRepository.findAnonymousByTemplateId(
                        templateId, PageRequest.of(page, BATCH_SIZE, sort));

                for (TestResult result : batch.getContent()) {
                    writeResultRow(printer, result);
                }

                printer.flush();
                page++;
            } while (batch.hasNext());
        }
    }

    private void writeResultRow(CSVPrinter printer, TestResult result) throws IOException {
        AnonymousTakerInfo takerInfo = result.getAnonymousTakerInfo();
        String name = takerInfo != null ? takerInfo.getDisplayName() : "Anonymous";
        String email = takerInfo != null ? takerInfo.getEmail() : "";

        String shareLinkLabel = "";
        if (result.getSession() != null && result.getSession().getShareLink() != null) {
            shareLinkLabel = result.getSession().getShareLink().getLabel();
            if (shareLinkLabel == null) shareLinkLabel = "";
        }

        String completedAt = result.getCompletedAt() != null
                ? result.getCompletedAt().format(DATE_FMT)
                : "";

        printer.printRecord(
                name,
                email,
                result.getOverallPercentage() != null ? String.format("%.1f", result.getOverallPercentage()) : "",
                result.getPassed() != null ? (result.getPassed() ? "Yes" : "No") : "",
                completedAt,
                shareLinkLabel,
                result.getTotalTimeSeconds() != null ? result.getTotalTimeSeconds() : "",
                result.getQuestionsAnswered() != null ? result.getQuestionsAnswered() : "",
                result.getQuestionsSkipped() != null ? result.getQuestionsSkipped() : ""
        );
    }
}
