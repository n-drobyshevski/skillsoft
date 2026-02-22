package app.skillsoft.assessmentbackend.services.external.impl;

import app.skillsoft.assessmentbackend.config.CacheConfig;
import app.skillsoft.assessmentbackend.config.OnetProperties;
import app.skillsoft.assessmentbackend.services.external.OnetService;
import app.skillsoft.assessmentbackend.services.external.impl.OnetApiResponses.*;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * O*NET Service implementation with real API integration and mock fallback.
 *
 * <p>When {@code skillsoft.onet.enabled=true} and credentials are configured,
 * makes real HTTP calls to the O*NET Web Services API. Otherwise, falls back
 * to hardcoded mock data for 9 common occupations.</p>
 *
 * <p>Resilience patterns applied:</p>
 * <ul>
 *   <li>Circuit Breaker: Opens after 50% failure rate over 10 calls, stays open 30s</li>
 *   <li>Retry: Up to 3 attempts with 500ms wait for transient failures</li>
 *   <li>Caching: L1 cache with Caffeine (24h TTL) for frequently accessed profiles</li>
 * </ul>
 */
@Service
@Slf4j
public class OnetServiceImpl implements OnetService {

    private final OnetProperties properties;
    private final RestClient restClient;

    // Mock data for common occupations (fallback when API is disabled)
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

        // Registered Nurses profile
        MOCK_PROFILES.put("29-1141.00", new OnetProfile(
            "29-1141.00",
            "Registered Nurses",
            "Assess patient health problems and needs, develop and implement nursing care plans, and maintain medical records.",
            Map.of(
                "Critical Thinking", 4.12,
                "Active Listening", 4.25,
                "Social Perceptiveness", 4.00,
                "Service Orientation", 4.12,
                "Coordination", 3.75,
                "Monitoring", 4.00
            ),
            Map.of(
                "Medicine and Dentistry", 4.25,
                "Biology", 3.62,
                "Psychology", 3.50
            ),
            Map.of(
                "Active Listening", 4.25,
                "Social Perceptiveness", 4.00,
                "Critical Thinking", 4.12
            ),
            Map.of(
                "Problem Sensitivity", 4.25,
                "Oral Comprehension", 4.00,
                "Deductive Reasoning", 3.88
            )
        ));

        // Accountants and Auditors profile
        MOCK_PROFILES.put("13-2011.00", new OnetProfile(
            "13-2011.00",
            "Accountants and Auditors",
            "Examine, analyze, and interpret accounting records to prepare financial statements and ensure compliance.",
            Map.of(
                "Analytical Thinking", 4.25,
                "Attention to Detail", 4.50,
                "Critical Thinking", 3.88,
                "Mathematical Reasoning", 4.00,
                "Integrity", 4.25
            ),
            Map.of(
                "Economics and Accounting", 4.50,
                "Mathematics", 3.75,
                "Law and Government", 3.25
            ),
            Map.of(
                "Active Listening", 3.75,
                "Critical Thinking", 3.88,
                "Reading Comprehension", 4.00
            ),
            Map.of(
                "Mathematical Reasoning", 4.00,
                "Deductive Reasoning", 3.88,
                "Number Facility", 4.12
            )
        ));

        // Elementary School Teachers profile
        MOCK_PROFILES.put("25-2021.00", new OnetProfile(
            "25-2021.00",
            "Elementary School Teachers",
            "Teach students basic academic, social, and other formative skills in public or private schools.",
            Map.of(
                "Instructing", 4.38,
                "Active Listening", 4.12,
                "Social Perceptiveness", 4.00,
                "Learning Strategies", 4.25
            ),
            Map.of(
                "Education and Training", 4.50,
                "English Language", 3.88,
                "Psychology", 3.50
            ),
            Map.of(
                "Instructing", 4.38,
                "Speaking", 4.12,
                "Learning Strategies", 4.25
            ),
            Map.of(
                "Oral Expression", 4.25,
                "Oral Comprehension", 4.12,
                "Speech Clarity", 4.00
            )
        ));

        // Marketing Managers profile
        MOCK_PROFILES.put("11-2021.00", new OnetProfile(
            "11-2021.00",
            "Marketing Managers",
            "Plan, direct, or coordinate marketing policies and programs. Determine demand for products and services.",
            Map.of(
                "Leadership", 4.12,
                "Critical Thinking", 4.00,
                "Persuasion", 4.25,
                "Coordination", 3.88,
                "Judgment and Decision Making", 4.12,
                "Negotiation", 3.75
            ),
            Map.of(
                "Sales and Marketing", 4.50,
                "Administration and Management", 3.88,
                "Communications and Media", 4.00
            ),
            Map.of(
                "Persuasion", 4.25,
                "Negotiation", 3.75,
                "Coordination", 3.88
            ),
            Map.of(
                "Oral Expression", 4.12,
                "Written Expression", 4.00,
                "Fluency of Ideas", 3.88
            )
        ));

        // Web and Digital Interface Designers profile
        MOCK_PROFILES.put("15-1255.00", new OnetProfile(
            "15-1255.00",
            "Web and Digital Interface Designers",
            "Design digital user interfaces or websites. Develop and test layouts, interfaces, functionality, and navigation for usability.",
            Map.of(
                "Critical Thinking", 3.88,
                "Complex Problem Solving", 3.75,
                "Active Learning", 3.62,
                "Judgment and Decision Making", 3.50,
                "Systems Evaluation", 3.38
            ),
            Map.of(
                "Computers and Electronics", 4.25,
                "Design", 4.50,
                "Communications and Media", 3.88,
                "English Language", 3.50
            ),
            Map.of(
                "Programming", 4.00,
                "Technology Design", 4.25,
                "Operations Analysis", 3.50
            ),
            Map.of(
                "Fluency of Ideas", 4.00,
                "Originality", 3.88,
                "Information Ordering", 3.75,
                "Visualization", 4.12
            )
        ));

        // Secretaries and Administrative Assistants profile
        MOCK_PROFILES.put("43-6014.00", new OnetProfile(
            "43-6014.00",
            "Secretaries and Administrative Assistants",
            "Perform routine administrative functions such as drafting correspondence, scheduling, and organizing files.",
            Map.of(
                "Attention to Detail", 4.00,
                "Time Management", 3.88,
                "Active Listening", 3.62,
                "Service Orientation", 3.75
            ),
            Map.of(
                "Administrative", 4.12,
                "English Language", 3.75,
                "Computers and Electronics", 3.50
            ),
            Map.of(
                "Active Listening", 3.62,
                "Writing", 3.50,
                "Time Management", 3.88
            ),
            Map.of(
                "Oral Comprehension", 3.75,
                "Written Comprehension", 3.62,
                "Information Ordering", 3.50
            )
        ));
    }

    public OnetServiceImpl(OnetProperties properties) {
        this.properties = properties;
        if (properties.isEnabled() && hasCredentials(properties)) {
            this.restClient = RestClient.builder()
                    .baseUrl(properties.getBaseUrl())
                    .defaultHeader(HttpHeaders.AUTHORIZATION, basicAuth(properties))
                    .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .build();
            log.info("O*NET API integration enabled (base URL: {})", properties.getBaseUrl());
        } else {
            this.restClient = null;
            log.info("O*NET API integration disabled — using mock data");
        }
    }

    /**
     * Package-private constructor for testing with an injected RestClient.
     */
    OnetServiceImpl(OnetProperties properties, RestClient restClient) {
        this.properties = properties;
        this.restClient = restClient;
    }

    @Override
    @CircuitBreaker(name = "onetService", fallbackMethod = "getProfileFallback")
    @Retry(name = "externalServices")
    @Cacheable(
        value = CacheConfig.ONET_PROFILES_CACHE,
        key = "#socCode",
        unless = "#result == null"
    )
    public Optional<OnetProfile> getProfile(String socCode) {
        log.debug("Fetching O*NET profile for SOC code: {} (cache miss)", socCode);

        if (!isApiEnabled()) {
            return Optional.ofNullable(MOCK_PROFILES.get(socCode));
        }

        return fetchProfileFromApi(socCode);
    }

    private Optional<OnetProfile> getProfileFallback(String socCode, Exception e) {
        log.warn("O*NET service unavailable for SOC code {}: {}", socCode, e.getMessage());
        return Optional.empty();
    }

    @Override
    @CircuitBreaker(name = "onetService", fallbackMethod = "getBenchmarkFallback")
    @Retry(name = "externalServices")
    public Optional<Double> getBenchmark(String socCode, String competencyName) {
        return getProfileInternal(socCode)
            .flatMap(profile -> {
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

    private Optional<Double> getBenchmarkFallback(String socCode, String competencyName, Exception e) {
        log.warn("O*NET benchmark unavailable for SOC code {} / competency {}: {}",
                 socCode, competencyName, e.getMessage());
        return Optional.empty();
    }

    @Override
    @CircuitBreaker(name = "onetService", fallbackMethod = "isValidSocCodeFallback")
    @Retry(name = "externalServices")
    public boolean isValidSocCode(String socCode) {
        return getProfileInternal(socCode).isPresent();
    }

    private boolean isValidSocCodeFallback(String socCode, Exception e) {
        log.warn("O*NET SOC code validation unavailable for {}: {}", socCode, e.getMessage());
        return false;
    }

    @Override
    @Cacheable(
        value = CacheConfig.ONET_PROFILES_CACHE,
        key = "'search:' + #keyword.toLowerCase()"
    )
    public List<OnetProfile> searchProfiles(String keyword) {
        log.debug("Searching O*NET profiles for keyword: {} (cache miss)", keyword);
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }

        if (!isApiEnabled()) {
            return searchMockProfiles(keyword);
        }

        return searchFromApi(keyword);
    }

    // ===== API Methods =====

    private Optional<OnetProfile> fetchProfileFromApi(String socCode) {
        // 1. Fetch occupation details
        OccupationWrapper wrapper = restClient.get()
                .uri("/online/occupations/{socCode}", socCode)
                .retrieve()
                .body(OccupationWrapper.class);

        if (wrapper == null || wrapper.occupation() == null) {
            return Optional.empty();
        }

        OccupationDetail detail = wrapper.occupation();

        // 2. Fetch skills
        Map<String, Double> skills = fetchCategoryScores(
                "/online/occupations/{socCode}/summary/skills", socCode);

        // 3. Fetch abilities
        Map<String, Double> abilities = fetchCategoryScores(
                "/online/occupations/{socCode}/summary/abilities", socCode);

        // 4. Fetch knowledge areas
        Map<String, Double> knowledgeAreas = fetchCategoryScores(
                "/online/occupations/{socCode}/summary/knowledge", socCode);

        // Benchmarks = merged skills + abilities (matching mock data structure)
        Map<String, Double> benchmarks = new LinkedHashMap<>(skills);
        benchmarks.putAll(abilities);

        return Optional.of(new OnetProfile(
                detail.code(),
                detail.title(),
                detail.description(),
                Collections.unmodifiableMap(benchmarks),
                Collections.unmodifiableMap(knowledgeAreas),
                Collections.unmodifiableMap(skills),
                Collections.unmodifiableMap(abilities)
        ));
    }

    private Map<String, Double> fetchCategoryScores(String uriTemplate, String socCode) {
        CategoryResponse response = restClient.get()
                .uri(uriTemplate, socCode)
                .retrieve()
                .body(CategoryResponse.class);

        if (response == null || response.element() == null) {
            return Map.of();
        }

        return convertScores(response.element());
    }

    private List<OnetProfile> searchFromApi(String keyword) {
        try {
            SearchResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/online/search")
                            .queryParam("keyword", keyword)
                            .queryParam("end", 10)
                            .build())
                    .retrieve()
                    .body(SearchResponse.class);

            if (response == null || response.occupation() == null) {
                return List.of();
            }

            return response.occupation().stream()
                    .map(occ -> new OnetProfile(
                            occ.code(),
                            occ.title(),
                            "",
                            Map.of(),
                            Map.of(),
                            Map.of(),
                            Map.of()
                    ))
                    .collect(Collectors.toList());
        } catch (RestClientException e) {
            log.warn("O*NET search API failed for keyword '{}', falling back to mock: {}",
                     keyword, e.getMessage());
            return searchMockProfiles(keyword);
        }
    }

    // ===== Mock Methods =====

    private List<OnetProfile> searchMockProfiles(String keyword) {
        String lowerKeyword = keyword.toLowerCase();
        return MOCK_PROFILES.values().stream()
            .filter(p -> p.socCode().toLowerCase().startsWith(lowerKeyword)
                      || p.occupationTitle().toLowerCase().contains(lowerKeyword)
                      || p.description().toLowerCase().contains(lowerKeyword))
            .limit(10)
            .collect(Collectors.toList());
    }

    /**
     * Internal profile lookup without circuit breaker (avoids double-wrapping).
     */
    private Optional<OnetProfile> getProfileInternal(String socCode) {
        if (!isApiEnabled()) {
            return Optional.ofNullable(MOCK_PROFILES.get(socCode));
        }
        return fetchProfileFromApi(socCode);
    }

    // ===== Helpers =====

    private boolean isApiEnabled() {
        return properties.isEnabled() && restClient != null;
    }

    /**
     * Convert O*NET element scores to a name→score map.
     * Only uses "LV" (Level) scale scores, converts from 0-100 to 1-5 scale.
     */
    private Map<String, Double> convertScores(List<ElementScore> elements) {
        Map<String, Double> result = new LinkedHashMap<>();
        for (ElementScore element : elements) {
            if (element.score() != null && "LV".equals(element.score().scaleId())) {
                double converted = (element.score().value() / 100.0) * 5.0;
                double clamped = Math.max(1.0, Math.min(5.0, converted));
                result.put(element.name(), clamped);
            }
        }
        return result;
    }

    private static boolean hasCredentials(OnetProperties props) {
        return props.getUsername() != null && !props.getUsername().isBlank()
                && props.getApiKey() != null && !props.getApiKey().isBlank();
    }

    private static String basicAuth(OnetProperties props) {
        String credentials = props.getUsername() + ":" + props.getApiKey();
        return "Basic " + Base64.getEncoder().encodeToString(
                credentials.getBytes(StandardCharsets.UTF_8));
    }
}
