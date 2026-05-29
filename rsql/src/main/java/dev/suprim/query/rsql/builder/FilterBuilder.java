package dev.suprim.query.rsql.builder;

import cz.jirutka.rsql.parser.ast.RSQLOperators;
import dev.suprim.query.model.FilterExpression;
import dev.suprim.query.rsql.operators.CustomRSQLOperators;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Fluent, type-safe RSQL filter builder.
 *
 * <pre>{@code
 * FilterBuilder.and()
 *     .eq("type", "SECRET")
 *     .eq("workspace_id", workspaceId)
 *     .or(FilterBuilder.or()
 *         .eq("principal_kind", "GROUP")
 *         .and(FilterBuilder.and()
 *             .eq("principal_kind", "MEMBER")
 *             .eq("principal_id", memberId)
 *         )
 *     )
 *     .build();
 * }</pre>
 */
public class FilterBuilder implements FilterExpression {

	// ==================== TYPES ====================

	public enum LogicalOperator {
		AND("and"), OR("or");

		private final String symbol;

		LogicalOperator(String symbol) {
			this.symbol = symbol;
		}

		public String symbol() {
			return symbol;
		}
	}

	public sealed interface Predicate permits Comparison, Group, Raw {
		String toRsql();
	}

	public record Comparison(
			String field, String operator, List<String> values
	) implements Predicate {
		@Override
		public String toRsql() {
			if (values.size() == 1) {
				return field + operator + "'" + values.getFirst() + "'";
			}
			String joined = String.join(",", values);
			return field + operator + "(" + joined + ")";
		}
	}

	public record Group(
			LogicalOperator operator, List<Predicate> predicates
	) implements Predicate {
		@Override
		public String toRsql() {
			if (predicates.isEmpty()) return "";
			String sep = " " + operator.symbol() + " ";
			String joined = predicates.stream()
			                          .map(Predicate::toRsql)
			                          .collect(Collectors.joining(sep));
			return predicates.size() > 1 ? "(" + joined + ")" : joined;
		}
	}

	public record Raw(String rsql) implements Predicate {
		@Override
		public String toRsql() {
			return "(" + rsql + ")";
		}
	}

	// ==================== BUILDER ====================

	private final LogicalOperator operator;
	private final List<Predicate> predicates = new ArrayList<>();

	private FilterBuilder(LogicalOperator operator) {
		this.operator = operator;
	}

	public static FilterBuilder and() {
		return new FilterBuilder(LogicalOperator.AND);
	}

	public static FilterBuilder or() {
		return new FilterBuilder(LogicalOperator.OR);
	}

	// ==================== COMPARISON ====================

	public FilterBuilder eq(String field, String value) {
		predicates.add(
				new Comparison(
						field,
						RSQLOperators.EQUAL.getSymbol(),
						List.of(value)
				)
		);
		return this;
	}

	public FilterBuilder neq(String field, String value) {
		predicates.add(
				new Comparison(
						field,
						RSQLOperators.NOT_EQUAL.getSymbol(),
						List.of(value)
				)
		);
		return this;
	}

	public FilterBuilder gt(String field, String value) {
		predicates.add(
				new Comparison(
						field,
						RSQLOperators.GREATER_THAN.getSymbol(),
						List.of(value)
				)
		);
		return this;
	}

	public FilterBuilder gte(String field, String value) {
		predicates.add(
				new Comparison(
						field,
						RSQLOperators.GREATER_THAN_OR_EQUAL.getSymbol(),
						List.of(value)
				)
		);
		return this;
	}

	public FilterBuilder lt(String field, String value) {
		predicates.add(
				new Comparison(
						field,
						RSQLOperators.LESS_THAN.getSymbol(),
						List.of(value)
				)
		);
		return this;
	}

	public FilterBuilder lte(String field, String value) {
		predicates.add(
				new Comparison(
						field,
						RSQLOperators.LESS_THAN_OR_EQUAL.getSymbol(),
						List.of(value)
				)
		);
		return this;
	}

	public FilterBuilder in(String field, String... values) {
		predicates.add(
				new Comparison(
						field,
						RSQLOperators.IN.getSymbol(),
						Arrays.asList(values)
				)
		);
		return this;
	}

	public FilterBuilder notIn(String field, String... values) {
		predicates.add(
				new Comparison(
						field,
						RSQLOperators.NOT_IN.getSymbol(),
						Arrays.asList(values)
				)
		);
		return this;
	}

	public FilterBuilder like(String field, String value) {
		predicates.add(new Comparison(
				field,
				CustomRSQLOperators.LIKE.getSymbol(),
				List.of(value)
		));
		return this;
	}

	public FilterBuilder iLike(String field, String value) {
		predicates.add(
				new Comparison(
						field,
						CustomRSQLOperators.ILIKE.getSymbol(),
						List.of(value)
				)
		);
		return this;
	}

	public FilterBuilder startWith(String field, String value) {
		predicates.add(
				new Comparison(
						field,
						CustomRSQLOperators.START_WITH.getSymbol(),
						List.of(value)
				)
		);
		return this;
	}

	public FilterBuilder endWith(String field, String value) {
		predicates.add(
				new Comparison(
						field,
						CustomRSQLOperators.END_WITH.getSymbol(),
						List.of(value)
				)
		);
		return this;
	}

	public FilterBuilder isNull(String field) {
		predicates.add(
				new Comparison(
						field,
						CustomRSQLOperators.IS_NULL.getSymbol(),
						List.of("true")
				)
		);
		return this;
	}

	public FilterBuilder isNotNull(String field) {
		predicates.add(
				new Comparison(
						field,
						CustomRSQLOperators.NOT_NULL.getSymbol(),
						List.of("true")
				)
		);
		return this;
	}

	public FilterBuilder jsonbContains(String field, String value) {
		predicates.add(
				new Comparison(
						field,
						CustomRSQLOperators.JSONB_CONTAIN.getSymbol(),
						List.of(value)
				)
		);
		return this;
	}

	/**
	 * JSONB contains with a single key-value pair.
	 * Generates: {@code field=jsonbContain='{"key":"value"}'}.
	 */
	public FilterBuilder jsonbContains(String field, String key, String value) {
		String json = "{\"" + key + "\":\"" + value + "\"}";
		predicates.add(
				new Comparison(
						field,
						CustomRSQLOperators.JSONB_CONTAIN.getSymbol(),
						List.of(json)
				)
		);
		return this;
	}

	/**
	 * JSONB contains with a map of key-value pairs.
	 * Values are serialized as JSON strings (toString for non-String types).
	 * Generates: {@code field=jsonbContain='{"k1":"v1","k2":"v2"}'}.
	 */
	public FilterBuilder jsonbContains(String field, Map<String, Object> map) {
		String json = mapToJson(map);
		predicates.add(
				new Comparison(
						field,
						CustomRSQLOperators.JSONB_CONTAIN.getSymbol(),
						List.of(json)
				)
		);
		return this;
	}

	public FilterBuilder jsonbKeyExists(String field, String key) {
		predicates.add(
				new Comparison(
						field,
						CustomRSQLOperators.JSONB_KEY_EXISTS.getSymbol(),
						List.of(key)
				)
		);
		return this;
	}

	// ==================== CONDITIONAL (null-safe) ====================

	/**
	 * Adds an equality predicate only if value is non-null.
	 * Accepts any type — calls {@code toString()} on the value.
	 */
	public FilterBuilder eqIfPresent(String field, Object value) {
		if (value != null) {
			eq(field, value.toString());
		}
		return this;
	}

	/**
	 * Adds a not-equal predicate only if value is non-null.
	 */
	public FilterBuilder neqIfPresent(String field, Object value) {
		if (value != null) {
			neq(field, value.toString());
		}
		return this;
	}

	/**
	 * Adds a greater-than predicate only if value is non-null.
	 */
	public FilterBuilder gtIfPresent(String field, Object value) {
		if (value != null) {
			gt(field, value.toString());
		}
		return this;
	}

	/**
	 * Adds a greater-than-or-equal predicate only if value is non-null.
	 */
	public FilterBuilder gteIfPresent(String field, Object value) {
		if (value != null) {
			gte(field, value.toString());
		}
		return this;
	}

	/**
	 * Adds a less-than predicate only if value is non-null.
	 */
	public FilterBuilder ltIfPresent(String field, Object value) {
		if (value != null) {
			lt(field, value.toString());
		}
		return this;
	}

	/**
	 * Adds a less-than-or-equal predicate only if value is non-null.
	 */
	public FilterBuilder lteIfPresent(String field, Object value) {
		if (value != null) {
			lte(field, value.toString());
		}
		return this;
	}

	/**
	 * Adds a LIKE predicate only if value is non-null and non-blank.
	 */
	public FilterBuilder likeIfPresent(String field, String value) {
		if (value != null && !value.isBlank()) {
			like(field, value);
		}
		return this;
	}

	/**
	 * Adds a case-insensitive LIKE predicate only if value is non-null and non-blank.
	 */
	public FilterBuilder iLikeIfPresent(String field, String value) {
		if (value != null && !value.isBlank()) {
			iLike(field, value);
		}
		return this;
	}

	/**
	 * Adds a starts-with predicate only if value is non-null and non-blank.
	 */
	public FilterBuilder startWithIfPresent(String field, String value) {
		if (value != null && !value.isBlank()) {
			startWith(field, value);
		}
		return this;
	}

	/**
	 * Adds an ends-with predicate only if value is non-null and non-blank.
	 */
	public FilterBuilder endWithIfPresent(String field, String value) {
		if (value != null && !value.isBlank()) {
			endWith(field, value);
		}
		return this;
	}

	/**
	 * Adds an IN predicate only if values array is non-null and non-empty.
	 */
	public FilterBuilder inIfPresent(String field, String... values) {
		if (values != null && values.length > 0) {
			in(field, values);
		}
		return this;
	}

	/**
	 * Adds a NOT IN predicate only if values array is non-null and non-empty.
	 */
	public FilterBuilder notInIfPresent(String field, String... values) {
		if (values != null && values.length > 0) {
			notIn(field, values);
		}
		return this;
	}

	// ==================== NESTING ====================

	public FilterBuilder and(FilterBuilder nested) {
		predicates.add(
				new Group(
						LogicalOperator.AND,
						List.copyOf(nested.predicates)
				)
		);
		return this;
	}

	public FilterBuilder or(FilterBuilder nested) {
		predicates.add(new Group(
				LogicalOperator.OR,
				List.copyOf(nested.predicates)
		));
		return this;
	}

	/**
	 * Include a pre-built raw RSQL string as a predicate.
	 */
	public FilterBuilder raw(String filter) {
		if (Objects.nonNull(filter) && !filter.isBlank()) {
			predicates.add(new Raw(filter));
		}
		return this;
	}

	// ==================== BUILD ====================

	public String build() {
		return new Group(operator, List.copyOf(predicates)).toRsql();
	}

	@Override
	public String toFilter() {
		return build();
	}

	// ==================== INTERNAL ====================

	private static String mapToJson(Map<String, Object> map) {
		String entries = map.entrySet().stream()
				.map(e -> "\"" + escapeJson(e.getKey()) + "\":" + toJsonValue(e.getValue()))
				.collect(Collectors.joining(","));
		return "{" + entries + "}";
	}

	private static String toJsonValue(Object value) {
		if (value == null) {
			return "null";
		}
		if (value instanceof Boolean || value instanceof Number) {
			return value.toString();
		}
		if (value instanceof Map<?, ?> nested) {
			Map<String, Object> typed = new LinkedHashMap<>();
			nested.forEach((k, v) -> typed.put(String.valueOf(k), v));
			return mapToJson(typed);
		}
		if (value instanceof Iterable<?> iterable) {
			List<String> elements = new ArrayList<>();
			for (Object item : iterable) {
				elements.add(toJsonValue(item));
			}
			return "[" + String.join(",", elements) + "]";
		}
		return "\"" + escapeJson(value.toString()) + "\"";
	}

	private static String escapeJson(String raw) {
		return raw.replace("\\", "\\\\").replace("\"", "\\\"");
	}
}
