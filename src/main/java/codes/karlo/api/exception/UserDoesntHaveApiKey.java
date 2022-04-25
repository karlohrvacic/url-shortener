package codes.karlo.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class UserDoesntHaveApiKey extends CommonException {
    public UserDoesntHaveApiKey(final String message) {
        super(message);
    }
}
