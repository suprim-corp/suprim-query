package dev.suprim.query.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Error codes for database operations.
 * Replaces API_STATUS from ai-runtime-backend.
 */
@Getter
@RequiredArgsConstructor
public enum DbErrorCode {
    SUCCESS(1, "Success"),
    SERVER_ERROR(99, "Server error"),
    INVALID_REQUEST(400, "Invalid request"),
    UNAUTHORIZED(401, "Unauthorized"),
    FORBIDDEN(403, "Forbidden"),
    NOT_FOUND(404, "Not found"),
    CONFLICT(409, "Conflict"),
    NOT_CONFIGURED(503, "Not configured");

    private final int code;
    private final String message;
}
