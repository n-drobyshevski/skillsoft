package app.skillsoft.assessmentbackend.services.export;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PDF Template Engine")
class PdfTemplateEngineTest {

    @TempDir
    Path tempDir;

    private Map<String, Object> sampleVariables() {
        return Map.ofEntries(
            Map.entry("title", "Manager Summary"),
            Map.entry("candidateName", "Jane Doe"),
            Map.entry("templateName", "Software Developer Assessment"),
            Map.entry("completedAt", "2026-03-18"),
            Map.entry("passed", true),
            Map.entry("scoreCircleSvg", "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"120\" height=\"120\"><circle cx=\"60\" cy=\"60\" r=\"50\" fill=\"none\" stroke=\"#ccc\" stroke-width=\"8\"/></svg>"),
            Map.entry("strengths", List.of("Problem Solving", "Communication")),
            Map.entry("developmentAreas", List.of("Time Management")),
            Map.entry("recommendation", "Recommend for hire"),
            Map.entry("generatedAt", "2026-03-18 10:30")
        );
    }

    @Test
    @DisplayName("Should render manager-summary template to valid PDF bytes")
    void shouldRenderManagerSummaryToPdf() {
        PdfTemplateEngine engine = new PdfTemplateEngine();
        byte[] pdfBytes = engine.render("manager-summary", sampleVariables(), Locale.ENGLISH);

        assertThat(pdfBytes).isNotEmpty();
        assertThat(pdfBytes.length).isGreaterThan(100);
        // PDF magic bytes
        assertThat(new String(pdfBytes, 0, 4)).isEqualTo("%PDF");
    }

    @Test
    @DisplayName("Should write valid PDF file to disk")
    void shouldWriteValidPdfFile() throws Exception {
        PdfTemplateEngine engine = new PdfTemplateEngine();
        byte[] pdf = engine.render("manager-summary", sampleVariables(), Locale.ENGLISH);

        Path file = tempDir.resolve("test.pdf");
        Files.write(file, pdf);

        assertThat(file).exists();
        assertThat(Files.size(file)).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should handle empty strengths and development areas")
    void shouldHandleEmptyLists() {
        PdfTemplateEngine engine = new PdfTemplateEngine();
        Map<String, Object> vars = Map.ofEntries(
            Map.entry("title", "Test"),
            Map.entry("candidateName", "Test User"),
            Map.entry("templateName", "Test"),
            Map.entry("completedAt", "2026-03-18"),
            Map.entry("passed", false),
            Map.entry("scoreCircleSvg", "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"120\" height=\"120\"></svg>"),
            Map.entry("strengths", List.of()),
            Map.entry("developmentAreas", List.of()),
            Map.entry("recommendation", ""),
            Map.entry("generatedAt", "2026-03-18")
        );

        byte[] pdf = engine.render("manager-summary", vars, Locale.ENGLISH);
        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
    }
}
