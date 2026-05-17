package app.skillsoft.assessmentbackend.repository.spec;

import app.skillsoft.assessmentbackend.domain.entities.TestResult;
import app.skillsoft.assessmentbackend.domain.entities.TestSession;
import app.skillsoft.assessmentbackend.domain.entities.TemplateShareLink;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA Specifications for filtering anonymous test results.
 * All specs are composable via {@code Specification.where(...).and(...)}.
 */
public final class AnonymousResultSpecification {

    private AnonymousResultSpecification() {
        // Utility class
    }

    /**
     * Base filter: anonymous results for a specific template.
     * Matches results where session.template.id = templateId AND session.clerkUserId IS NULL.
     */
    public static Specification<TestResult> anonymousForTemplate(UUID templateId) {
        return (root, query, cb) -> {
            Join<TestResult, TestSession> session = root.join("session", JoinType.INNER);
            // Eagerly fetch template and shareLink for DTO mapping
            if (query != null && query.getResultType() == TestResult.class) {
                session.fetch("template", JoinType.INNER);
                session.fetch("shareLink", JoinType.LEFT);
            }
            return cb.and(
                    cb.equal(session.get("template").get("id"), templateId),
                    cb.isNull(session.get("clerkUserId"))
            );
        };
    }

    /**
     * Filter by completion date range.
     */
    public static Specification<TestResult> completedBetween(LocalDateTime from, LocalDateTime to) {
        return (root, query, cb) -> {
            if (from != null && to != null) {
                return cb.between(root.get("completedAt"), from, to);
            } else if (from != null) {
                return cb.greaterThanOrEqualTo(root.get("completedAt"), from);
            } else if (to != null) {
                return cb.lessThanOrEqualTo(root.get("completedAt"), to);
            }
            return null; // No constraint
        };
    }

    /**
     * Filter by score range (overallPercentage).
     */
    public static Specification<TestResult> scoreBetween(Double minScore, Double maxScore) {
        return (root, query, cb) -> {
            if (minScore != null && maxScore != null) {
                return cb.between(root.get("overallPercentage"), minScore, maxScore);
            } else if (minScore != null) {
                return cb.greaterThanOrEqualTo(root.get("overallPercentage"), minScore);
            } else if (maxScore != null) {
                return cb.lessThanOrEqualTo(root.get("overallPercentage"), maxScore);
            }
            return null;
        };
    }

    /**
     * Filter by pass/fail status.
     */
    public static Specification<TestResult> passedIs(Boolean passed) {
        return (root, query, cb) -> {
            if (passed == null) {
                return null;
            }
            return cb.equal(root.get("passed"), passed);
        };
    }

    /**
     * Filter by share link ID.
     * Reuses existing session join from {@link #anonymousForTemplate(UUID)} to avoid duplicates.
     */
    public static Specification<TestResult> fromShareLink(UUID shareLinkId) {
        return (root, query, cb) -> {
            if (shareLinkId == null) {
                return null;
            }
            Join<TestResult, TestSession> session = getOrCreateSessionJoin(root);
            Join<TestSession, TemplateShareLink> shareLink = session.join("shareLink", JoinType.INNER);
            return cb.equal(shareLink.get("id"), shareLinkId);
        };
    }

    /**
     * Reuses an existing "session" join on the root if one exists, otherwise creates a new one.
     * Prevents duplicate INNER JOINs when multiple specs access the session relationship.
     */
    @SuppressWarnings("unchecked")
    private static Join<TestResult, TestSession> getOrCreateSessionJoin(Root<TestResult> root) {
        return (Join<TestResult, TestSession>) root.getJoins().stream()
                .filter(j -> j.getAttribute().getName().equals("session"))
                .findFirst()
                .orElseGet(() -> root.join("session", JoinType.INNER));
    }
}
