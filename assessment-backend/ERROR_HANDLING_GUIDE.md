# Spring Boot Error Handling Guide

## Exception Handling Best Practices

### Rule 1: Let GlobalExceptionHandler Do Its Job

**DON'T:**
```java
@GetMapping("/{id}")
public ResponseEntity<EntityDto> getEntity(@PathVariable UUID id) {
    try {
        EntityDto entity = service.getEntity(id);
        return ResponseEntity.ok(entity);
    } catch (ResourceNotFoundException e) {
        return ResponseEntity.notFound().build();
    } catch (IllegalStateException e) {
        return ResponseEntity.badRequest().build();
    }
}
```

**DO:**
```java
@GetMapping("/{id}")
public ResponseEntity<EntityDto> getEntity(@PathVariable UUID id) {
    // GlobalExceptionHandler will catch and handle exceptions
    EntityDto entity = service.getEntity(id);
    return ResponseEntity.ok(entity);
}
```

### Rule 2: Use Appropriate Exception Types

| Exception Type | HTTP Status | When to Use |
|----------------|-------------|-------------|
| `ResourceNotFoundException` | 404 Not Found | Entity doesn't exist in database |
| `IllegalStateException` | 400 Bad Request | Invalid operation for current state |
| `IllegalArgumentException` | 400 Bad Request | Invalid input parameters |
| `DuplicateSessionException` | 409 Conflict | Resource already exists |
| `AccessDeniedException` | 403 Forbidden | User lacks permissions |
| `AuthenticationException` | 401 Unauthorized | User not authenticated |

### Rule 3: Throw Exceptions in Service Layer

**Service Implementation:**
```java
@Service
@Transactional
public class EntityServiceImpl implements EntityService {

    @Override
    public EntityDto getEntity(UUID id) {
        // Throw ResourceNotFoundException for missing entities
        Entity entity = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Entity", id));

        // Throw IllegalStateException for invalid state operations
        if (entity.getStatus() == Status.ARCHIVED) {
            throw new IllegalStateException("Cannot modify archived entity");
        }

        // Throw IllegalArgumentException for invalid inputs
        if (entity.getRelatedItems() == null || entity.getRelatedItems().isEmpty()) {
            throw new IllegalArgumentException("Entity must have at least one related item");
        }

        return toDto(entity);
    }
}
```

### Rule 4: Minimize Controller Logging

**DON'T:**
```java
@GetMapping("/{id}")
public ResponseEntity<EntityDto> getEntity(@PathVariable UUID id) {
    logger.info("GET /api/entities/{}", id);
    EntityDto entity = service.getEntity(id);
    logger.info("Found entity: {}", entity);
    return ResponseEntity.ok(entity);
}
```

**DO:**
```java
@GetMapping("/{id}")
public ResponseEntity<EntityDto> getEntity(@PathVariable UUID id) {
    logger.debug("GET /api/entities/{}", id);
    EntityDto entity = service.getEntity(id);
    return ResponseEntity.ok(entity);
}
```

**Use INFO only for critical business events:**
```java
@PostMapping("/{id}/complete")
public ResponseEntity<ResultDto> completeEntity(@PathVariable UUID id) {
    ResultDto result = service.completeEntity(id);
    logger.info("Completed entity {}, score: {}", id, result.score());
    return ResponseEntity.ok(result);
}
```

### Rule 5: Add Defensive Validation in Service Methods

```java
public CurrentQuestionDto getCurrentQuestion(UUID sessionId) {
    // Layer 1: Session Exists → 404 if not found
    Session session = sessionRepository.findById(sessionId)
        .orElseThrow(() -> new ResourceNotFoundException("Session", sessionId));

    // Layer 2: Session Status → 400 if invalid state
    if (session.getStatus() == SessionStatus.COMPLETED) {
        throw new IllegalStateException("Cannot get current question for completed session");
    }

    // Layer 3: Data Integrity → 400 if data missing
    if (session.getQuestionOrder() == null || session.getQuestionOrder().isEmpty()) {
        log.error("Session {} has no questions in questionOrder", sessionId);
        throw new IllegalStateException("Session has no questions. Please contact support.");
    }

    // Layer 4: Bounds Checking → 400 if out of bounds
    int currentIndex = session.getCurrentQuestionIndex();
    if (currentIndex < 0 || currentIndex >= session.getQuestionOrder().size()) {
        throw new IllegalStateException("Invalid question index. Please contact support.");
    }

    // Layer 5: Related Entity Exists → 404 if missing
    UUID questionId = session.getQuestionOrder().get(currentIndex);
    Question question = questionRepository.findById(questionId)
        .orElseThrow(() -> new ResourceNotFoundException("Question", questionId));

    return buildResponse(session, question);
}
```

## Logging Configuration

### logback-spring.xml Pattern

```xml
<configuration>
    <!-- Root logger: WARN (minimize noise) -->
    <root level="WARN">
        <appender-ref ref="CONSOLE" />
    </root>

    <!-- Application: WARN (except critical services) -->
    <logger name="app.skillsoft.assessmentbackend" level="WARN" />

    <!-- Critical services: INFO (important business logic) -->
    <logger name="app.skillsoft.assessmentbackend.services.impl.CriticalService" level="INFO" />

    <!-- Controllers: WARN (reduce request spam) -->
    <logger name="app.skillsoft.assessmentbackend.controller" level="WARN" />

    <!-- Framework: WARN (minimize Spring/Hibernate logs) -->
    <logger name="org.hibernate" level="WARN" />
    <logger name="org.springframework" level="WARN" />
    <logger name="com.zaxxer.hikari" level="ERROR" />
</configuration>
```

### Logging Levels Guide

| Level | When to Use | Example |
|-------|-------------|---------|
| ERROR | Errors requiring immediate attention | Database connection failure |
| WARN | Potential issues or fallbacks | Using fallback question selection |
| INFO | Critical business events | Session completed, user registered |
| DEBUG | Development debugging | Request received, entity found |
| TRACE | Detailed debugging | SQL queries, method entry/exit |

**Production:** Set most loggers to WARN, critical services to INFO
**Development:** Set application to DEBUG, framework to INFO

## GlobalExceptionHandler Coverage

The GlobalExceptionHandler in this project handles:

1. **Spring Framework Exceptions:**
   - MethodArgumentNotValidException → 400
   - MissingServletRequestParameterException → 400
   - HttpRequestMethodNotSupportedException → 405
   - HttpMediaTypeNotSupportedException → 415
   - HttpMessageNotReadableException → 400
   - NoResourceFoundException → 404

2. **Security Exceptions:**
   - AccessDeniedException → 403
   - AuthenticationException → 401

3. **Domain Exceptions:**
   - ResourceNotFoundException → 404
   - IllegalStateException → 400
   - IllegalArgumentException → 400
   - DuplicateSessionException → 409

4. **Database Exceptions:**
   - DataIntegrityViolationException → 409
   - DataAccessException → 500

5. **Validation Exceptions:**
   - ConstraintViolationException → 400
   - MethodArgumentTypeMismatchException → 400

## Error Response Format

All errors return ErrorResponse DTO:

```json
{
  "status": 404,
  "message": "Session not found with id: 123e4567-e89b-12d3-a456-426614174000",
  "details": "The requested resource does not exist",
  "path": "/api/v1/tests/sessions/123e4567-e89b-12d3-a456-426614174000/current-question",
  "timestamp": "2025-12-11T22:30:00.123Z",
  "correlationId": "a1b2c3d4",
  "context": {
    "resourceType": "Session",
    "resourceId": "123e4567-e89b-12d3-a456-426614174000"
  }
}
```

## Common Patterns

### Pattern 1: Find Entity or Throw 404

```java
Entity entity = repository.findById(id)
    .orElseThrow(() -> new ResourceNotFoundException("Entity", id));
```

### Pattern 2: Validate State or Throw 400

```java
if (entity.getStatus() != Status.ACTIVE) {
    throw new IllegalStateException("Operation requires active status");
}
```

### Pattern 3: Validate Input or Throw 400

```java
if (input < 0 || input > 100) {
    throw new IllegalArgumentException("Value must be between 0 and 100");
}
```

### Pattern 4: Optional with Default

```java
return repository.findById(id)
    .map(this::toDto)
    .orElseGet(() -> getDefaultDto());
```

### Pattern 5: Optional to ResponseEntity

```java
return repository.findById(id)
    .map(ResponseEntity::ok)
    .orElseGet(() -> ResponseEntity.notFound().build());
```

## Testing Error Scenarios

```java
@Test
@DisplayName("Should throw ResourceNotFoundException when session not found")
void shouldThrowNotFoundWhenSessionMissing() {
    UUID sessionId = UUID.randomUUID();
    when(sessionRepository.findById(sessionId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.getCurrentQuestion(sessionId))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Session not found");
}

@Test
@DisplayName("Should throw IllegalStateException when session completed")
void shouldThrowBadRequestWhenSessionCompleted() {
    TestSession session = new TestSession();
    session.setStatus(SessionStatus.COMPLETED);
    when(sessionRepository.findById(any())).thenReturn(Optional.of(session));

    assertThatThrownBy(() -> service.getCurrentQuestion(session.getId()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("completed session");
}
```

## Summary

1. **Never** catch exceptions in controllers unless absolutely necessary
2. **Always** throw specific exception types in services
3. **Use** ResourceNotFoundException for 404 scenarios
4. **Use** IllegalStateException for invalid state operations
5. **Use** IllegalArgumentException for invalid inputs
6. **Log** at DEBUG level for routine operations
7. **Log** at INFO level for critical business events
8. **Log** at ERROR level for system failures
9. **Trust** GlobalExceptionHandler to handle exceptions properly
10. **Test** error scenarios with appropriate assertions
