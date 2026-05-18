package dev.suprim.query.model;

import static java.util.Objects.isNull;

/**
 * A raw SQL expression that can be used in SELECT, WHERE, or ORDER BY clauses.
 * No quoting or escaping is applied — the expression is rendered as-is.
 *
 * @param expression the raw SQL expression
 * @param alias      optional alias for the expression
 */
public record ExpressionField(String expression, String alias) {

	public ExpressionField {
		if (isNull(expression) || expression.isBlank()) {
			throw new IllegalArgumentException("Expression must not be null or blank");
		}
	}

	/**
	 * Creates an ExpressionField from a raw SQL string.
	 *
	 * @param sql the raw SQL expression
	 * @return a new ExpressionField with no alias
	 */
	public static ExpressionField raw(String sql) {
		return new ExpressionField(sql, null);
	}

	/**
	 * Returns a new ExpressionField with the given alias.
	 *
	 * @param alias the alias to assign
	 * @return a new ExpressionField with the alias set
	 */
	public ExpressionField as(String alias) {
		if (isNull(alias) || alias.isBlank()) {
			throw new IllegalArgumentException("Alias must not be null or blank");
		}
		return new ExpressionField(expression, alias);
	}

	/**
	 * Renders the expression, appending {@code AS "alias"} if an alias is set.
	 *
	 * @return the rendered SQL fragment
	 */
	public String renderWithAlias() {
		if (isNull(alias)) {
			return expression;
		}
		return expression + " AS \"" + alias + "\"";
	}
}
