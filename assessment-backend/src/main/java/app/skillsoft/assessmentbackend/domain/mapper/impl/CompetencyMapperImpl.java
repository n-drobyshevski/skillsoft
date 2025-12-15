package app.skillsoft.assessmentbackend.domain.mapper.impl;

import app.skillsoft.assessmentbackend.domain.dto.BehavioralIndicatorDto;
import app.skillsoft.assessmentbackend.domain.dto.CompetencyDto;
import app.skillsoft.assessmentbackend.domain.entities.Competency;
import app.skillsoft.assessmentbackend.domain.mapper.BehavioralIndicatorMapper;
import app.skillsoft.assessmentbackend.domain.mapper.CompetencyMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class CompetencyMapperImpl implements CompetencyMapper {

    private final BehavioralIndicatorMapper behavioralIndicatorMapper;

    @Autowired
    public CompetencyMapperImpl(BehavioralIndicatorMapper behavioralIndicatorMapper) {
        this.behavioralIndicatorMapper = behavioralIndicatorMapper;
    }

    @Override
    public Competency fromDto(CompetencyDto dto) {
        if (dto == null) {
            return null;
        }

        Competency competency = new Competency();
        competency.setId(dto.id());
        competency.setName(dto.name());
        competency.setDescription(dto.description());
        competency.setCategory(dto.category());
        competency.setStandardCodes(dto.standardCodes());
        competency.setActive(dto.isActive());
        competency.setApprovalStatus(dto.approvalStatus());
        competency.setVersion(dto.version());
        competency.setCreatedAt(dto.createdAt());
        competency.setLastModified(dto.lastModified());

        // We don't set behavioral indicators here to avoid circular dependency
        // They should be handled separately

        return competency;
    }

    @Override
    public CompetencyDto toDto(Competency entity) {
        if (entity == null) {
            return null;
        }

        List<BehavioralIndicatorDto> behavioralIndicatorDtos = entity.getBehavioralIndicators() != null ?
                entity.getBehavioralIndicators().stream()
                        .map(behavioralIndicatorMapper::toDto)
                        .collect(Collectors.toList()) :
                Collections.emptyList();

        return new CompetencyDto(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getCategory(),
                entity.getStandardCodes(),
                entity.isActive(),
                entity.getApprovalStatus(),
                behavioralIndicatorDtos,
                entity.getVersion(),
                entity.getCreatedAt(),
                entity.getLastModified()
        );
    }
}
