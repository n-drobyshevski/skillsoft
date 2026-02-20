package app.skillsoft.assessmentbackend.controller;

import app.skillsoft.assessmentbackend.domain.entities.UserRole;
import app.skillsoft.assessmentbackend.services.UserService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.svix.Webhook;
import com.svix.exceptions.WebhookVerificationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.PostConstruct;
import java.net.http.HttpHeaders;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for handling Clerk webhooks.
 * Syncs user data from Clerk to the local database.
 * 
 * Supported webhook events:
 * - user.created: Creates a new user in the database
 * - user.updated: Updates existing user information
 * - user.deleted: Permanently deletes user from the database
 * 
 * Security:
 * - Uses Svix library to verify webhook signatures
 * - Requires CLERK_WEBHOOK_SECRET environment variable
 * 
 * @see <a href="https://clerk.com/docs/integrations/webhooks/overview">Clerk Webhooks Documentation</a>
 */
@RestController
@RequestMapping("/api/webhooks/clerk")
public class ClerkWebhookController {

    private static final Logger logger = LoggerFactory.getLogger(ClerkWebhookController.class);

    private final UserService userService;
    private final ObjectMapper objectMapper;
    
    @Value("${clerk.webhook.secret}")
    private String webhookSecret;

    public ClerkWebhookController(UserService userService, ObjectMapper objectMapper) {
        this.userService = userService;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    void validateWebhookSecret() {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            throw new IllegalStateException(
                "CLERK_WEBHOOK_SECRET must be configured. Set the environment variable or clerk.webhook.secret property.");
        }
    }

    /**
     * Handle incoming Clerk webhooks.
     * 
     * Webhook Signature Verification Process:
     * 1. Extract three headers from the request:
     *    - svix-id: Unique message identifier
     *    - svix-timestamp: Unix timestamp when message was sent
     *    - svix-signature: HMAC signature to verify authenticity
     * 
     * 2. Svix Webhook class verifies the signature using:
     *    - The webhook secret (from CLERK_WEBHOOK_SECRET env variable)
     *    - The raw request payload
     *    - The three headers mentioned above
     * 
     * 3. If verification fails, it throws WebhookVerificationException
     *    This prevents malicious requests from being processed
     * 
     * @param payload Raw webhook payload from Clerk
     * @param svixId Webhook message ID
     * @param svixTimestamp Webhook timestamp
     * @param svixSignature Webhook signature for verification
     * @return ResponseEntity with appropriate status code
     */
    @PostMapping
    public ResponseEntity<?> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("svix-id") String svixId,
            @RequestHeader("svix-timestamp") String svixTimestamp,
            @RequestHeader("svix-signature") String svixSignature) {

        // Check if webhook secret is configured
        if (webhookSecret == null || webhookSecret.isEmpty()) {
            logger.error("CLERK_WEBHOOK_SECRET is not configured");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Webhook secret not configured"));
        }

        try {
            // STEP 1: Verify webhook signature using Svix
            // This ensures the request actually came from Clerk and hasn't been tampered with
            Webhook webhook = new Webhook(webhookSecret);
            
            // The verify method will:
            // - Check if the timestamp is recent (prevents replay attacks)
            // - Validate the HMAC signature matches the payload
            // - Throw WebhookVerificationException if verification fails
            // 
            // Svix expects headers as HttpHeaders (Map<String, List<String>>)
            HttpHeaders headers = HttpHeaders.of(
                    Map.of(
                            "svix-id", List.of(svixId),
                            "svix-timestamp", List.of(svixTimestamp),
                            "svix-signature", List.of(svixSignature)
                    ),
                    (name, value) -> true  // BiPredicate that accepts all headers
            );
            
            webhook.verify(payload, headers);

            logger.info("Webhook signature verified successfully");

            // STEP 2: Parse the verified payload
            JsonNode webhookData = objectMapper.readTree(payload);
            String eventType = webhookData.get("type").asText();
            JsonNode data = webhookData.get("data");

            logger.info("Processing webhook event: {}", eventType);

            // STEP 3: Handle different event types
            return switch (eventType) {
                case "user.created" -> handleUserCreated(data);
                case "user.updated" -> handleUserUpdated(data);
                case "user.deleted" -> handleUserDeleted(data);
                default -> {
                    logger.warn("Unhandled webhook event type: {}", eventType);
                    yield ResponseEntity.ok(Map.of("message", "Event type not handled"));
                }
            };

        } catch (WebhookVerificationException e) {
            // Signature verification failed - this is a security issue
            logger.error("Webhook signature verification failed", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid webhook signature"));
        } catch (Exception e) {
            // Other errors (parsing, database, etc.)
            logger.error("Error processing webhook", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Failed to process webhook: " + e.getMessage()));
        }
    }

    /**
     * Handle user.created event.
     * Creates a new user in the database with data from Clerk.
     * 
     * @param data Clerk user data from webhook payload
     * @return ResponseEntity indicating success or failure
     */
    private ResponseEntity<?> handleUserCreated(JsonNode data) {
        try {
            String clerkId = data.get("id").asText();
            String email = extractEmail(data);
            String firstName = extractField(data, "first_name");
            String lastName = extractField(data, "last_name");
            UserRole role = extractRole(data);

            logger.info("Creating user with Clerk ID: {}", clerkId);

            userService.createUser(clerkId, email, firstName, lastName, role);

            return ResponseEntity.ok(Map.of(
                    "message", "User created successfully",
                    "clerkId", clerkId
            ));
        } catch (IllegalArgumentException e) {
            logger.warn("User already exists: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "User already exists"));
        } catch (Exception e) {
            logger.error("Error creating user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create user"));
        }
    }

    /**
     * Handle user.updated event.
     * Updates existing user information in the database.
     * 
     * @param data Clerk user data from webhook payload
     * @return ResponseEntity indicating success or failure
     */
    private ResponseEntity<?> handleUserUpdated(JsonNode data) {
        try {
            String clerkId = data.get("id").asText();
            String email = extractEmail(data);
            String firstName = extractField(data, "first_name");
            String lastName = extractField(data, "last_name");

            logger.info("Updating user with Clerk ID: {}", clerkId);

            userService.syncUserFromClerk(clerkId, email, firstName, lastName);

            // Check if role was updated in public_metadata
            UserRole newRole = extractRole(data);
            if (newRole != null) {
                userService.updateUserRole(clerkId, newRole);
            }

            return ResponseEntity.ok(Map.of(
                    "message", "User updated successfully",
                    "clerkId", clerkId
            ));
        } catch (Exception e) {
            logger.error("Error updating user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update user"));
        }
    }

    /**
     * Handle user.deleted event.
     * Permanently removes user from the database.
     * 
     * @param data Clerk user data from webhook payload
     * @return ResponseEntity indicating success or failure
     */
    private ResponseEntity<?> handleUserDeleted(JsonNode data) {
        try {
            String clerkId = data.get("id").asText();

            logger.info("Deleting user with Clerk ID: {}", clerkId);

            boolean deleted = userService.deleteUser(clerkId);

            if (deleted) {
                return ResponseEntity.ok(Map.of(
                        "message", "User deleted successfully",
                        "clerkId", clerkId
                ));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "User not found"));
            }
        } catch (Exception e) {
            logger.error("Error deleting user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete user"));
        }
    }

    /**
     * Extract primary email from Clerk user data.
     * Clerk stores emails in an email_addresses array.
     * 
     * @param data Clerk user data
     * @return Email address or null if not found
     */
    private String extractEmail(JsonNode data) {
        JsonNode emailAddresses = data.get("email_addresses");
        if (emailAddresses != null && emailAddresses.isArray() && emailAddresses.size() > 0) {
            return emailAddresses.get(0).get("email_address").asText();
        }
        return null;
    }

    /**
     * Extract a field from Clerk user data, returning null if not present.
     * 
     * @param data Clerk user data
     * @param fieldName Field name to extract
     * @return Field value or null
     */
    private String extractField(JsonNode data, String fieldName) {
        JsonNode field = data.get(fieldName);
        return (field != null && !field.isNull()) ? field.asText() : null;
    }

    /**
     * Extract user role from Clerk public_metadata.
     * Defaults to USER role if not specified.
     * 
     * Role mapping:
     * - Clerk stores role in: data.public_metadata.role
     * - Supports: ADMIN, EDITOR, USER
     * - Falls back to USER if missing or invalid
     * 
     * @param data Clerk user data
     * @return UserRole enum value
     */
    private UserRole extractRole(JsonNode data) {
        try {
            JsonNode publicMetadata = data.get("public_metadata");
            if (publicMetadata != null && publicMetadata.has("role")) {
                String roleString = publicMetadata.get("role").asText();
                return UserRole.valueOf(roleString.toUpperCase());
            }
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid role in public_metadata, defaulting to USER");
        }
        return UserRole.USER; // Default role
    }
}
