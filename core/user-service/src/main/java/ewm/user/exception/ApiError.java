package ewm.user.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public class ApiError {

    private final HttpStatus status;
    private final String reason;
    private final String message;
}
