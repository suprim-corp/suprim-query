package dev.suprim.query.rsql.builder;

import cz.jirutka.rsql.parser.ast.RSQLOperators;
import dev.suprim.query.rsql.operators.CustomRSQLOperators;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
public class FilterBuilder {

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
		predicates.add(new Comparison(
				field,
				RSQLOperators.EQUAL.getSymbol(),
				List.of(value)
		));
		return this;
	}

	public FilterBuilder neq(String field, String value) {
		predicates.add(new Comparison(
				field,
				RSQLOperators.NOT_EQUAL.getSymbol(),
				List.of(value)
		));
		return this;
	}

	public FilterBuilder gt(String field, String value) {
		predicates.add(new Comparison(
				field,
				RSQLOperators.GREATER_THAN.getSymbol(),
				List.of(value)
		));
		return this;
	}

	public FilterBuilder gte(String field, String value) {
		predicates.add(new Comparison(
				field,
				RSQLOperators.GREATER_THAN_OR_EQUAL.getSymbol(),
				List.of(value)
		));
		return this;
	}

	public FilterBuilder lt(String field, String value) {
		predicates.add(new Comparison(
				field,
				RSQLOperators.LESS_THAN.getSymbol(),
				List.of(value)
		));
		return this;
	}

	public FilterBuilder lte(String field, String value) {
		predicates.add(new Comparison(
				field,
				RSQLOperators.LESS_THAN_OR_EQUAL.getSymbol(),
				List.of(value)
		));
		return this;
	}

	public FilterBuilder in(String field, String... values) {
		predicates.add(new Comparison(
				field,
				RSQLOperators.IN.getSymbol(),
				Arrays.asList(values)
		));
		return this;
	}

	public FilterBuilder notIn(String field, String... values) {
		predicates.add(new Comparison(
				field,
				RSQLOperators.NOT_IN.getSymbol(),
				Arrays.asList(values)
		));
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

	public FilterBuilder ilike(String field, String value) {
		predicates.add(new Comparison(
				field,
				CustomRSQLOperators.ILIKE.getSymbol(),
				List.of(value)
		));
		return this;
	}

	public FilterBuilder startWith(String field, String value) {
		predicates.add(new Comparison(
				field,
				CustomRSQLOperators.START_WITH.getSymbol(),
				List.of(value)
		));
		return this;
	}

	public FilterBuilder endWith(String field, String value) {
		predicates.add(new Comparison(
				field,
				CustomRSQLOperators.END_WITH.getSymbol(),
				List.of(value)
		));
		return this;
	}

	public FilterBuilder isNull(String field) {
		predicates.add(new Comparison(
				field,
				CustomRSQLOperators.IS_NULL.getSymbol(),
				List.of("true")
		));
		return this;
	}

	public FilterBuilder isNotNull(String field) {
		predicates.add(new Comparison(
				field,
				CustomRSQLOperators.NOT_NULL.getSymbol(),
				List.of("true")
		));
		return this;
	}

	public FilterBuilder jsonbContains(String field, String value) {
		predicates.add(new Comparison(
				field,
				CustomRSQLOperators.JSONB_CONTAIN.getSymbol(),
				List.of(value)
		));
		return this;
	}

	public FilterBuilder jsonbKeyExists(String field, String key) {
		predicates.add(new Comparison(
				field,
				CustomRSQLOperators.JSONB_KEY_EXISTS.getSymbol(),
				List.of(key)
		));
		return this;
	}

	// ==================== NESTING ====================

	public FilterBuilder and(FilterBuilder nested) {
		predicates.add(new Group(
				LogicalOperator.AND,
				List.copyOf(nested.predicates)
		));
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
}
