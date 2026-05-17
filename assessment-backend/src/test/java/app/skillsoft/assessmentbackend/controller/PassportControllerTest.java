package app.skillsoft.assessmentbackend.controller;

import app.skillsoft.assessmentbackend.services.external.PassportService;
import app.skillsoft.assessmentbackend.services.external.PassportService.CompetencyPassport;
import app.skillsoft.assessmentbackend.services.external.PassportService.PassportDetails;
import app.skillsoft.assessmentbackend.testutil.PassportTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for PassportController using @WebMvcTest.
 *
 * Method-level security (@PreAuthorize) is NOT enforced in this slice because
 * SecurityConfig is excluded by @Profile("!test") and TestSecurityConfig does
 * not enable @EnableMethodSecurity. @WithMockUser provides a valid authentication
 * principal so the filter chain does not reject requests.
 *
 * Tests cover:
 * - GET /api/v1/passports/user/{clerkUserId}  — 200 with DTO, 404, score conversion
 * - GET /api/v1/passports/user/{clerkUserId}/valid — valid and invalid passport flags
 */
@WebMvcTest(PassportController.class)
@DisplayName("Passport Controller Tests")
@WithMockUser
class PassportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PassportService passportService;

    private static final String CLERK_USER_ID = PassportTestFixtures.CLERK_USER_ID;
    private static final String BASE_URL = "/api/v1/passports/user/" + CLERK_USER_ID;

    private static final UUID ENTITY_ID = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");

    private PassportDetails buildDefaultDetails() {
        CompetencyPassport passport = PassportTestFixtures.createPassport();
        return new PassportDetails(
                ENTITY_ID,
                passport,
                CLERK_USER_ID,
                LocalDateTime.now().plusDays(180)
        );
    }

    @Nested
    @DisplayName("GET /api/v1/passports/user/{clerkUserId} — Retrieve Passport")
    class GetPassportTests {

        @Test
        @DisplayName("Returns 200 with mapped DTO when passport exists")
        void getPassport_whenExists_returns200WithDto() throws Exception {
            // Given
            PassportDetails details = buildDefaultDetails();
            when(passportService.getPassportDetailsByClerkUserId(CLERK_USER_ID))
                    .thenReturn(Optional.of(details));

            // When & Then
            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(ENTITY_ID.toString()))
                    .andExpect(jsonPath("$.candidateId").value(PassportTestFixtures.CANDIDATE_ID.toString()))
                    .andExpect(jsonPath("$.clerkUserId").value(CLERK_USER_ID))
                    .andExpect(jsonPath("$.scores").isMap())
                    .andExpect(jsonPath("$.bigFiveProfile").isMap())
                    // CompetencyPassportDto record declares 'boolean isValid',
                    // so Jackson serializes it as "isValid", not "valid"
                    .andExpect(jsonPath("$.isValid").value(true));

            verify(passportService).getPassportDetailsByClerkUserId(CLERK_USER_ID);
        }

        @Test
        @DisplayName("Returns 404 when no passport exists for the user")
        void getPassport_whenNotExists_returns404() throws Exception {
            // Given
            when(passportService.getPassportDetailsByClerkUserId(CLERK_USER_ID))
                    .thenReturn(Optional.empty());

            // When & Then
            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isNotFound());

            verify(passportService).getPassportDetailsByClerkUserId(CLERK_USER_ID);
        }

        @Test
        @DisplayName("Scores in response are on 0-100 scale, not the raw 1-5 backend scale")
        void getPassport_verifiesScoreConversion() throws Exception {
            // Given — COMPETENCY_1 raw score = 4.2  →  expected percentage = 80.0
            //         COMPETENCY_2 raw score = 3.5  →  expected percentage = 62.5
            //         COMPETENCY_3 raw score = 2.8  →  expected percentage = 45.0
            PassportDetails details = buildDefaultDetails();
            when(passportService.getPassportDetailsByClerkUserId(CLERK_USER_ID))
                    .thenReturn(Optional.of(details));

            String comp1Path = "$.scores['" + PassportTestFixtures.COMPETENCY_1.toString() + "']";
            String comp2Path = "$.scores['" + PassportTestFixtures.COMPETENCY_2.toString() + "']";
            String comp3Path = "$.scores['" + PassportTestFixtures.COMPETENCY_3.toString() + "']";

            // When & Then
            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath(comp1Path).value(80.0))
                    .andExpect(jsonPath(comp2Path).value(62.5))
                    .andExpect(jsonPath(comp3Path).value(45.0));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/passports/user/{clerkUserId}/valid — Validity Check")
    class HasValidPassportTests {

        private static final String VALID_URL = BASE_URL + "/valid";

        @Test
        @DisplayName("Returns { valid: true } when user has a valid passport")
        void hasValidPassport_whenValid_returnsTrue() throws Exception {
            // Given
            when(passportService.hasValidPassportByClerkUserId(CLERK_USER_ID)).thenReturn(true);

            // When & Then
            mockMvc.perform(get(VALID_URL))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.valid").value(true));

            verify(passportService).hasValidPassportByClerkUserId(CLERK_USER_ID);
        }

        @Test
        @DisplayName("Returns { valid: false } when user has no valid passport")
        void hasValidPassport_whenInvalid_returnsFalse() throws Exception {
            // Given
            when(passportService.hasValidPassportByClerkUserId(CLERK_USER_ID)).thenReturn(false);

            // When & Then
            mockMvc.perform(get(VALID_URL))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.valid").value(false));

            verify(passportService).hasValidPassportByClerkUserId(CLERK_USER_ID);
        }
    }
}
