package dev.suprim.query.exception;

import lombok.Getter;

/**
 * Exception for database operations.
 * Replaces ApiException from ai-runtime-backend.
 */
@Getter
public class DbException extends Exception {
    private final int code;
    private final Object data;

    public DbException(DbErrorCode error) {
        super(error.getMessage());
        this.code = error.getCode();
        this.data = null;
    }

    public DbException(DbErrorCode error, String message) {
        super(message);
        this.code = error.getCode();
        this.data = null;
    }

    public DbException(DbErrorCode error, String message, Object data) {
        super(message);
        this.code = error.getCode();
        this.data = data;
    }

    public DbException(DbErrorCode error, Throwable cause) {
        super(error.getMessage(), cause);
        this.code = error.getCode();
        this.data = null;
    }
}
