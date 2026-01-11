package app.skillsoft.assessmentbackend.services;

import app.skillsoft.assessmentbackend.domain.dto.blueprint.JobFitBlueprint;
import app.skillsoft.assessmentbackend.domain.dto.blueprint.OverviewBlueprint;
import app.skillsoft.assessmentbackend.domain.dto.blueprint.TeamFitBlueprint;
import app.skillsoft.assessmentbackend.domain.dto.blueprint.TestBlueprintDto;
import app.skillsoft.assessmentbackend.domain.entities.AssessmentGoal;
import app.skillsoft.assessmentbackend.domain.entities.DifficultyLevel;
import app.skillsoft.assessmentbackend.domain.entities.TestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for converting legacy blueprint configurations to typed blueprint DTOs.
 *
 * <p>Handles the transition from the deprecated {@code Map<String, Object>} blueprint
 * format to the type-safe polymorphic {@link TestBlueprintDto} hierarchy.</p>
 *
 * <h3>Conversion Strategy:</h3>
 * <ul>
 *   <li>{@link AssessmentGoal#OVERVIEW} → {@link OverviewBlueprint}</li>
 *   <li>{@link AssessmentGoal#JOB_FIT} → {@link JobFitBlueprint}</li>
 *   <li>{@link AssessmentGoal#TEAM_FIT} → {@link TeamFitBlueprint}</li>
 * </ul>
 *
 * <p>This service is used during template creation/update to automatically migrate
 * legacy data and ensure templates have valid typed blueprints for test assembly.</p>
 */
@Service
public class BlueprintConversionService {

    private static final Logger log = LoggerFactory.getLogger(BlueprintConversionService.class);

    /**
     * Converts legacy blueprint data to typed blueprint based on the template's goal.
     *
     * <p>The conversion logic prioritizes data from different sources:</p>
     * <ol>
     *   <li>If typedBlueprint already exists, returns null (no conversion needed)</li>
     *   <li>Attempts to extract data from the legacy blueprint map</li>
     *   <li>Falls back to legacy competencyIds field</li>
     *   <li>Returns null if no valid source data exists</li>
     * </ol>
     *
     * @param template The template to convert
     * @return A typed blueprint DTO, or null if conversion is not possible
     */
    public TestBlueprintDto convertLegacyBlueprint(TestTemplate template) {
        if (template == null) {
            return null;
        }

        // Skip if already has typed blueprint
        if (template.getTypedBlueprint() != null) {
            log.debug("Template {} already has typedBlueprint, skipping conversion", template.getId());
            return template.getTypedBlueprint();
        }

        AssessmentGoal goal = template.getGoal();
        if (goal == null) {
            goal = AssessmentGoal.OVERVIEW; // Default to OVERVIEW
        }

        Map<String, Object> legacyBlueprint = template.getBlueprint();
        List<UUID> legacyCompetencyIds = template.getCompetencyIds();

        log.info("Converting legacy blueprint for template {} with goal {}",
                template.getId(), goal);

        return switch (goal) {
            case OVERVIEW -> convertToOverviewBlueprint(legacyBlueprint, legacyCompetencyIds, template);
            case JOB_FIT -> convertToJobFitBlueprint(legacyBlueprint, template);
            case TEAM_FIT -> convertToTeamFitBlueprint(legacyBlueprint, template);
        };
    }

    /**
     * Converts to OverviewBlueprint from legacy data.
     *
     * <p>Extracts competency IDs from:</p>
     * <ol>
     *   <li>blueprint.competencyIds (if present)</li>
     *   <li>template.competencyIds (legacy field)</li>
     * </ol>
     */
    private OverviewBlueprint convertToOverviewBlueprint(
            Map<String, Object> legacyBlueprint,
            List<UUID> legacyCompetencyIds,
            TestTemplate template) {

        List<UUID> competencyIds = new ArrayList<>();
        boolean includeBigFive = true;
        int questionsPerIndicator = template.getQuestionsPerIndicator() != null
                ? template.getQuestionsPerIndicator()
                : 3;

        // Try to extract competency IDs from legacy blueprint
        if (legacyBlueprint != null && !legacyBlueprint.isEmpty()) {
            Object compIds = legacyBlueprint.get("competencyIds");
            if (compIds instanceof List<?> list) {
                for (Object item : list) {
                    UUID uuid = parseUuid(item);
                    if (uuid != null) {
                        competencyIds.add(uuid);
                    }
                }
            }

            // Extract includeBigFive if present
            Object bigFive = legacyBlueprint.get("includeBigFive");
            if (bigFive instanceof Boolean b) {
                includeBigFive = b;
            }

            // Extract questionsPerIndicator if present
            Object qpi = legacyBlueprint.get("questionsPerIndicator");
            if (qpi instanceof Number n) {
                questionsPerIndicator = n.intValue();
            }
        }

        // Fall back to legacy competencyIds if no IDs found in blueprint
        if (competencyIds.isEmpty() && legacyCompetencyIds != null && !legacyCompetencyIds.isEmpty()) {
            competencyIds.addAll(legacyCompetencyIds);
        }

        // Cannot create valid blueprint without competencies
        if (competencyIds.isEmpty()) {
            log.warn("Cannot convert to OverviewBlueprint: no competency IDs found for template {}",
                    template.getId());
            return null;
        }

        OverviewBlueprint blueprint = new OverviewBlueprint();
        blueprint.setCompetencyIds(competencyIds);
        blueprint.setIncludeBigFive(includeBigFive);
        blueprint.setQuestionsPerIndicator(questionsPerIndicator);
        blueprint.setPreferredDifficulty(DifficultyLevel.INTERMEDIATE);
        blueprint.setShuffleQuestions(Boolean.TRUE.equals(template.getShuffleQuestions()));

        log.info("Created OverviewBlueprint with {} competencies for template {}",
                competencyIds.size(), template.getId());

        return blueprint;
    }

    /**
     * Converts to JobFitBlueprint from legacy data.
     *
     * <p>Requires O*NET SOC code from blueprint.onetSocCode or blueprint.onet_soc_code</p>
     */
    private JobFitBlueprint convertToJobFitBlueprint(
            Map<String, Object> legacyBlueprint,
            TestTemplate template) {

        if (legacyBlueprint == null || legacyBlueprint.isEmpty()) {
            log.warn("Cannot convert to JobFitBlueprint: no legacy blueprint for template {}",
                    template.getId());
            return null;
        }

        // Try both camelCase and snake_case keys
        String onetSocCode = extractString(legacyBlueprint, "onetSocCode", "onet_soc_code");

        if (onetSocCode == null || onetSocCode.isBlank()) {
            log.warn("Cannot convert to JobFitBlueprint: no O*NET SOC code for template {}",
                    template.getId());
            return null;
        }

        int strictnessLevel = 50; // Default
        Object strictness = legacyBlueprint.get("strictnessLevel");
        if (strictness == null) {
            strictness = legacyBlueprint.get("strictness_level");
        }
        if (strictness instanceof Number n) {
            strictnessLevel = n.intValue();
        }

        JobFitBlueprint blueprint = new JobFitBlueprint();
        blueprint.setOnetSocCode(onetSocCode);
        blueprint.setStrictnessLevel(strictnessLevel);

        log.info("Created JobFitBlueprint with SOC code {} for template {}",
                onetSocCode, template.getId());

        return blueprint;
    }

    /**
     * Converts to TeamFitBlueprint from legacy data.
     *
     * <p>Requires team ID from blueprint.teamId or blueprint.team_id</p>
     */
    private TeamFitBlueprint convertToTeamFitBlueprint(
            Map<String, Object> legacyBlueprint,
            TestTemplate template) {

        if (legacyBlueprint == null || legacyBlueprint.isEmpty()) {
            log.warn("Cannot convert to TeamFitBlueprint: no legacy blueprint for template {}",
                    template.getId());
            return null;
        }

        // Try both camelCase and snake_case keys
        String teamIdStr = extractString(legacyBlueprint, "teamId", "team_id");
        UUID teamId = parseUuid(teamIdStr);

        if (teamId == null) {
            log.warn("Cannot convert to TeamFitBlueprint: no team ID for template {}",
                    template.getId());
            return null;
        }

        double saturationThreshold = 0.75; // Default
        Object threshold = legacyBlueprint.get("saturationThreshold");
        if (threshold == null) {
            threshold = legacyBlueprint.get("saturation_threshold");
        }
        if (threshold instanceof Number n) {
            saturationThreshold = n.doubleValue();
        }

        TeamFitBlueprint blueprint = new TeamFitBlueprint();
        blueprint.setTeamId(teamId);
        blueprint.setSaturationThreshold(saturationThreshold);

        log.info("Created TeamFitBlueprint with team ID {} for template {}",
                teamId, template.getId());

        return blueprint;
    }

    /**
     * Extracts a string value from a map, trying multiple keys.
     */
    private String extractString(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (value instanceof String s && !s.isBlank()) {
                return s;
            }
        }
        return null;
    }

    /**
     * Parses a UUID from various object types.
     */
    private UUID parseUuid(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof UUID uuid) {
            return uuid;
        }
        if (value instanceof String s && !s.isBlank()) {
            try {
                return UUID.fromString(s);
            } catch (IllegalArgumentException e) {
                log.debug("Failed to parse UUID from string: {}", s);
                return null;
            }
        }
        return null;
    }

    /**
     * Checks if a template has a valid blueprint for test assembly.
     *
     * @param template The template to check
     * @return true if the template has a valid typedBlueprint
     */
    public boolean hasValidBlueprint(TestTemplate template) {
        if (template == null) {
            return false;
        }
        return template.getTypedBlueprint() != null;
    }

    /**
     * Validates and potentially upgrades a template's blueprint.
     *
     * <p>This method attempts to ensure the template has a valid typed blueprint
     * by converting legacy data if necessary.</p>
     *
     * @param template The template to validate/upgrade
     * @return true if the template now has a valid typed blueprint
     */
    public boolean ensureTypedBlueprint(TestTemplate template) {
        if (template == null) {
            return false;
        }

        if (template.getTypedBlueprint() != null) {
            return true; // Already has valid blueprint
        }

        TestBlueprintDto converted = convertLegacyBlueprint(template);
        if (converted != null) {
            template.setTypedBlueprint(converted);
            log.info("Successfully upgraded template {} to typed blueprint", template.getId());
            return true;
        }

        return false;
    }
}
