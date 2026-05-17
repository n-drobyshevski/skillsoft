package app.skillsoft.assessmentbackend.services.export.svg;

public final class ScoreCircleSvg {

    private static final int STROKE_WIDTH = 8;
    private static final String TRACK_COLOR = "#e5e7eb";
    private static final String PROGRESS_COLOR = "#3b82f6";
    private static final String TEXT_COLOR = "#1f2937";

    private ScoreCircleSvg() {}

    public static String render(double value, double maxValue, int size) {
        double center = size / 2.0;
        double radius = center - STROKE_WIDTH / 2.0 - 4.0;
        double circumference = 2 * Math.PI * radius;
        double normalized = Math.min(Math.max((value / maxValue) * 100.0, 0.0), 100.0);
        double dashoffset = circumference * (1.0 - normalized / 100.0);
        int displayPercent = (int) Math.round(normalized);

        return String.format("""
            <svg xmlns="http://www.w3.org/2000/svg" width="%d" height="%d" viewBox="0 0 %d %d">
              <circle cx="%.1f" cy="%.1f" r="%.1f" fill="none" stroke="%s" stroke-width="%d"/>
              <circle cx="%.1f" cy="%.1f" r="%.1f" fill="none" stroke="%s" stroke-width="%d" \
            stroke-linecap="round" stroke-dasharray="%.2f" stroke-dashoffset="%.2f" \
            transform="rotate(-90 %.1f %.1f)"/>
              <text x="%.1f" y="%.1f" text-anchor="middle" dominant-baseline="central" \
            font-family="Inter, sans-serif" font-size="%d" font-weight="600" fill="%s">%d%%</text>
            </svg>""",
            size, size, size, size,
            center, center, radius, TRACK_COLOR, STROKE_WIDTH,
            center, center, radius, PROGRESS_COLOR, STROKE_WIDTH,
            circumference, dashoffset, center, center,
            center, center, size / 4, TEXT_COLOR, displayPercent
        );
    }
}
