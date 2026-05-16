package cc.hrva.urlshortener.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidApiKeyException extends CommonException {

    public InvalidApiKeyException(final String message) {
        super(message);
    }

}
