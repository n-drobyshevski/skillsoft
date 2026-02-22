package app.skillsoft.assessmentbackend.services.external.impl;

import app.skillsoft.assessmentbackend.config.PassportProperties;
import app.skillsoft.assessmentbackend.domain.entities.CompetencyPassportEntity;
import app.skillsoft.assessmentbackend.domain.entities.User;
import app.skillsoft.assessmentbackend.repository.CompetencyPassportRepository;
import app.skillsoft.assessmentbackend.repository.UserRepository;
import app.skillsoft.assessmentbackend.services.external.PassportService.CompetencyPassport;
import app.skillsoft.assessmentbackend.testutil.PassportTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the database-backed PassportServiceImpl.
 */
@DisplayName("PassportServiceImpl Unit Tests")
@ExtendWith(MockitoExtension.class)
class PassportServiceImplTest {

    @Mock
    private CompetencyPassportRepository passportRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PassportProperties passportProperties;

    @InjectMocks
    private PassportServiceImpl service;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(PassportTestFixtures.CANDIDATE_ID);
        testUser.setClerkId(PassportTestFixtures.CLERK_USER_ID);
    }

    // ---- getPassportByClerkUserId ----

    @Nested
    @DisplayName("getPassportByClerkUserId")
    class GetPassportByClerkUserId {

        @Test
        @DisplayName("should return passport when valid entity exists")
        void shouldReturnPassportWhenValidEntityExists() {
            // Given
            CompetencyPassportEntity entity = PassportTestFixtures.createEntity();
            when(passportRepository.findValidByClerkUserId(eq(PassportTestFixtures.CLERK_USER_ID), any(LocalDateTime.class)))
                .thenReturn(Optional.of(entity));
            when(userRepository.findByClerkId(PassportTestFixtures.CLERK_USER_ID))
                .thenReturn(Optional.of(testUser));

            // When
            Optional<CompetencyPassport> result = service.getPassportByClerkUserId(PassportTestFixtures.CLERK_USER_ID);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().candidateId()).isEqualTo(PassportTestFixtures.CANDIDATE_ID);
            assertThat(result.get().competencyScores()).hasSize(3);
            assertThat(result.get().isValid()).isTrue();
        }

        @Test
        @DisplayName("should return empty when no valid entity exists")
        void shouldReturnEmptyWhenNoValidEntityExists() {
            // Given
            when(passportRepository.findValidByClerkUserId(eq(PassportTestFixtures.CLERK_USER_ID), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

            // When
            Optional<CompetencyPassport> result = service.getPassportByClerkUserId(PassportTestFixtures.CLERK_USER_ID);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty for null clerkUserId")
        void shouldReturnEmptyForNullClerkUserId() {
            // When
            Optional<CompetencyPassport> result = service.getPassportByClerkUserId(null);

            // Then
            assertThat(result).isEmpty();
            verifyNoInteractions(passportRepository);
        }

        @Test
        @DisplayName("should return empty for blank clerkUserId")
        void shouldReturnEmptyForBlankClerkUserId() {
            // When
            Optional<CompetencyPassport> result = service.getPassportByClerkUserId("  ");

            // Then
            assertThat(result).isEmpty();
            verifyNoInteractions(passportRepository);
        }

        @Test
        @DisplayName("should return passport with null candidateId when user not found")
        void shouldReturnPassportWithNullCandidateIdWhenUserNotFound() {
            // Given
            CompetencyPassportEntity entity = PassportTestFixtures.createEntity();
            when(passportRepository.findValidByClerkUserId(eq(PassportTestFixtures.CLERK_USER_ID), any(LocalDateTime.class)))
                .thenReturn(Optional.of(entity));
            when(userRepository.findByClerkId(PassportTestFixtures.CLERK_USER_ID))
                .thenReturn(Optional.empty());

            // When
            Optional<CompetencyPassport> result = service.getPassportByClerkUserId(PassportTestFixtures.CLERK_USER_ID);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().candidateId()).isNull();
        }
    }

    // ---- getPassport (by UUID) ----

    @Nested
    @DisplayName("getPassport")
    class GetPassport {

        @Test
        @DisplayName("should return passport when user and valid entity exist")
        void shouldReturnPassportWhenUserAndValidEntityExist() {
            // Given
            CompetencyPassportEntity entity = PassportTestFixtures.createEntity();
            when(userRepository.findById(PassportTestFixtures.CANDIDATE_ID))
                .thenReturn(Optional.of(testUser));
            when(passportRepository.findValidByClerkUserId(eq(PassportTestFixtures.CLERK_USER_ID), any(LocalDateTime.class)))
                .thenReturn(Optional.of(entity));

            // When
            Optional<CompetencyPassport> result = service.getPassport(PassportTestFixtures.CANDIDATE_ID);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().candidateId()).isEqualTo(PassportTestFixtures.CANDIDATE_ID);
        }

        @Test
        @DisplayName("should return empty when user not found")
        void shouldReturnEmptyWhenUserNotFound() {
            // Given
            when(userRepository.findById(PassportTestFixtures.CANDIDATE_ID))
                .thenReturn(Optional.empty());

            // When
            Optional<CompetencyPassport> result = service.getPassport(PassportTestFixtures.CANDIDATE_ID);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty when passport expired")
        void shouldReturnEmptyWhenPassportExpired() {
            // Given
            when(userRepository.findById(PassportTestFixtures.CANDIDATE_ID))
                .thenReturn(Optional.of(testUser));
            when(passportRepository.findValidByClerkUserId(eq(PassportTestFixtures.CLERK_USER_ID), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

            // When
            Optional<CompetencyPassport> result = service.getPassport(PassportTestFixtures.CANDIDATE_ID);

            // Then
            assertThat(result).isEmpty();
        }
    }

    // ---- hasValidPassport ----

    @Nested
    @DisplayName("hasValidPassport")
    class HasValidPassport {

        @Test
        @DisplayName("should return true when valid passport exists")
        void shouldReturnTrueWhenValidPassportExists() {
            // Given
            when(userRepository.findById(PassportTestFixtures.CANDIDATE_ID))
                .thenReturn(Optional.of(testUser));
            when(passportRepository.existsValidByClerkUserId(eq(PassportTestFixtures.CLERK_USER_ID), any(LocalDateTime.class)))
                .thenReturn(true);

            // When
            boolean result = service.hasValidPassport(PassportTestFixtures.CANDIDATE_ID);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false when no valid passport")
        void shouldReturnFalseWhenNoValidPassport() {
            // Given
            when(userRepository.findById(PassportTestFixtures.CANDIDATE_ID))
                .thenReturn(Optional.of(testUser));
            when(passportRepository.existsValidByClerkUserId(eq(PassportTestFixtures.CLERK_USER_ID), any(LocalDateTime.class)))
                .thenReturn(false);

            // When
            boolean result = service.hasValidPassport(PassportTestFixtures.CANDIDATE_ID);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return false when user not found")
        void shouldReturnFalseWhenUserNotFound() {
            // Given
            when(userRepository.findById(PassportTestFixtures.CANDIDATE_ID))
                .thenReturn(Optional.empty());

            // When
            boolean result = service.hasValidPassport(PassportTestFixtures.CANDIDATE_ID);

            // Then
            assertThat(result).isFalse();
        }
    }

    // ---- hasValidPassportByClerkUserId ----

    @Nested
    @DisplayName("hasValidPassportByClerkUserId")
    class HasValidPassportByClerkUserId {

        @Test
        @DisplayName("should return true when valid passport exists")
        void shouldReturnTrueWhenValid() {
            // Given
            when(passportRepository.existsValidByClerkUserId(eq(PassportTestFixtures.CLERK_USER_ID), any(LocalDateTime.class)))
                .thenReturn(true);

            // When
            boolean result = service.hasValidPassportByClerkUserId(PassportTestFixtures.CLERK_USER_ID);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false for null clerkUserId")
        void shouldReturnFalseForNull() {
            // When
            boolean result = service.hasValidPassportByClerkUserId(null);

            // Then
            assertThat(result).isFalse();
            verifyNoInteractions(passportRepository);
        }
    }

    // ---- savePassport ----

    @Nested
    @DisplayName("savePassport")
    class SavePassport {

        @Test
        @DisplayName("should create new entity when none exists")
        void shouldCreateNewEntityWhenNoneExists() {
            // Given
            when(userRepository.findById(PassportTestFixtures.CANDIDATE_ID))
                .thenReturn(Optional.of(testUser));
            when(passportRepository.findByClerkUserId(PassportTestFixtures.CLERK_USER_ID))
                .thenReturn(Optional.empty());
            when(passportProperties.getValidityDays()).thenReturn(180);

            CompetencyPassport passport = PassportTestFixtures.createPassport();

            // When
            service.savePassport(passport);

            // Then
            ArgumentCaptor<CompetencyPassportEntity> captor = ArgumentCaptor.forClass(CompetencyPassportEntity.class);
            verify(passportRepository).save(captor.capture());

            CompetencyPassportEntity saved = captor.getValue();
            assertThat(saved.getClerkUserId()).isEqualTo(PassportTestFixtures.CLERK_USER_ID);
            assertThat(saved.getCompetencyScores()).hasSize(3);
            assertThat(saved.getExpiresAt()).isAfter(LocalDateTime.now().plusDays(179));
        }

        @Test
        @DisplayName("should update existing entity on upsert")
        void shouldUpdateExistingEntityOnUpsert() {
            // Given
            CompetencyPassportEntity existing = PassportTestFixtures.createEntity();
            when(userRepository.findById(PassportTestFixtures.CANDIDATE_ID))
                .thenReturn(Optional.of(testUser));
            when(passportRepository.findByClerkUserId(PassportTestFixtures.CLERK_USER_ID))
                .thenReturn(Optional.of(existing));
            when(passportProperties.getValidityDays()).thenReturn(180);

            Map<UUID, Double> newScores = Map.of(PassportTestFixtures.COMPETENCY_1, 5.0);
            CompetencyPassport passport = new CompetencyPassport(
                PassportTestFixtures.CANDIDATE_ID, newScores, Map.of(), LocalDateTime.now(), true);

            // When
            service.savePassport(passport);

            // Then
            ArgumentCaptor<CompetencyPassportEntity> captor = ArgumentCaptor.forClass(CompetencyPassportEntity.class);
            verify(passportRepository).save(captor.capture());

            CompetencyPassportEntity saved = captor.getValue();
            assertThat(saved.getId()).isEqualTo(existing.getId());
            assertThat(saved.getCompetencyScores()).hasSize(1);
        }

        @Test
        @DisplayName("should throw when user not found for candidateId")
        void shouldThrowWhenUserNotFound() {
            // Given
            when(userRepository.findById(PassportTestFixtures.CANDIDATE_ID))
                .thenReturn(Optional.empty());

            CompetencyPassport passport = PassportTestFixtures.createPassport();

            // When / Then
            assertThatThrownBy(() -> service.savePassport(passport))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No User found for candidateId");
        }

        @Test
        @DisplayName("should convert UUID keys to String keys for JSONB storage")
        void shouldConvertUuidKeysToStringKeys() {
            // Given
            when(userRepository.findById(PassportTestFixtures.CANDIDATE_ID))
                .thenReturn(Optional.of(testUser));
            when(passportRepository.findByClerkUserId(PassportTestFixtures.CLERK_USER_ID))
                .thenReturn(Optional.empty());
            when(passportProperties.getValidityDays()).thenReturn(180);

            CompetencyPassport passport = PassportTestFixtures.createPassport();

            // When
            service.savePassport(passport);

            // Then
            ArgumentCaptor<CompetencyPassportEntity> captor = ArgumentCaptor.forClass(CompetencyPassportEntity.class);
            verify(passportRepository).save(captor.capture());

            Map<String, Double> stored = captor.getValue().getCompetencyScores();
            assertThat(stored).containsKey(PassportTestFixtures.COMPETENCY_1.toString());
            assertThat(stored).containsKey(PassportTestFixtures.COMPETENCY_2.toString());
            assertThat(stored).containsKey(PassportTestFixtures.COMPETENCY_3.toString());
        }
    }

    // ---- savePassportForClerkUser ----

    @Nested
    @DisplayName("savePassportForClerkUser")
    class SavePassportForClerkUser {

        @Test
        @DisplayName("should save with source result ID")
        void shouldSaveWithSourceResultId() {
            // Given
            when(passportRepository.findByClerkUserId(PassportTestFixtures.CLERK_USER_ID))
                .thenReturn(Optional.empty());
            when(passportProperties.getValidityDays()).thenReturn(90);

            // When
            service.savePassportForClerkUser(
                PassportTestFixtures.CLERK_USER_ID,
                PassportTestFixtures.DEFAULT_SCORES,
                PassportTestFixtures.DEFAULT_BIG_FIVE,
                PassportTestFixtures.SOURCE_RESULT_ID
            );

            // Then
            ArgumentCaptor<CompetencyPassportEntity> captor = ArgumentCaptor.forClass(CompetencyPassportEntity.class);
            verify(passportRepository).save(captor.capture());

            CompetencyPassportEntity saved = captor.getValue();
            assertThat(saved.getSourceResultId()).isEqualTo(PassportTestFixtures.SOURCE_RESULT_ID);
            assertThat(saved.getExpiresAt()).isAfter(LocalDateTime.now().plusDays(89));
            assertThat(saved.getExpiresAt()).isBefore(LocalDateTime.now().plusDays(91));
        }
    }

    // ---- Score conversion (toRecord) ----

    @Nested
    @DisplayName("Score Conversion")
    class ScoreConversion {

        @Test
        @DisplayName("should convert String keys back to UUID keys in toRecord")
        void shouldConvertStringKeysBackToUuidKeys() {
            // Given
            CompetencyPassportEntity entity = PassportTestFixtures.createEntity();
            when(userRepository.findById(PassportTestFixtures.CANDIDATE_ID))
                .thenReturn(Optional.of(testUser));
            when(passportRepository.findValidByClerkUserId(eq(PassportTestFixtures.CLERK_USER_ID), any(LocalDateTime.class)))
                .thenReturn(Optional.of(entity));

            // When
            Optional<CompetencyPassport> result = service.getPassport(PassportTestFixtures.CANDIDATE_ID);

            // Then
            assertThat(result).isPresent();
            Map<UUID, Double> scores = result.get().competencyScores();
            assertThat(scores).containsKey(PassportTestFixtures.COMPETENCY_1);
            assertThat(scores).containsKey(PassportTestFixtures.COMPETENCY_2);
            assertThat(scores).containsKey(PassportTestFixtures.COMPETENCY_3);
            assertThat(scores.get(PassportTestFixtures.COMPETENCY_1)).isEqualTo(4.2);
        }

        @Test
        @DisplayName("should return empty bigFiveProfile map when entity has null")
        void shouldReturnEmptyBigFiveWhenNull() {
            // Given
            CompetencyPassportEntity entity = PassportTestFixtures.createEntity();
            entity.setBigFiveProfile(null);
            when(userRepository.findById(PassportTestFixtures.CANDIDATE_ID))
                .thenReturn(Optional.of(testUser));
            when(passportRepository.findValidByClerkUserId(eq(PassportTestFixtures.CLERK_USER_ID), any(LocalDateTime.class)))
                .thenReturn(Optional.of(entity));

            // When
            Optional<CompetencyPassport> result = service.getPassport(PassportTestFixtures.CANDIDATE_ID);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().bigFiveProfile()).isEmpty();
        }

        @Test
        @DisplayName("should preserve Big Five profile data in round-trip")
        void shouldPreserveBigFiveProfileInRoundTrip() {
            // Given
            CompetencyPassportEntity entity = PassportTestFixtures.createEntity();
            when(userRepository.findById(PassportTestFixtures.CANDIDATE_ID))
                .thenReturn(Optional.of(testUser));
            when(passportRepository.findValidByClerkUserId(eq(PassportTestFixtures.CLERK_USER_ID), any(LocalDateTime.class)))
                .thenReturn(Optional.of(entity));

            // When
            Optional<CompetencyPassport> result = service.getPassport(PassportTestFixtures.CANDIDATE_ID);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().bigFiveProfile())
                .containsEntry("OPENNESS", 3.5)
                .containsEntry("CONSCIENTIOUSNESS", 4.0)
                .containsEntry("NEUROTICISM", 2.5);
        }
    }
}
