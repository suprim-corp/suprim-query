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
	 * Creates a COALESCE expression from the given arguments.
	 * Arguments are rendered as-is (no quoting) — user handles quoting.
	 *
	 * @param args the arguments to COALESCE
	 * @return a new ExpressionField with no alias
	 */
	public static ExpressionField coalesce(String... args) {
		if (isNull(args) || args.length == 0) {
			throw new IllegalArgumentException("Arguments must not be null or empty");
		}
		for (int i = 0; i < args.length; i++) {
			if (isNull(args[i]) || args[i].isBlank()) {
				throw new IllegalArgumentException("Argument at index " + i + " must not be null or blank");
			}
		}
		return new ExpressionField("COALESCE(" + String.join(", ", args) + ")", null);
	}

	/**
	 * Creates a NULLIF expression.
	 * Arguments are rendered as-is (no quoting) — user handles quoting.
	 *
	 * @param expr  the expression to evaluate
	 * @param value the value to compare against
	 * @return a new ExpressionField with no alias
	 */
	public static ExpressionField nullif(String expr, String value) {
		if (isNull(expr) || expr.isBlank()) {
			throw new IllegalArgumentException("Expression argument must not be null or blank");
		}
		if (isNull(value) || value.isBlank()) {
			throw new IllegalArgumentException("Value argument must not be null or blank");
		}
		return new ExpressionField("NULLIF(" + expr + ", " + value + ")", null);
	}

	/**
	 * Creates a DATE_TRUNC expression.
	 * Precision is auto-wrapped with single quotes, column is auto-quoted with double quotes.
	 * Renders as: {@code DATE_TRUNC('precision', "column")}.
	 *
	 * @param precision the truncation precision (e.g. year, month, day)
	 * @param column    the column name
	 * @return a new ExpressionField with no alias
	 */
	public static ExpressionField dateTrunc(String precision, String column) {
		if (isNull(precision) || precision.isBlank()) {
			throw new IllegalArgumentException("Precision must not be null or blank");
		}
		validateColumn(column);
		return new ExpressionField("DATE_TRUNC('" + precision.strip() + "', " + quoteColumn(column) + ")", null);
	}

	/**
	 * Creates an EXTRACT expression.
	 * Field is rendered raw (SQL keyword), source is auto-quoted with double quotes.
	 * Renders as: {@code EXTRACT(field FROM "source")}.
	 *
	 * @param field  the date/time field to extract (e.g. YEAR, MONTH, DAY, HOUR)
	 * @param source the source column name
	 * @return a new ExpressionField with no alias
	 */
	public static ExpressionField extract(String field, String source) {
		if (isNull(field) || field.isBlank()) {
			throw new IllegalArgumentException("Field must not be null or blank");
		}
		if (isNull(source) || source.isBlank()) {
			throw new IllegalArgumentException("Source must not be null or blank");
		}
		return new ExpressionField("EXTRACT(" + field.strip() + " FROM " + quoteColumn(source) + ")", null);
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
