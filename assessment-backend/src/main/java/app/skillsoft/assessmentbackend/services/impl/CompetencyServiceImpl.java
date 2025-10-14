package app.skillsoft.assessmentbackend.services.impl;

import app.skillsoft.assessmentbackend.domain.entities.Competency;
import app.skillsoft.assessmentbackend.repository.CompetencyRepository;
import app.skillsoft.assessmentbackend.services.CompetencyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

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
}
