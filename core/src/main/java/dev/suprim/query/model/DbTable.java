package dev.suprim.query.model;

import dev.suprim.query.exception.DbErrorCode;
import dev.suprim.query.exception.DbException;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static java.util.Objects.isNull;

@Slf4j
public record DbTable(
		String schema, String name, String fullName, String alias,
		List<DbColumn> dbColumns, String type, String coverChar
) {
	public String render() {
		return fullName + " " + alias;
	}

	public DbTable copyWithAlias(String tableAlias) {
		List<DbColumn> columns =
				dbColumns.stream()
				         .map(col -> col.copyWithTableAlias(tableAlias))
				         .toList();
		return new DbTable(
				schema,
				name,
				fullName,
				tableAlias,
				columns,
				type,
				coverChar
		);
	}

	public DbColumn buildColumn(String columnName) throws DbException {
		if (isNull(columnName) || columnName.isBlank()) {
			throw new DbException(
					DbErrorCode.INVALID_REQUEST,
					"Column name must not be null or blank"
			);
		}
		DbAlias dbAlias = getAlias(columnName);
		return getDbColumn(dbAlias);
	}

	private DbColumn getDbColumn(DbAlias dbAlias) throws DbException {
		return this.dbColumns.stream()
		                     .filter(col -> dbAlias.name()
		                                           .equalsIgnoreCase(col.name())
		                     )
		                     .map(col -> col.copyWithAlias(dbAlias))
		                     .findFirst()
		                     .orElseThrow(() -> new DbException(
				                     DbErrorCode.INVALID_REQUEST,
				                     "Column not found: %s.%s".formatted(
						                     name,
						                     dbAlias.name()
				                     )
		                     ));
	}

	/**
	 * Parses a field expression into column name, alias, and JSONB operator parts.
	 * Uses {@code column:alias} syntax for renaming output columns.
	 *
	 * <p><b>Syntax guide:</b>
	 * <ul>
	 *   <li>Arrow operators ({@code ->}, {@code ->>}): chainable, auto-quotes unquoted keys</li>
	 *   <li>Path operators ({@code #>}, {@code #>>}): dot-separated path, wrapped in PostgreSQL array syntax</li>
	 *   <li>Asterisk shorthand ({@code *}, {@code **}): splits on delimiter, quotes each segment</li>
	 * </ul>
	 *
	 * @see DbAlias
	 */
	private DbAlias getAlias(String fieldExpression) throws DbException {
		String[] aliasParts = fieldExpression.split(":", 2);
		String columnName = aliasParts[0];
		String colName = columnName;
		String jsonParts = "";

		if (columnName.contains("#>>")) {
			colName = columnName.substring(0, columnName.indexOf("#>>"));
			jsonParts = parsePathArray(columnName, "#>>", 3);
		} else if (columnName.contains("#>")) {
			colName = columnName.substring(0, columnName.indexOf("#>"));
			jsonParts = parsePathArray(columnName, "#>", 2);
		} else if (columnName.contains("->>") || columnName.contains("->")) {
			jsonParts = parseChainedArrows(columnName);
			colName = columnName.substring(0, columnName.indexOf("->"));
		} else if (columnName.contains("**")) {
			colName = columnName.substring(0, columnName.indexOf("**"));
			jsonParts = parseAsteriskShorthand(
					columnName,
					"\\*\\*",
					true,
					fieldExpression
			);
		} else if (columnName.contains("*")) {
			colName = columnName.substring(0, columnName.indexOf("*"));
			jsonParts = parseAsteriskShorthand(
					columnName,
					"\\*",
					false,
					fieldExpression
			);
		}

		String alias = aliasParts.length == 2 ? aliasParts[1] : "";
		return new DbAlias(colName.trim(), alias.trim(), jsonParts);
	}

	/**
	 * Parses PostgreSQL path array syntax: {@code data#>>a.b.c} produces {@code #>>'{a,b,c}'}
	 */
	private String parsePathArray(
			String columnName,
			String operator,
			int operatorLength
	) {
		int index = columnName.indexOf(operator);
		String path = columnName.substring(index + operatorLength);
		return operator + "'{" + path.replace(".", ",") + "}'";
	}

	/**
	 * Parses asterisk shorthand into chained arrow operators.
	 * <ul>
	 *   <li>{@code **} (textExtractLast=true): intermediate uses {@code ->}, last uses {@code ->>}</li>
	 *   <li>{@code *} (textExtractLast=false): all segments use {@code ->}</li>
	 * </ul>
	 */
	private String parseAsteriskShorthand(
			String columnName, String delimiterRegex,
			boolean textExtractLast, String fieldExpression
	) throws DbException {
		int index = textExtractLast ? columnName.indexOf("**") : columnName.indexOf(
				"*");
		String remainder = columnName.substring(
				index + (textExtractLast ? 2 : 1));
		validateNotBlank(remainder, fieldExpression);
		String[] segments = remainder.split(delimiterRegex);
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < segments.length; i++) {
			if (textExtractLast && i == segments.length - 1) {
				sb.append("->>'").append(segments[i]).append("'");
			} else {
				sb.append("->'").append(segments[i]).append("'");
			}
		}
		return sb.toString();
	}

	private void validateNotBlank(
			String remainder,
			String expression
	) throws DbException {
		if (remainder.isBlank()) {
			throw new DbException(
					DbErrorCode.INVALID_REQUEST,
					"Invalid JSONB path expression: trailing delimiter in '%s'".formatted(
							expression)
			);
		}
	}

	/**
	 * Parses chained arrow operators ({@code ->} and {@code ->>}) from a field expression.
	 * Walks the expression left-to-right, consuming each operator and its key.
	 * Unquoted keys are auto-wrapped in single quotes for valid PostgreSQL syntax.
	 *
	 * <p>Example: {@code data->feedback->>type} produces {@code ->'feedback'->>'type'}
	 */
	private String parseChainedArrows(String expression) throws DbException {
		StringBuilder result = new StringBuilder();
		int i = expression.indexOf("->");

		while (i < expression.length() && expression.charAt(i) == '-') {
			if (expression.startsWith("->>", i)) {
				i += 3;
				String key = extractKey(expression, i);
				i += key.length();
				result.append("->>").append(quoteIfNeeded(key));
			} else {
				i += 2;
				String key = extractKey(expression, i);
				i += key.length();
				result.append("->").append(quoteIfNeeded(key));
			}
		}
		return result.toString();
	}

	/**
	 * Extracts a JSON key starting at the given position.
	 * <ul>
	 *   <li>Quoted: {@code 'feedback'} — reads until closing quote</li>
	 *   <li>Unquoted: {@code feedback} — reads until next {@code ->} or end of string</li>
	 * </ul>
	 */
	private String extractKey(String expression, int start) throws DbException {
		if (start >= expression.length()) {
			return "";
		}
		if (expression.charAt(start) == '\'') {
			int end = expression.indexOf('\'', start + 1);
			if (end == -1) {
				throw new DbException(
						DbErrorCode.INVALID_REQUEST,
						"Unclosed quote in JSONB path expression: '%s'".formatted(
								expression)
				);
			}
			return expression.substring(start, end + 1);
		}
		int nextOp = expression.indexOf("->", start);
		if (nextOp == -1) {
			return expression.substring(start);
		}
		return expression.substring(start, nextOp);
	}

	/**
	 * Wraps key in single quotes if not already quoted.
	 * A key is considered quoted if it starts with a single quote and has length >= 2.
	 */
	private String quoteIfNeeded(String key) {
		if (key.length() >= 2 && key.charAt(0) == '\'') {
			return key;
		}
		return "'" + key + "'";
	}

	public List<DbColumn> buildColumns() {
		return dbColumns;
	}

	public List<DbColumn> buildPkColumns() {
		return dbColumns.stream().filter(DbColumn::pk).toList();
	}

	public String[] getKeyColumnNames() {
		return buildPkColumns().stream().map(DbColumn::name).toList().toArray(
				String[]::new);
	}

	public String getColumnDataTypeName(String columnName) throws DbException {
		return lookupColumn(columnName).columnDataTypeName();
	}

	private DbColumn lookupColumn(String columnName) throws DbException {
		return dbColumns.stream()
		                .filter(col -> columnName.equalsIgnoreCase(col.name()))
		                .findFirst()
		                .orElseThrow(() -> new DbException(
				                DbErrorCode.INVALID_REQUEST,
				                "Column not found: %s.%s".formatted(
						                name,
						                columnName
				                )
		                ));
	}
}
