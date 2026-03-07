package ewm.event.client.config;

import ewm.common.exception.AccessDeniedException;
import ewm.common.exception.ConflictException;
import ewm.common.exception.NotFoundException;
import ewm.common.exception.ServiceUnavailableException;
import ewm.common.exception.ValidationException;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;

@Slf4j
public class EventClientConfig {

    private final ErrorDecoder defaultDecoder = new ErrorDecoder.Default();

    @Bean
    public ErrorDecoder eventClientErrorDecoder() {
        return (methodKey, response) -> {
            int status = response.status();
            String message = "event-service: " + response.reason();
            if (status == 400) {
                log.debug("event-service bad request: {} -> 400", methodKey);
                return new ValidationException(message);
            }
            if (status == 403) {
                log.debug("event-service forbidden: {} -> 403", methodKey);
                return new AccessDeniedException(message);
            }
            if (status == 404) {
                log.debug("event-service not found: {} -> 404", methodKey);
                return new NotFoundException("Event not found");
            }
            if (status == 409) {
                log.debug("event-service conflict: {} -> 409", methodKey);
                return new ConflictException(message);
            }
            if (status >= 500) {
                log.warn("event-service error: {} -> {}", methodKey, status);
                return new ServiceUnavailableException(message);
            }
            return defaultDecoder.decode(methodKey, response);
        };
    }
}
