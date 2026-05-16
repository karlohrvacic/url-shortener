package cc.hrva.urlshortener.exception;

public abstract class CommonException extends RuntimeException {

    protected CommonException() {
        super();
    }

    protected CommonException(final String message) {
        super(message);
    }

    protected CommonException(final String message, final Throwable cause) {
        super(message, cause);
    }

}
