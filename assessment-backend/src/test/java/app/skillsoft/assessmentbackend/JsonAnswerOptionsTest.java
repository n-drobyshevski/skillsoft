package app.skillsoft.assessmentbackend;

import app.skillsoft.assessmentbackend.config.TestJacksonConfig;
import app.skillsoft.assessmentbackend.config.TestHibernateConfig;
import app.skillsoft.assessmentbackend.domain.entities.AssessmentQuestion;
import app.skillsoft.assessmentbackend.domain.entities.QuestionType;
import app.skillsoft.assessmentbackend.repository.AssessmentQuestionRepository;
import app.skillsoft.assessmentbackend.util.AnswerOptionUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for basic JSON answer options storage and retrieval functionality
 */
@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestJacksonConfig.class, TestHibernateConfig.class})
public class JsonAnswerOptionsTest {

    @Autowired
    private AssessmentQuestionRepository questionRepository;
    
    @BeforeEach
    void setup() {
        questionRepository.deleteAll();
    }
    
    @Test
    void testLikertScaleAnswerOptions() {
        // Create a simple Likert scale question with answer options
        AssessmentQuestion question = new AssessmentQuestion();
        question.setQuestionText("How satisfied are you with the team communication?");
        question.setQuestionType(QuestionType.LIKERT_SCALE);
        
        // Create Likert scale options
        List<String> labels = List.of("Very Dissatisfied", "Dissatisfied", "Neutral", "Satisfied", "Very Satisfied");
        List<Integer> values = List.of(1, 2, 3, 4, 5);
        
        List<Map<String, Object>> answerOptions = AnswerOptionUtils.createLikertScaleOptions(labels, values);
        question.setAnswerOptions(answerOptions);
        
        // Save and retrieve the question
        AssessmentQuestion savedQuestion = questionRepository.save(question);
        AssessmentQuestion retrievedQuestion = questionRepository.findById(savedQuestion.getId()).orElse(null);
        
        // Verify
        assertNotNull(retrievedQuestion);
        assertEquals(5, retrievedQuestion.getAnswerOptions().size());
        assertEquals("Very Satisfied", retrievedQuestion.getAnswerOptions().get(4).get("label"));
        assertEquals(5, retrievedQuestion.getAnswerOptions().get(4).get("value"));
    }
    
    @Test
    void testMultipleChoiceAnswerOptions() {
        // Create a multiple choice question with correct answer
        AssessmentQuestion question = new AssessmentQuestion();
        question.setQuestionText("Which communication style is most effective for addressing conflicts?");
        question.setQuestionType(QuestionType.MULTIPLE_CHOICE);
        
        // Create multiple choice options with one correct answer
        List<String> options = List.of(
            "Passive communication", 
            "Aggressive communication",
            "Assertive communication", 
            "Avoidant communication"
        );
        List<Boolean> correctAnswers = List.of(false, false, true, false);
        
        List<Map<String, Object>> answerOptions = AnswerOptionUtils.createMultipleChoiceOptions(options, correctAnswers);
        question.setAnswerOptions(answerOptions);
        
        // Save and retrieve
        AssessmentQuestion savedQuestion = questionRepository.save(question);
        AssessmentQuestion retrievedQuestion = questionRepository.findById(savedQuestion.getId()).orElse(null);
        
        // Verify
        assertNotNull(retrievedQuestion);
        assertEquals(4, retrievedQuestion.getAnswerOptions().size());
        
        // Verify the correct option (using "text" key as per AnswerOptionUtils)
        boolean hasCorrectOption = false;
        for (Map<String, Object> option : retrievedQuestion.getAnswerOptions()) {
            if (option.get("text") != null && option.get("text").equals("Assertive communication") && 
                Boolean.TRUE.equals(option.get("correct"))) {
                hasCorrectOption = true;
                break;
            }
        }
        assertTrue(hasCorrectOption, "Should have found the correct answer option");
    }
    
    @Test
    void testSituationalJudgmentAnswerOptions() {
        // Create a situational judgment question
        AssessmentQuestion question = new AssessmentQuestion();
        question.setQuestionText("Your team member is consistently late to meetings. What do you do?");
        question.setQuestionType(QuestionType.SITUATIONAL_JUDGMENT);
        
        // Create situational judgment options
        List<String> options = List.of(
            "Ignore the behavior to avoid confrontation",
            "Report them to management immediately",
            "Speak to them privately about the issue",
            "Call them out in front of the team"
        );
        
        List<Integer> scores = List.of(2, 3, 5, 1);
        List<String> explanations = List.of(
            "Avoids addressing the issue",
            "Escalates too quickly without direct communication",
            "Addresses the issue professionally with respect",
            "Creates unnecessary tension in the team"
        );
        
        List<Map<String, Object>> answerOptions = AnswerOptionUtils.createSituationalJudgmentOptions(
            options, scores, explanations);
        question.setAnswerOptions(answerOptions);
        
        // Save and retrieve the question
        AssessmentQuestion savedQuestion = questionRepository.save(question);
        AssessmentQuestion retrievedQuestion = questionRepository.findById(savedQuestion.getId()).orElse(null);
        
        // Verify
        assertNotNull(retrievedQuestion);
        assertEquals(4, retrievedQuestion.getAnswerOptions().size());
        
        // Verify the explanation is present (using "text" and "explanation" keys as per AnswerOptionUtils)
        boolean hasHighScoreOption = false;
        for (Map<String, Object> option : retrievedQuestion.getAnswerOptions()) {
            if (option.get("explanation") != null && 
                option.get("explanation").equals("Addresses the issue professionally with respect") && 
                (Integer)option.get("score") == 5) {
                hasHighScoreOption = true;
                break;
            }
        }
        assertTrue(hasHighScoreOption, "Should have found the high scoring option with explanation");
    }
    
    @Test
    void testBasicJsonStorage() {
        // Test that JSONB storage and retrieval works correctly
        AssessmentQuestion question = new AssessmentQuestion();
        question.setQuestionText("Basic JSON storage test");
        question.setQuestionType(QuestionType.LIKERT_SCALE);
        
        // Create simple JSON structure
        List<Map<String, Object>> options = AnswerOptionUtils.createLikertScaleOptions(
            List.of("Poor", "Fair", "Good", "Very Good", "Excellent"),
            List.of(1, 2, 3, 4, 5)
        );
        question.setAnswerOptions(options);
        
        // Save and retrieve
        AssessmentQuestion savedQuestion = questionRepository.save(question);
        AssessmentQuestion retrievedQuestion = questionRepository.findById(savedQuestion.getId()).orElse(null);
        
        // Verify JSON structure is preserved (using "label" and "value" keys as per AnswerOptionUtils)
        assertNotNull(retrievedQuestion);
        assertNotNull(retrievedQuestion.getAnswerOptions());
        assertEquals(5, retrievedQuestion.getAnswerOptions().size());
        assertEquals("Excellent", retrievedQuestion.getAnswerOptions().get(4).get("label"));
        assertEquals(5, retrievedQuestion.getAnswerOptions().get(4).get("value"));
    }
}