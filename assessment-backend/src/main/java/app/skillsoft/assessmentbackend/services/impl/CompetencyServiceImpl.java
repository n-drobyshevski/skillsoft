package app.skillsoft.assessmentbackend.services.impl;

import app.skillsoft.assessmentbackend.config.CacheConfig;
import app.skillsoft.assessmentbackend.domain.entities.Competency;
import app.skillsoft.assessmentbackend.repository.CompetencyRepository;
import app.skillsoft.assessmentbackend.services.CompetencyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CompetencyServiceImpl implements CompetencyService {

    private static final Logger logger = LoggerFactory.getLogger(CompetencyServiceImpl.class);

    private final CompetencyRepository competencyRepository;

    @Autowired
    public CompetencyServiceImpl(CompetencyRepository competencyRepository) {
        this.competencyRepository = competencyRepository;
    }

    @Override
    @Cacheable(CacheConfig.COMPETENCIES_CACHE)
    public List<Competency> listCompetencies() {
        return competencyRepository.findAll();
    }

    @Override
    @Transactional
    @CacheEvict(value = CacheConfig.COMPETENCIES_CACHE, allEntries = true)
    public Competency createCompetency(Competency competency) {
        if( null != competency.getId() ) {
            throw new IllegalArgumentException("New competency cannot already have an ID");
        }
        if(null == competency.getName() || competency.getName().isBlank()) {
            throw new IllegalArgumentException("Competency name is required");

        }

        // Set created and modified timestamps
        LocalDateTime now = LocalDateTime.now();
        competency.setCreatedAt(now);
        competency.setLastModified(now);

        // Set initial version
        competency.setVersion(1);
        Competency competencyToSave = new Competency(
                null,
                competency.getName(),
                competency.getDescription(),
                competency.getCategory(),
                competency.getStandardCodes(),
                competency.isActive(),
                competency.getApprovalStatus(),
                null,
                competency.getVersion(),
                competency.getCreatedAt(),
                competency.getLastModified()
        );
        return competencyRepository.save(competencyToSave);
    }

    @Override
    public Optional<Competency> findCompetencyById(UUID id) {
        return competencyRepository.findById(id);
    }

    @Override
    @Transactional
    @CacheEvict(value = CacheConfig.COMPETENCIES_CACHE, allEntries = true)
    public Competency updateCompetency(UUID id, Competency competencyDetails) {
        return competencyRepository.findById(id)
                .map(existingCompetency -> {
                    logger.debug("Updating competency {}, current standardCodes: {}", 
                        id, existingCompetency.getStandardCodes());
                    logger.debug("New standardCodes: {}", competencyDetails.getStandardCodes());
                    
                    // Update fields
                    existingCompetency.setName(competencyDetails.getName());
                    existingCompetency.setDescription(competencyDetails.getDescription());
                    existingCompetency.setCategory(competencyDetails.getCategory());
                    existingCompetency.setStandardCodes(competencyDetails.getStandardCodes());
                    existingCompetency.setActive(competencyDetails.isActive());
                    existingCompetency.setApprovalStatus(competencyDetails.getApprovalStatus());

                    // Increment version
                    existingCompetency.setVersion(existingCompetency.getVersion() + 1);

                    // Update last modified timestamp
                    existingCompetency.setLastModified(LocalDateTime.now());

                    // Save and flush to ensure the update is persisted immediately
                    Competency saved = competencyRepository.saveAndFlush(existingCompetency);
                    
                    logger.debug("Saved competency {}, standardCodes after save: {}", 
                        saved.getId(), saved.getStandardCodes());
                    
                    return saved;
                })
                .orElseThrow(() -> new RuntimeException("Competency not found with id: " + id));
    }

    @Override
    @Transactional
    @CacheEvict(value = CacheConfig.COMPETENCIES_CACHE, allEntries = true)
    public boolean deleteCompetency(UUID id) {
        if (competencyRepository.existsById(id)) {
            competencyRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
