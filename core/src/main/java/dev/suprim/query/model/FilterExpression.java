package dev.suprim.query.model;

/**
 * Interface for objects that can produce an RSQL filter string.
 * Implemented by {@code FilterBuilder} in the rsql module.
 */
@FunctionalInterface
public interface FilterExpression {

	/**
	 * Produces the RSQL filter string.
	 */
	String toFilter();
}
