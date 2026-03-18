package app.skillsoft.assessmentbackend.services.export;

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

            try {
                ClassPathResource fontResource = new ClassPathResource("fonts/Inter-Regular.ttf");
                if (fontResource.exists()) {
                    renderer.getFontResolver().addFont(
                        fontResource.getURL().toString(), true
                    );
                }
            } catch (Exception e) {
                logger.warn("Could not load Inter font, falling back to defaults: {}", e.getMessage());
            }

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
}
