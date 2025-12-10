package app.skillsoft.assessmentbackend.domain.entities;

/**
 * Context Scope for Behavioral Indicators
 * 
 * Part of the Two-Tier Scoping System for Smart Assessment filtering.
 * This enum classifies behavioral indicators by their applicability scope,
 * enabling context-neutral filtering for Scenario A (Universal Baseline) assessments.
 * 
 * Usage:
 * - UNIVERSAL: Used in Scenario A for Competency Passport generation
 * - PROFESSIONAL/TECHNICAL/MANAGERIAL: Used in Scenario B/C for targeted assessments
 * 
 * Related to ROADMAP.md "Smart Assessment: Tags & Scoping Strategy"
 */
public enum ContextScope {
    /**
     * Universal scope - Applies to every human regardless of job role.
     * Examples: "Active Listening", "Emotional Regulation", "Time Awareness"
     * 
     * Critical for Scenario A (Universal Baseline) - these indicators form
     * the foundation of the context-neutral Competency Passport.
     */
    UNIVERSAL,

    /**
     * Professional scope - Applies to white-collar/office jobs.
     * Examples: "Email Etiquette", "Meeting Facilitation", "Professional Writing"
     * 
     * Used in Scenario B (Job Fit) for office-based occupations.
     */
    PROFESSIONAL,

    /**
     * Technical scope - Specific to technical/hard-skill contexts.
     * Examples: "Code Review", "Technical Documentation", "Debugging Strategy"
     * 
     * Used in Scenario B (Job Fit) for technical roles (IT, Engineering, etc.)
     */
    TECHNICAL,

    /**
     * Managerial scope - Specific to people management contexts.
     * Examples: "Delegation", "Performance Feedback", "Team Motivation"
     * 
     * Used in Scenario B (Job Fit) and Scenario C (Team Fit) for leadership roles.
     */
    MANAGERIAL
}
