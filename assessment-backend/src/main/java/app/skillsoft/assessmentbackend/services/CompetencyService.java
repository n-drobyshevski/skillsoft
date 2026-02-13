package app.skillsoft.assessmentbackend.services;

import app.skillsoft.assessmentbackend.domain.entities.Competency;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CompetencyService {
    List<Competency> listCompetencies();

    Competency createCompetency(Competency competency);

    Optional<Competency> findCompetencyById(UUID id);

    Competency updateCompetency(UUID id, Competency competency);

    boolean deleteCompetency(UUID id);
}
