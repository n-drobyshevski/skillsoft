package app.skillsoft.assessmentbackend.services.export;

import com.lowagie.text.pdf.BaseFont;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.util.Locale;
import java.util.Map;

@Component
public class PdfTemplateEngine {

    private static final Logger logger = LoggerFactory.getLogger(PdfTemplateEngine.class);
    private final SpringTemplateEngine templateEngine;

    public PdfTemplateEngine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/pdf/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.XML);
        resolver.setCharacterEncoding("UTF-8");

        this.templateEngine = new SpringTemplateEngine();
        this.templateEngine.setTemplateResolver(resolver);
    }

    public byte[] render(String templateName, Map<String, Object> variables, Locale locale) {
        Context context = new Context(locale);
        context.setVariables(variables);

        String html = templateEngine.process(templateName, context);
        logger.debug("Rendered HTML template '{}' ({} chars)", templateName, html.length());

        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();

            // Load Noto Sans with IDENTITY_H encoding for full Unicode/Cyrillic support
            loadFont(renderer, "fonts/NotoSans-Regular.ttf");
            loadFont(renderer, "fonts/NotoSans-Bold.ttf");

            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.createPDF(os);

            byte[] pdf = os.toByteArray();
            logger.info("Generated PDF '{}': {} bytes", templateName, pdf.length);
            return pdf;
        } catch (Exception e) {
            logger.error("Failed to render PDF template '{}': {}", templateName, e.getMessage(), e);
            throw new RuntimeException("PDF generation failed: " + e.getMessage(), e);
        }
    }

    private void loadFont(ITextRenderer renderer, String classpathPath) {
        try {
            ClassPathResource res = new ClassPathResource(classpathPath);
            if (res.exists()) {
                renderer.getFontResolver().addFont(
                    res.getURL().toString(),
                    BaseFont.IDENTITY_H,  // Unicode horizontal — required for Cyrillic
                    true                   // embedded in PDF
                );
                logger.debug("Loaded font: {}", classpathPath);
            } else {
                logger.warn("Font not found on classpath: {}", classpathPath);
            }
        } catch (Exception e) {
            logger.warn("Could not load font {}: {}", classpathPath, e.getMessage());
        }
    }
}
