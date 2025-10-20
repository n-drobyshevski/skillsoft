package app.skillsoft.assessmentbackend.repository;


import app.skillsoft.assessmentbackend.domain.entities.AssessmentQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AssessmentQuestionRepository extends JpaRepository<AssessmentQuestion, UUID> {

    public List<AssessmentQuestion> findByBehavioralIndicatorId(UUID behavioralIndicatorId);
}
