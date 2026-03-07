package ewm.request.exception;

/**
 * Выбрасывается при недоступности зависимого сервиса (user-service, event-service).
 * Обрабатывается ErrorHandler как HTTP 503 Service Unavailable.
 */
public class ServiceUnavailableException extends RuntimeException {

    public ServiceUnavailableException(String message) {
        super(message);
    }

    public ServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
