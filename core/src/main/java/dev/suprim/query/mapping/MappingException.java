package dev.suprim.query.mapping;

/**
 * Thrown when result mapping fails (reflection error, type coercion failure, etc.).
 */
public class MappingException extends RuntimeException {

	public MappingException(String message) {
		super(message);
	}

	public MappingException(String message, Throwable cause) {
		super(message, cause);
	}
}
