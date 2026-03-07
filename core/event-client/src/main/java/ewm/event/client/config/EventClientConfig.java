package ewm.event.client.config;

import ewm.common.exception.NotFoundException;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;

@Slf4j
public class EventClientConfig {

    private final ErrorDecoder defaultDecoder = new ErrorDecoder.Default();

    @Bean
    public ErrorDecoder eventClientErrorDecoder() {
        return (methodKey, response) -> {
            if (response.status() == 404) {
                log.debug("Event not found from event-service: {} -> 404", methodKey);
                return new NotFoundException("Event not found");
            }
            if (response.status() >= 500) {
                log.warn("event-service error: {} -> {}", methodKey, response.status());
            }
            return defaultDecoder.decode(methodKey, response);
        };
    }
}
