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
	 * Creates a COUNT aggregate expression.
	 * If column is {@code "*"}, renders as {@code COUNT(*)}.
	 * Otherwise, the column name is quoted: {@code COUNT("column")}.
	 *
	 * @param column the column name or "*"
	 * @return a new ExpressionField with no alias
	 */
	public static ExpressionField count(String column) {
		validateColumn(column);
		String col = "*".equals(column.strip()) ? "*" : quoteColumn(column);
		return new ExpressionField("COUNT(" + col + ")", null);
	}

	/**
	 * Creates a COUNT(DISTINCT ...) aggregate expression.
	 * The column name is quoted: {@code COUNT(DISTINCT "column")}.
	 *
	 * @param column the column name
	 * @return a new ExpressionField with no alias
	 */
	public static ExpressionField countDistinct(String column) {
		validateColumn(column);
		return new ExpressionField("COUNT(DISTINCT " + quoteColumn(column) + ")", null);
	}

	/**
	 * Creates a SUM aggregate expression.
	 * The column name is quoted: {@code SUM("column")}.
	 *
	 * @param column the column name
	 * @return a new ExpressionField with no alias
	 */
	public static ExpressionField sum(String column) {
		validateColumn(column);
		return new ExpressionField("SUM(" + quoteColumn(column) + ")", null);
	}

	/**
	 * Creates an AVG aggregate expression.
	 * The column name is quoted: {@code AVG("column")}.
	 *
	 * @param column the column name
	 * @return a new ExpressionField with no alias
	 */
	public static ExpressionField avg(String column) {
		validateColumn(column);
		return new ExpressionField("AVG(" + quoteColumn(column) + ")", null);
	}

	/**
	 * Creates a MAX aggregate expression.
	 * The column name is quoted: {@code MAX("column")}.
	 *
	 * @param column the column name
	 * @return a new ExpressionField with no alias
	 */
	public static ExpressionField max(String column) {
		validateColumn(column);
		return new ExpressionField("MAX(" + quoteColumn(column) + ")", null);
	}

	/**
	 * Creates a MIN aggregate expression.
	 * The column name is quoted: {@code MIN("column")}.
	 *
	 * @param column the column name
	 * @return a new ExpressionField with no alias
	 */
	public static ExpressionField min(String column) {
		validateColumn(column);
		return new ExpressionField("MIN(" + quoteColumn(column) + ")", null);
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

	private static void validateColumn(String column) {
		if (isNull(column) || column.isBlank()) {
			throw new IllegalArgumentException("Column must not be null or blank");
		}
	}

	private static String quoteColumn(String column) {
		return "\"" + column.strip() + "\"";
	}
}
