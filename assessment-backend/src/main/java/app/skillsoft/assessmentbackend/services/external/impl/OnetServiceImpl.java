package app.skillsoft.assessmentbackend.services.external.impl;

import app.skillsoft.assessmentbackend.services.external.OnetService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Mock implementation of OnetService for development and testing.
 * 
 * In production, this would integrate with the actual O*NET Web Services API
 * or a locally cached O*NET database.
 */
@Service
@Slf4j
public class OnetServiceImpl implements OnetService {

    // Mock data for common occupations
    private static final Map<String, OnetProfile> MOCK_PROFILES = new HashMap<>();

    static {
        // Software Developer profile
        MOCK_PROFILES.put("15-1252.00", new OnetProfile(
            "15-1252.00",
            "Software Developers",
            "Research, design, and develop computer and network software or specialized utility programs.",
            Map.of(
                "Critical Thinking", 4.25,
                "Complex Problem Solving", 4.12,
                "Programming", 4.50,
                "Systems Analysis", 3.88,
                "Quality Control Analysis", 3.62
            ),
            Map.of(
                "Computers and Electronics", 4.50,
                "Engineering and Technology", 3.75,
                "Mathematics", 3.62
            ),
            Map.of(
                "Programming", 4.50,
                "Systems Analysis", 3.88,
                "Technology Design", 3.75
            ),
            Map.of(
                "Deductive Reasoning", 4.12,
                "Inductive Reasoning", 4.00,
                "Information Ordering", 3.88
            )
        ));

        // Project Manager profile
        MOCK_PROFILES.put("11-9199.00", new OnetProfile(
            "11-9199.00",
            "Managers, All Other",
            "Plan, direct, or coordinate operations of organizations.",
            Map.of(
                "Leadership", 4.25,
                "Critical Thinking", 4.00,
                "Coordination", 4.12,
                "Time Management", 4.00,
                "Decision Making", 4.25
            ),
            Map.of(
                "Administration and Management", 4.25,
                "Customer and Personal Service", 3.75
            ),
            Map.of(
                "Coordination", 4.12,
                "Judgment and Decision Making", 4.00,
                "Management of Personnel Resources", 3.88
            ),
            Map.of(
                "Oral Comprehension", 4.00,
                "Written Comprehension", 3.88,
                "Problem Sensitivity", 3.75
            )
        ));

        // Data Scientist profile
        MOCK_PROFILES.put("15-2051.00", new OnetProfile(
            "15-2051.00",
            "Data Scientists",
            "Develop and implement methods for collecting, processing, and analyzing data.",
            Map.of(
                "Analytical Thinking", 4.50,
                "Statistical Analysis", 4.25,
                "Machine Learning", 4.00,
                "Data Visualization", 3.88,
                "Critical Thinking", 4.25
            ),
            Map.of(
                "Mathematics", 4.50,
                "Computers and Electronics", 4.25,
                "English Language", 3.50
            ),
            Map.of(
                "Data Analysis", 4.50,
                "Programming", 4.00,
                "Science", 3.88
            ),
            Map.of(
                "Inductive Reasoning", 4.25,
                "Mathematical Reasoning", 4.50,
                "Deductive Reasoning", 4.12
            )
        ));
    }

    @Override
    public Optional<OnetProfile> getProfile(String socCode) {
        log.debug("Fetching O*NET profile for SOC code: {}", socCode);
        return Optional.ofNullable(MOCK_PROFILES.get(socCode));
    }

    @Override
    public Optional<Double> getBenchmark(String socCode, String competencyName) {
        return getProfile(socCode)
            .flatMap(profile -> {
                // Check all categories for the competency
                Double benchmark = profile.benchmarks().get(competencyName);
                if (benchmark != null) return Optional.of(benchmark);
                
                benchmark = profile.skills().get(competencyName);
                if (benchmark != null) return Optional.of(benchmark);
                
                benchmark = profile.abilities().get(competencyName);
                if (benchmark != null) return Optional.of(benchmark);
                
                benchmark = profile.knowledgeAreas().get(competencyName);
                return Optional.ofNullable(benchmark);
            });
    }

    @Override
    public boolean isValidSocCode(String socCode) {
        return MOCK_PROFILES.containsKey(socCode);
    }
}