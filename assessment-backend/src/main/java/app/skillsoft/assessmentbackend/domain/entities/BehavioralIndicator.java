package app.skillsoft.assessmentbackend.domain.entities;


import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.util.UUID;

@Entity
@Table(name = "behavioral_indicators",
       uniqueConstraints = {
           @UniqueConstraint(name = "uc_behavioralindicator_competency_title",
                             columnNames = {"competency_id", "title"})
       }
)
public class BehavioralIndicator {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "competency_id", nullable = false)
    private Competency competency;

    @Column(name ="title", nullable = false)
    private String title;

    @Column(name ="description", length = 1000)
    private String description;

    @Column(name="observability_level", nullable = false)
    @Enumerated(EnumType.STRING)
    private ProficiencyLevel observabilityLevel;

    @Column(name="measurement_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private IndicatorMeasurementType measurementType;

    @Column(name="weight", nullable = false)
    private float weight;

    @Column(name="examples", length = 1000)
    private String examples;

    @Column(name="counter_examples", length = 500)
    private String counterExamples;

    @Column(name="is_active", nullable = false)
    private boolean isActive;

    @Column(name = "approval_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private ApprovalStatus approvalStatus;

    @Column(name = "order_index", nullable = false)
    @Min(value = 1, message = "Order index must be positive")
    @Max(value = 20, message = "Maximum 20 indicators per competency")
    private Integer orderIndex;

    public BehavioralIndicator() {

    }

    @Override
    public String toString() {
        return "BehavioralIndicator{" +
                "id=" + id +
                ", competency=" + competency +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", observabilityLevel=" + observabilityLevel +
                ", measurementType=" + measurementType +
                ", weight=" + weight +
                ", examples='" + examples + '\'' +
                ", counterExamples='" + counterExamples + '\'' +
                ", isActive=" + isActive +
                ", approvalStatus=" + approvalStatus +
                ", orderIndex=" + orderIndex +
                '}';
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setCompetency(Competency competency) {
        this.competency = competency;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setObservabilityLevel(ProficiencyLevel observabilityLevel) {
        this.observabilityLevel = observabilityLevel;
    }

    public void setMeasurementType(IndicatorMeasurementType measurementType) {
        this.measurementType = measurementType;
    }

    public void setWeight(float weight) {
        this.weight = weight;
    }

    public void setExamples(String examples) {
        this.examples = examples;
    }

    public void setCounterExamples(String counterExamples) {
        this.counterExamples = counterExamples;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public void setApprovalStatus(ApprovalStatus approvalStatus) {
        this.approvalStatus = approvalStatus;
    }

    public void setOrderIndex(Integer orderIndex) {
        this.orderIndex = orderIndex;
    }

    public Competency getCompetency() {
        return competency;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public ProficiencyLevel getObservabilityLevel() {
        return observabilityLevel;
    }

    public IndicatorMeasurementType getMeasurementType() {
        return measurementType;
    }

    public float getWeight() {
        return weight;
    }

    public String getExamples() {
        return examples;
    }

    public String getCounterExamples() {
        return counterExamples;
    }

    public boolean isActive() {
        return isActive;
    }

    public ApprovalStatus getApprovalStatus() {
        return approvalStatus;
    }

    public Integer getOrderIndex() {
        return orderIndex;
    }

    public BehavioralIndicator(UUID id, Competency competency, String title, String description, ProficiencyLevel observabilityLevel, IndicatorMeasurementType measurementType, float weight, String examples, String counterExamples, boolean isActive, ApprovalStatus approvalStatus, Integer orderIndex) {
        this.id = id;
        this.competency = competency;
        this.title = title;
        this.description = description;
        this.observabilityLevel = observabilityLevel;
        this.measurementType = measurementType;
        this.weight = weight;
        this.examples = examples;
        this.counterExamples = counterExamples;
        this.isActive = isActive;
        this.approvalStatus = approvalStatus;
        this.orderIndex = orderIndex;
    }
}
