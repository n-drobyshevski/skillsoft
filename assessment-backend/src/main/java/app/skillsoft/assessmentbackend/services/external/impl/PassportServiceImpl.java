package app.skillsoft.assessmentbackend.services.external.impl;

import app.skillsoft.assessmentbackend.services.external.PassportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mock implementation of PassportService for development and testing.
 * 
 * In production, this would integrate with a persistent storage system
 * for candidate competency passports.
 */
@Service
@Slf4j
public class PassportServiceImpl implements PassportService {

    // In-memory storage for mock passports
    private final Map<UUID, CompetencyPassport> passportStore = new ConcurrentHashMap<>();

    @Override
    public Optional<CompetencyPassport> getPassport(UUID candidateId) {
        log.debug("Fetching passport for candidate: {}", candidateId);
        return Optional.ofNullable(passportStore.get(candidateId))
            .filter(CompetencyPassport::isValid);
    }

    @Override
    public Optional<Double> getCompetencyScore(UUID candidateId, UUID competencyId) {
        return getPassport(candidateId)
            .map(passport -> passport.competencyScores().get(competencyId));
    }

    @Override
    public boolean hasValidPassport(UUID candidateId) {
        return getPassport(candidateId).isPresent();
    }

    @Override
    public void savePassport(CompetencyPassport passport) {
        log.debug("Saving passport for candidate: {}", passport.candidateId());
        passportStore.put(passport.candidateId(), passport);
    }

    /**
     * Create a demo passport for testing purposes.
     * 
     * @param candidateId The candidate ID
     * @param competencyScores Map of competency IDs to scores
     * @return The created passport
     */
    public CompetencyPassport createDemoPassport(UUID candidateId, Map<UUID, Double> competencyScores) {
        CompetencyPassport passport = new CompetencyPassport(
            candidateId,
            competencyScores,
            Map.of(
                "OPENNESS", 3.5,
                "CONSCIENTIOUSNESS", 4.0,
                "EXTRAVERSION", 3.2,
                "AGREEABLENESS", 3.8,
                "NEUROTICISM", 2.5
            ),
            LocalDateTime.now(),
            true
        );
        savePassport(passport);
        return passport;
    }
}