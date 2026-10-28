<!-- Load when: throwing domain error, designing validation message, or wiring `@ExceptionHandler`. -->

# Errors

## Domain errors

- All domain-thrown errors extend `com.faust0z.B2BSystem.core.DomainRuleException`. Exception carries its own HTTP status, title, message — no need to map elsewhere. See `core/DomainRuleException.java`.
- Examples: `core/ResourceNotFoundException.java`, `core/EndDateBeforeStartException.java`, `core/PinRequiredException.java`, `staffing/web/DaysRangeTooWideException.java`, `staffing/web/IncorrectMinutesAmountException.java`, `user/RoleEscalationException.java`, `access/domain/DniNotFoundException.java`, `access/domain/AmbiguousIdentityException.java`.

## Global handler

- `core/GlobalExceptionHandler.java` is `@RestControllerAdvice` that maps every thrown type to RFC-7807 `ProblemDetail`:
  - `DomainRuleException` → uses exception's own status/title/message.
  - `BadCredentialsException` → 401.
  - `AccessDeniedException` → 403.
  - `MethodArgumentNotValidException` / `BindException` / `HandlerMethodValidationException` → 400, extracts first field error message.
  - `HttpMessageNotReadableException` → 400 "JSON Malformado".
  - `OptimisticLockingFailureException` → 409.
  - `NoResourceFoundException` → 404 with resource path.
  - Everything else → 500.
- Handler attaches `traceId` property (from MDC) and `timestamp`. Do not add `traceId` manually in services.
- Logger picks WARN for 4xx and ERROR for 5xx.

## Validation message style

- Field errors return first `@NotBlank` / `@Size` / etc. message. Keep messages short and concrete (Spanish to match current code: e.g. `"El DNI es obligatorio"`, `"El email debe ser válido"`).

## What NOT to do

- Do not catch exceptions inside services to rethrow as `ResponseStatusException` — throw `DomainRuleException` subclass so global handler picks it up.
- Do not use `Optional` as return type from controllers.
- Do not return `Map<String, String>` errors from controllers.
