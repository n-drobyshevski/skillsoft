package app.skillsoft.assessmentbackend.services;

import app.skillsoft.assessmentbackend.domain.entities.Competency;

import java.util.List;

public interface CompetencyService {
    List<Competency> listCompetencies();
}
