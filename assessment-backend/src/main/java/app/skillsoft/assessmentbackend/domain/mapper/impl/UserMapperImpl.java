package app.skillsoft.assessmentbackend.domain.mapper.impl;

import app.skillsoft.assessmentbackend.domain.dto.UserDto;
import app.skillsoft.assessmentbackend.domain.entities.User;
import app.skillsoft.assessmentbackend.domain.mapper.UserMapper;
import org.springframework.stereotype.Component;

/**
 * Implementation of UserMapper for converting between User entity and UserDto.
 */
@Component
public class UserMapperImpl implements UserMapper {

    @Override
    public User fromDto(UserDto dto) {
        if (dto == null) {
            return null;
        }

        User user = new User();
        user.setId(dto.id());
        user.setClerkId(dto.clerkId());
        user.setEmail(dto.email());
        user.setUsername(dto.username());
        user.setFirstName(dto.firstName());
        user.setLastName(dto.lastName());
        user.setImageUrl(dto.imageUrl());
        user.setHasImage(dto.hasImage());
        user.setRole(dto.role());
        user.setActive(dto.isActive());
        user.setBanned(dto.banned());
        user.setLocked(dto.locked());
        user.setPreferences(dto.preferences());
        user.setCreatedAt(dto.createdAt());
        user.setUpdatedAt(dto.updatedAt());
        user.setLastLogin(dto.lastLogin());
        user.setClerkCreatedAt(dto.clerkCreatedAt());
        user.setLastSignInAt(dto.lastSignInAt());

        return user;
    }

    @Override
    public UserDto toDto(User entity) {
        if (entity == null) {
            return null;
        }

        return new UserDto(
                entity.getId(),
                entity.getClerkId(),
                entity.getEmail(),
                entity.getUsername(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getImageUrl(),
                entity.isHasImage(),
                entity.getRole(),
                entity.isActive(),
                entity.isBanned(),
                entity.isLocked(),
                entity.getPreferences(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getLastLogin(),
                entity.getClerkCreatedAt(),
                entity.getLastSignInAt()
        );
    }
}