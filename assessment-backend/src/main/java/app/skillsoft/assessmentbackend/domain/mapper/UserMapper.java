package app.skillsoft.assessmentbackend.domain.mapper;

import app.skillsoft.assessmentbackend.domain.dto.UserDto;
import app.skillsoft.assessmentbackend.domain.entities.User;

/**
 * Mapper interface for converting between User entity and UserDto.
 */
public interface UserMapper {
    
    /**
     * Convert UserDto to User entity.
     * @param dto UserDto to convert
     * @return User entity
     */
    User fromDto(UserDto dto);
    
    /**
     * Convert User entity to UserDto.
     * @param entity User entity to convert
     * @return UserDto
     */
    UserDto toDto(User entity);
}