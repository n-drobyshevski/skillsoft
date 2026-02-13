package app.skillsoft.assessmentbackend.testutils;

import app.skillsoft.assessmentbackend.domain.entities.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Factory class for creating test entities with bilingual (English/Russian) support.
 *
 * All factory methods create entities with realistic test data that includes both
 * English and Russian text to ensure proper handling of Cyrillic characters.
 *
 * Usage:
 * <pre>
 * // Create a single entity
 * User admin = TestDataFactory.createUser(UserRole.ADMIN);
 * Competency competency = TestDataFactory.createCompetency();
 * TestTemplate template = TestDataFactory.createTestTemplate(AssessmentGoal.JOB_FIT);
 *
 * // Create with custom ID
 * UUID customId = UUID.randomUUID();
 * Competency withId = TestDataFactory.createCompetencyWithId(customId);
 *
 * // Create multiple entities
 * List&lt;Competency&gt; competencies = TestDataFactory.createCompetencies(5);
 * </pre>
 */
public final class TestDataFactory {

    private static final AtomicInteger COUNTER = new AtomicInteger(1);

    // English/Russian test data pairs for bilingual testing
    private static final String[] COMPETENCY_NAMES = {
        "Strategic Leadership / Стратегическое лидерство",
        "Critical Thinking / Критическое мышление",
        "Communication Skills / Навыки коммуникации",
        "Problem Solving / Решение проблем",
        "Team Collaboration / Командная работа"
    };

    private static final String[] INDICATOR_TITLES = {
        "Decision Making / Принятие решений",
        "Conflict Resolution / Разрешение конфликтов",
        "Active Listening / Активное слушание",
        "Time Management / Управление временем",
        "Adaptability / Адаптивность"
    };

    private static final String[] TEAM_NAMES = {
        "Alpha Team / Команда Альфа",
        "Development Squad / Отдел разработки",
        "Innovation Lab / Инновационная лаборатория",
        "Customer Success / Клиентский успех"
    };

    private TestDataFactory() {
        // Utility class - no instantiation
    }

    /**
     * Resets the counter for sequential IDs. Useful for deterministic test data.
     */
    public static void resetCounter() {
        COUNTER.set(1);
    }

    // ============================================
    // USER FACTORY METHODS
    // ============================================

    /**
     * Creates a User entity with the specified role.
     *
     * @param role The UserRole for the user
     * @return A new User entity (not persisted)
     */
    public static User createUser(UserRole role) {
        int count = COUNTER.getAndIncrement();
        String roleSuffix = role.name().toLowerCase();

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setClerkId("clerk_test_" + roleSuffix + "_" + count);
        user.setEmail(roleSuffix + count + "@test.skillsoft.app");
        user.setUsername("test" + roleSuffix + count);
        user.setFirstName("Test / Тест");
        user.setLastName(role.getDisplayName() + " " + count);
        user.setRole(role);
        user.setActive(true);
        user.setBanned(false);
        user.setLocked(false);
        user.setHasImage(false);
        user.setPreferences("{}");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setClerkCreatedAt(LocalDateTime.now().minusDays(30));

        return user;
    }

    /**
     * Creates an admin user.
     * @return A new User entity with ADMIN role
     */
    public static User createAdminUser() {
        return createUser(UserRole.ADMIN);
    }

    /**
     * Creates an editor user.
     * @return A new User entity with EDITOR role
     */
    public static User createEditorUser() {
        return createUser(UserRole.EDITOR);
    }

    /**
     * Creates a basic user.
     * @return A new User entity with USER role
     */
    public static User createBasicUser() {
        return createUser(UserRole.USER);
    }

    /**
     * Creates a User with a specific clerk ID (useful for mocking Clerk integration).
     *
     * @param clerkId The Clerk ID to assign
     * @param role The UserRole
     * @return A new User entity
     */
    public static User createUserWithClerkId(String clerkId, UserRole role) {
        User user = createUser(role);
        user.setClerkId(clerkId);
        return user;
    }

    // ============================================
    // COMPETENCY FACTORY METHODS
    // ============================================

    /**
     * Creates a Competency entity with bilingual test data.
     *
     * @return A new Competency entity (not persisted)
     */
    public static Competency createCompetency() {
        int count = COUNTER.getAndIncrement();
        int nameIndex = count % COMPETENCY_NAMES.length;

        Competency competency = new Competency();
        competency.setId(UUID.randomUUID());
        competency.setName(COMPETENCY_NAMES[nameIndex] + " " + count);
        competency.setDescription("Test competency description / Описание тестовой компетенции " + count);
        competency.setCategory(CompetencyCategory.values()[count % CompetencyCategory.values().length]);
        competency.setApprovalStatus(ApprovalStatus.APPROVED);
        competency.setActive(true);

        return competency;
    }

    /**
     * Creates a Competency with a specific ID.
     *
     * @param id The UUID to assign
     * @return A new Competency entity
     */
    public static Competency createCompetencyWithId(UUID id) {
        Competency competency = createCompetency();
        competency.setId(id);
        return competency;
    }

    /**
     * Creates multiple competencies.
     *
     * @param count Number of competencies to create
     * @return List of Competency entities
     */
    public static List<Competency> createCompetencies(int count) {
        List<Competency> competencies = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            competencies.add(createCompetency());
        }
        return competencies;
    }

    // ============================================
    // BEHAVIORAL INDICATOR FACTORY METHODS
    // ============================================

    /**
     * Creates a BehavioralIndicator entity with bilingual test data.
     *
     * @return A new BehavioralIndicator entity (not persisted)
     */
    public static BehavioralIndicator createBehavioralIndicator() {
        int count = COUNTER.getAndIncrement();
        int titleIndex = count % INDICATOR_TITLES.length;

        BehavioralIndicator indicator = new BehavioralIndicator();
        indicator.setId(UUID.randomUUID());
        indicator.setTitle(INDICATOR_TITLES[titleIndex] + " " + count);
        indicator.setDescription("Indicator description / Описание индикатора " + count);
        indicator.setContextScope(ContextScope.values()[count % ContextScope.values().length]);
        indicator.setObservabilityLevel(ObservabilityLevel.values()[count % ObservabilityLevel.values().length]);
        indicator.setWeight(1.0f + (count % 3) * 0.5f);
        indicator.setActive(true);

        return indicator;
    }

    /**
     * Creates a BehavioralIndicator linked to a Competency.
     *
     * @param competency The parent Competency
     * @return A new BehavioralIndicator entity
     */
    public static BehavioralIndicator createBehavioralIndicator(Competency competency) {
        BehavioralIndicator indicator = createBehavioralIndicator();
        indicator.setCompetency(competency);
        return indicator;
    }

    // ============================================
    // ASSESSMENT QUESTION FACTORY METHODS
    // ============================================

    /**
     * Creates an AssessmentQuestion entity with bilingual test data.
     *
     * @param type The question type
     * @return A new AssessmentQuestion entity (not persisted)
     */
    public static AssessmentQuestion createAssessmentQuestion(QuestionType type) {
        int count = COUNTER.getAndIncrement();

        AssessmentQuestion question = new AssessmentQuestion();
        question.setId(UUID.randomUUID());
        question.setQuestionText("Test question / Тестовый вопрос " + count);
        question.setQuestionType(type);
        question.setAnswerOptions(createAnswerOptionsForType(type));
        question.setScoringRubric("Scoring rubric / Критерии оценки " + count);
        question.setTimeLimit(300);
        question.setDifficultyLevel(DifficultyLevel.values()[count % DifficultyLevel.values().length]);
        question.setActive(true);
        question.setOrderIndex(count);

        return question;
    }

    /**
     * Creates an AssessmentQuestion linked to a BehavioralIndicator.
     *
     * @param indicator The parent BehavioralIndicator
     * @param type The question type
     * @return A new AssessmentQuestion entity
     */
    public static AssessmentQuestion createAssessmentQuestion(BehavioralIndicator indicator, QuestionType type) {
        AssessmentQuestion question = createAssessmentQuestion(type);
        question.setBehavioralIndicator(indicator);
        return question;
    }

    /**
     * Creates answer options appropriate for the question type.
     */
    private static List<Map<String, Object>> createAnswerOptionsForType(QuestionType type) {
        List<Map<String, Object>> options = new ArrayList<>();

        switch (type) {
            case LIKERT_SCALE, FREQUENCY_SCALE -> {
                String[] labels = {"Полностью не согласен", "Не согласен", "Нейтрально", "Согласен", "Полностью согласен"};
                for (int i = 0; i < 5; i++) {
                    Map<String, Object> option = new HashMap<>();
                    option.put("value", i + 1);
                    option.put("label", labels[i]);
                    option.put("score", i + 1);
                    options.add(option);
                }
            }
            case MULTIPLE_CHOICE -> {
                Map<String, Object> option1 = new HashMap<>();
                option1.put("id", "A");
                option1.put("text", "Option A / Вариант А");
                option1.put("isCorrect", true);
                options.add(option1);

                Map<String, Object> option2 = new HashMap<>();
                option2.put("id", "B");
                option2.put("text", "Option B / Вариант Б");
                option2.put("isCorrect", false);
                options.add(option2);

                Map<String, Object> option3 = new HashMap<>();
                option3.put("id", "C");
                option3.put("text", "Option C / Вариант В");
                option3.put("isCorrect", false);
                options.add(option3);
            }
            case SITUATIONAL_JUDGMENT -> {
                Map<String, Object> sjOption1 = new HashMap<>();
                sjOption1.put("action", "Immediate action / Немедленное действие");
                sjOption1.put("effectiveness", 3);
                options.add(sjOption1);

                Map<String, Object> sjOption2 = new HashMap<>();
                sjOption2.put("action", "Consult others / Консультация");
                sjOption2.put("effectiveness", 5);
                options.add(sjOption2);
            }
            default -> {
                // Default options for other types
                Map<String, Object> defaultOption = new HashMap<>();
                defaultOption.put("value", 1);
                defaultOption.put("label", "Default option / Вариант по умолчанию");
                options.add(defaultOption);
            }
        }

        return options;
    }

    // ============================================
    // TEST TEMPLATE FACTORY METHODS
    // ============================================

    /**
     * Creates a TestTemplate entity with the specified assessment goal.
     *
     * @param goal The assessment goal (OVERVIEW, JOB_FIT, TEAM_FIT)
     * @return A new TestTemplate entity (not persisted)
     */
    public static TestTemplate createTestTemplate(AssessmentGoal goal) {
        int count = COUNTER.getAndIncrement();

        TestTemplate template = new TestTemplate();
        template.setId(UUID.randomUUID());
        template.setName("Test Template / Тестовый шаблон " + count);
        template.setDescription("Template description / Описание шаблона " + count);
        template.setGoal(goal);
        template.setStatus(TemplateStatus.DRAFT);
        template.setVersion(1);
        template.setQuestionsPerIndicator(3);
        template.setTimeLimitMinutes(60);
        template.setPassingScore(70.0);
        template.setIsActive(true);
        template.setShuffleQuestions(true);
        template.setShuffleOptions(true);
        template.setAllowSkip(true);
        template.setAllowBackNavigation(true);
        template.setShowResultsImmediately(true);
        template.setVisibility(TemplateVisibility.PRIVATE);
        template.setBlueprint(createBlueprintForGoal(goal));

        return template;
    }

    /**
     * Creates a TestTemplate with an owner.
     *
     * @param goal The assessment goal
     * @param owner The template owner
     * @return A new TestTemplate entity
     */
    public static TestTemplate createTestTemplate(AssessmentGoal goal, User owner) {
        TestTemplate template = createTestTemplate(goal);
        template.setOwner(owner);
        return template;
    }

    /**
     * Creates a blueprint map for the specified goal.
     */
    private static Map<String, Object> createBlueprintForGoal(AssessmentGoal goal) {
        Map<String, Object> blueprint = new HashMap<>();

        switch (goal) {
            case OVERVIEW -> {
                blueprint.put("strategy", "UNIVERSAL_BASELINE");
                blueprint.put("competencies", "CROSS_FUNCTIONAL_ONLY");
                blueprint.put("saveAsPassport", true);
                blueprint.put("indicatorsPerCompetency", 2);
                blueprint.put("questionsPerIndicator", 2);
            }
            case JOB_FIT -> {
                blueprint.put("strategy", "TARGETED_FIT");
                blueprint.put("onetSocCode", "15-1132.00");
                blueprint.put("useOnetBenchmarks", true);
                blueprint.put("reusePassportData", true);
            }
            case TEAM_FIT -> {
                blueprint.put("strategy", "DYNAMIC_GAP_ANALYSIS");
                blueprint.put("teamId", UUID.randomUUID().toString());
                blueprint.put("normalizationStandard", "ESCO_V1");
                blueprint.put("saturationThreshold", 0.75);
            }
        }

        return blueprint;
    }

    // ============================================
    // TEST SESSION FACTORY METHODS
    // ============================================

    /**
     * Creates a TestSession entity.
     *
     * @param template The test template
     * @param clerkUserId The Clerk user ID of the user taking the test
     * @return A new TestSession entity (not persisted)
     */
    public static TestSession createTestSession(TestTemplate template, String clerkUserId) {
        TestSession session = new TestSession();
        session.setId(UUID.randomUUID());
        session.setTemplate(template);
        session.setClerkUserId(clerkUserId);
        session.setStatus(SessionStatus.NOT_STARTED);
        session.setStartedAt(null);
        session.setCompletedAt(null);

        return session;
    }

    /**
     * Creates a TestSession entity using a User entity.
     *
     * @param template The test template
     * @param user The user taking the test
     * @return A new TestSession entity (not persisted)
     */
    public static TestSession createTestSession(TestTemplate template, User user) {
        return createTestSession(template, user.getClerkId());
    }

    /**
     * Creates a TestSession that has been started.
     *
     * @param template The test template
     * @param clerkUserId The Clerk user ID of the user taking the test
     * @return A new TestSession entity in IN_PROGRESS status
     */
    public static TestSession createInProgressSession(TestTemplate template, String clerkUserId) {
        TestSession session = createTestSession(template, clerkUserId);
        session.setStatus(SessionStatus.IN_PROGRESS);
        session.setStartedAt(LocalDateTime.now());
        return session;
    }

    /**
     * Creates a TestSession that has been started using a User entity.
     *
     * @param template The test template
     * @param user The user taking the test
     * @return A new TestSession entity in IN_PROGRESS status
     */
    public static TestSession createInProgressSession(TestTemplate template, User user) {
        return createInProgressSession(template, user.getClerkId());
    }

    // ============================================
    // TEAM FACTORY METHODS
    // ============================================

    /**
     * Creates a Team entity with bilingual test data.
     *
     * @param creator The user creating the team
     * @return A new Team entity (not persisted)
     */
    public static Team createTeam(User creator) {
        int count = COUNTER.getAndIncrement();
        int nameIndex = count % TEAM_NAMES.length;

        Team team = new Team();
        team.setId(UUID.randomUUID());
        team.setName(TEAM_NAMES[nameIndex] + " " + count);
        team.setDescription("Team description / Описание команды " + count);
        team.setCreatedBy(creator);
        team.setMetadata("{}");

        return team;
    }

    /**
     * Creates a Team with members.
     *
     * @param creator The user creating the team
     * @param memberCount Number of members to add (excluding creator)
     * @return A new Team entity with members
     */
    public static Team createTeamWithMembers(User creator, int memberCount) {
        Team team = createTeam(creator);

        // Add creator as a member
        TeamMember creatorMember = new TeamMember();
        creatorMember.setId(UUID.randomUUID());
        creatorMember.setTeam(team);
        creatorMember.setUser(creator);
        creatorMember.setRole(TeamMemberRole.LEADER);
        creatorMember.setActive(true);
        team.getMembers().add(creatorMember);

        // Add additional members
        for (int i = 0; i < memberCount; i++) {
            User member = createBasicUser();
            TeamMember teamMember = new TeamMember();
            teamMember.setId(UUID.randomUUID());
            teamMember.setTeam(team);
            teamMember.setUser(member);
            teamMember.setRole(TeamMemberRole.MEMBER);
            teamMember.setActive(true);
            team.getMembers().add(teamMember);
        }

        return team;
    }

    // ============================================
    // TEMPLATE SHARE FACTORY METHODS
    // ============================================

    /**
     * Creates a TemplateShare entity for a user.
     *
     * @param template The template being shared
     * @param granteeUser The user receiving access
     * @param permission The permission level
     * @return A new TemplateShare entity (not persisted)
     */
    public static TemplateShare createTemplateShare(
            TestTemplate template,
            User granteeUser,
            SharePermission permission) {

        TemplateShare share = new TemplateShare();
        share.setId(UUID.randomUUID());
        share.setTemplate(template);
        share.setGranteeType(GranteeType.USER);
        share.setUser(granteeUser);
        share.setPermission(permission);
        share.setGrantedBy(template.getOwner());
        share.setGrantedAt(LocalDateTime.now());
        share.setActive(true);

        return share;
    }

    /**
     * Creates a TemplateShare entity for a team.
     *
     * @param template The template being shared
     * @param granteeTeam The team receiving access
     * @param permission The permission level
     * @return A new TemplateShare entity (not persisted)
     */
    public static TemplateShare createTemplateShareForTeam(
            TestTemplate template,
            Team granteeTeam,
            SharePermission permission) {

        TemplateShare share = new TemplateShare();
        share.setId(UUID.randomUUID());
        share.setTemplate(template);
        share.setGranteeType(GranteeType.TEAM);
        share.setTeam(granteeTeam);
        share.setPermission(permission);
        share.setGrantedBy(template.getOwner());
        share.setGrantedAt(LocalDateTime.now());
        share.setActive(true);

        return share;
    }

    /**
     * Creates a TemplateShareLink entity.
     *
     * @param template The template being shared
     * @param permission The permission level for link access
     * @return A new TemplateShareLink entity (not persisted)
     */
    public static TemplateShareLink createTemplateShareLink(
            TestTemplate template,
            SharePermission permission) {

        TemplateShareLink link = new TemplateShareLink();
        link.setId(UUID.randomUUID());
        link.setTemplate(template);
        link.setToken(UUID.randomUUID().toString().replace("-", ""));
        link.setPermission(permission);
        link.setCreatedBy(template.getOwner());
        link.setCreatedAt(LocalDateTime.now());
        link.setExpiresAt(LocalDateTime.now().plusDays(7));
        link.setActive(true);
        link.setMaxUses(null); // Unlimited
        link.setCurrentUses(0);

        return link;
    }
}
