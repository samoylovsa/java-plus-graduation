package ewm.common.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;


@Slf4j
@RestControllerAdvice
public class GlobalErrorHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> String.format("Field: %s. Error: %s. Value: %s",
                        error.getField(), error.getDefaultMessage(), error.getRejectedValue()))
                .orElse("Validation failed");

        return new ApiError(
                HttpStatus.BAD_REQUEST,
                "Incorrectly made request.",
                errorMessage,
                LocalDateTime.now()
        );
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleMissingServletRequestParameter(MissingServletRequestParameterException ex) {
        return new ApiError(
                HttpStatus.BAD_REQUEST,
                "Required request parameter missing.",
                ex.getMessage(),
                LocalDateTime.now()
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String errorMessage = String.format(
                "Failed to convert value of type %s to required type %s; %s",
                ex.getValue() != null ? ex.getValue().getClass().getSimpleName() : "null",
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown",
                ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage()
        );

        return new ApiError(
                HttpStatus.BAD_REQUEST,
                "Incorrectly made request.",
                errorMessage,
                LocalDateTime.now()
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleConstraintViolation(ConstraintViolationException ex) {
        String errorMessage = ex.getConstraintViolations()
                .stream()
                .findFirst()
                .map(violation -> String.format("Field: %s. Error: %s. Value: %s",
                        violation.getPropertyPath(), violation.getMessage(), violation.getInvalidValue()))
                .orElse("Constraint violation");

        return new ApiError(
                HttpStatus.BAD_REQUEST,
                "Incorrectly made request.",
                errorMessage,
                LocalDateTime.now()
        );
    }

    @ExceptionHandler(BusinessRuleException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleBusinessRule(BusinessRuleException ex) {
        return new ApiError(
                HttpStatus.BAD_REQUEST,
                "Incorrectly made request.",
                ex.getMessage(),
                LocalDateTime.now()
        );
    }

    @ExceptionHandler(ConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleConflict(ConflictException ex) {
        log.warn("Conflict: {}", ex.getMessage());
        return new ApiError(
                HttpStatus.CONFLICT,
                "For the requested operation the conditions are not met.",
                ex.getMessage(),
                LocalDateTime.now()
        );
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError handleNotFound(NotFoundException ex) {
        log.warn("Not found: {}", ex.getMessage());
        return new ApiError(
                HttpStatus.NOT_FOUND,
                "The required object was not found.",
                ex.getMessage(),
                LocalDateTime.now()
        );
    }

    @ExceptionHandler(ValidationException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiError handleValidation(ValidationException ex) {
        log.warn("Validation error: {}", ex.getMessage());
        return new ApiError(
                HttpStatus.FORBIDDEN,
                "For the requested operation the conditions are not met.",
                ex.getMessage(),
                LocalDateTime.now()
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiError handleAccessDenied(AccessDeniedException ex) {
        return new ApiError(
                HttpStatus.FORBIDDEN,
                "Access denied.",
                ex.getMessage(),
                LocalDateTime.now()
        );
    }

    @ExceptionHandler(ServiceUnavailableException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ApiError handleServiceUnavailable(ServiceUnavailableException ex) {
        log.warn("Dependency unavailable: {}", ex.getMessage());
        return new ApiError(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Required service is temporarily unavailable.",
                ex.getMessage(),
                LocalDateTime.now()
        );
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleIllegalState(IllegalStateException ex) {
        return new ApiError(
                HttpStatus.CONFLICT,
                "For the requested operation the conditions are not met.",
                ex.getMessage(),
                LocalDateTime.now()
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleIllegalArgument(IllegalArgumentException ex) {
        return new ApiError(
                HttpStatus.BAD_REQUEST,
                "Incorrectly made request.",
                ex.getMessage(),
                LocalDateTime.now()
        );
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiError handleException(Exception ex) {
        log.error("Internal error", ex);
        return new ApiError(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal server error",
                ex.getMessage() != null ? ex.getMessage() : "Unexpected error",
                LocalDateTime.now()
        );
    }
}
