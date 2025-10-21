package app.skillsoft.assessmentbackend.services.impl;

import app.skillsoft.assessmentbackend.domain.entities.Competency;
import app.skillsoft.assessmentbackend.repository.CompetencyRepository;
import app.skillsoft.assessmentbackend.services.CompetencyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CompetencyServiceImpl implements CompetencyService {

    private final CompetencyRepository competencyRepository;

    @Autowired
    public CompetencyServiceImpl(CompetencyRepository competencyRepository) {
        this.competencyRepository = competencyRepository;
    }

    @Override
    public List<Competency> listCompetencies() {
        return competencyRepository.findAll();
    }

    @Override
    @Transactional
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
                competency.getLevel(),
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
    public Competency updateCompetency(UUID id, Competency competencyDetails) {
        return competencyRepository.findById(id)
                .map(existingCompetency -> {
                    // Update fields
                    existingCompetency.setName(competencyDetails.getName());
                    existingCompetency.setDescription(competencyDetails.getDescription());
                    existingCompetency.setCategory(competencyDetails.getCategory());
                    existingCompetency.setLevel(competencyDetails.getLevel());
                    existingCompetency.setStandardCodes(competencyDetails.getStandardCodes());
                    existingCompetency.setActive(competencyDetails.isActive());
                    existingCompetency.setApprovalStatus(competencyDetails.getApprovalStatus());

                    // Increment version
                    existingCompetency.setVersion(existingCompetency.getVersion() + 1);

                    // Update last modified timestamp
                    existingCompetency.setLastModified(LocalDateTime.now());

                    return competencyRepository.save(existingCompetency);
                })
                .orElseThrow(() -> new RuntimeException("Competency not found with id: " + id));
    }

    @Override
    @Transactional
    public void deleteCompetency(UUID id) {
        competencyRepository.deleteById(id);
    }
}
