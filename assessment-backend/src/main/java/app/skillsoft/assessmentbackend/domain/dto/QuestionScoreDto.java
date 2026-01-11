package app.skillsoft.assessmentbackend.domain.dto;

import java.util.UUID;

/**
 * DTO for storing individual question scores within an indicator.
 * Used by the lazy-load endpoint for detailed question breakdown.
 *
 * Includes correctAnswer for learning/review purposes.
 */
public class QuestionScoreDto {

    private UUID questionId;
    private String questionText;
    private String questionType;
    private Double score;
    private Double maxScore;
    private String userAnswer;
    private String correctAnswer;
    private Integer timeSpentSeconds;

    // Constructors
    public QuestionScoreDto() {
    }

    public QuestionScoreDto(UUID questionId, String questionText, String questionType,
                            Double score, Double maxScore) {
        this.questionId = questionId;
        this.questionText = questionText;
        this.questionType = questionType;
        this.score = score;
        this.maxScore = maxScore;
    }

    // Builder-style setters for fluent API
    public QuestionScoreDto withUserAnswer(String userAnswer) {
        this.userAnswer = userAnswer;
        return this;
    }

    public QuestionScoreDto withCorrectAnswer(String correctAnswer) {
        this.correctAnswer = correctAnswer;
        return this;
    }

    public QuestionScoreDto withTimeSpent(Integer timeSpentSeconds) {
        this.timeSpentSeconds = timeSpentSeconds;
        return this;
    }

    // Getters and Setters
    public UUID getQuestionId() {
        return questionId;
    }

    public void setQuestionId(UUID questionId) {
        this.questionId = questionId;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public String getQuestionType() {
        return questionType;
    }

    public void setQuestionType(String questionType) {
        this.questionType = questionType;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public Double getMaxScore() {
        return maxScore;
    }

    public void setMaxScore(Double maxScore) {
        this.maxScore = maxScore;
    }

    public String getUserAnswer() {
        return userAnswer;
    }

    public void setUserAnswer(String userAnswer) {
        this.userAnswer = userAnswer;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public void setCorrectAnswer(String correctAnswer) {
        this.correctAnswer = correctAnswer;
    }

    public Integer getTimeSpentSeconds() {
        return timeSpentSeconds;
    }

    public void setTimeSpentSeconds(Integer timeSpentSeconds) {
        this.timeSpentSeconds = timeSpentSeconds;
    }

    @Override
    public String toString() {
        return "QuestionScoreDto{" +
                "questionId=" + questionId +
                ", questionText='" + questionText + '\'' +
                ", questionType='" + questionType + '\'' +
                ", score=" + score +
                ", maxScore=" + maxScore +
                ", timeSpentSeconds=" + timeSpentSeconds +
                '}';
    }
}
