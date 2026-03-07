package ewm.comment.exception;

/**
 * Thrown when a dependent service (event-service) is unavailable.
 * Handled by ErrorHandler as HTTP 503 Service Unavailable.
 */
public class ServiceUnavailableException extends RuntimeException {

    public ServiceUnavailableException(String message) {
        super(message);
    }

    public ServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
