package dev.suprim.query.exception;

/**
 * Unchecked wrapper for {@link DbException}, preserving the HTTP-like error code
 * across call boundaries that do not permit checked exceptions
 * (e.g. {@code RSQLVisitor} interface methods).
 *
 * <p>Callers can catch this exception and inspect {@link #getCode()} to map it
 * to an appropriate HTTP status or application error code instead of treating
 * all failures as generic server errors.
 *
 * <pre>{@code
 * try {
 *     readService.read(request);
 * } catch (DbRuntimeException e) {
 *     if (e.getCode() == 400) {
 *         // 400 Bad Request — invalid RSQL filter
 *     }
 * }
 * }</pre>
 */
public class DbRuntimeException extends RuntimeException {

    private final int code;

    public DbRuntimeException(DbException cause) {
        super(cause.getMessage(), cause);
        this.code = cause.getCode();
    }

    /**
     * Returns the numeric error code from the wrapped {@link DbException}
     * (mirrors {@link DbErrorCode} values, e.g. 400, 404, 503).
     */
    public int getCode() {
        return code;
    }
}
