package dev.suprim.query.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExpressionFieldTest {

	@Test
	void raw_shouldCreateExpressionWithNoAlias() {
		ExpressionField expr = ExpressionField.raw("COUNT(*)");

		assertThat(expr.expression()).isEqualTo("COUNT(*)");
		assertThat(expr.alias()).isNull();
	}

	@Test
	void raw_withNull_shouldThrowException() {
		assertThatThrownBy(() -> ExpressionField.raw(null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Expression must not be null or blank");
	}

	@Test
	void raw_withBlank_shouldThrowException() {
		assertThatThrownBy(() -> ExpressionField.raw("   "))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Expression must not be null or blank");
	}

	@Test
	void raw_withEmpty_shouldThrowException() {
		assertThatThrownBy(() -> ExpressionField.raw(""))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Expression must not be null or blank");
	}

	@Test
	void as_shouldReturnNewInstanceWithAlias() {
		ExpressionField original = ExpressionField.raw("COUNT(*)");

		ExpressionField aliased = original.as("total");

		assertThat(aliased.expression()).isEqualTo("COUNT(*)");
		assertThat(aliased.alias()).isEqualTo("total");
		// original is unchanged
		assertThat(original.alias()).isNull();
	}

	@Test
	void as_withNull_shouldThrowException() {
		ExpressionField expr = ExpressionField.raw("COUNT(*)");

		assertThatThrownBy(() -> expr.as(null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Alias must not be null or blank");
	}

	@Test
	void as_withBlank_shouldThrowException() {
		ExpressionField expr = ExpressionField.raw("COUNT(*)");

		assertThatThrownBy(() -> expr.as("  "))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Alias must not be null or blank");
	}

	@Test
	void renderWithAlias_withAlias_shouldIncludeAsClause() {
		ExpressionField expr = ExpressionField.raw("COUNT(*)").as("total");

		String result = expr.renderWithAlias();

		assertThat(result).isEqualTo("COUNT(*) AS \"total\"");
	}

	@Test
	void renderWithAlias_withoutAlias_shouldReturnExpressionOnly() {
		ExpressionField expr = ExpressionField.raw("NOW()");

		String result = expr.renderWithAlias();

		assertThat(result).isEqualTo("NOW()");
	}

	@Test
	void renderWithAlias_withComplexExpression_shouldRenderCorrectly() {
		ExpressionField expr = ExpressionField.raw("COALESCE(u.name, 'unknown')").as("display_name");

		String result = expr.renderWithAlias();

		assertThat(result).isEqualTo("COALESCE(u.name, 'unknown') AS \"display_name\"");
	}

	@Test
	void as_canBeChained_shouldUseLastAlias() {
		ExpressionField expr = ExpressionField.raw("SUM(amount)")
				.as("first_alias")
				.as("final_alias");

		assertThat(expr.alias()).isEqualTo("final_alias");
		assertThat(expr.renderWithAlias()).isEqualTo("SUM(amount) AS \"final_alias\"");
	}

	@Test
	void constructor_withNullExpression_shouldThrowException() {
		assertThatThrownBy(() -> new ExpressionField(null, null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Expression must not be null or blank");
	}

	@Test
	void constructor_withBlankExpression_shouldThrowException() {
		assertThatThrownBy(() -> new ExpressionField("", "alias"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Expression must not be null or blank");
	}

	// === count() ===

	@Test
	void count_withStar_shouldNotQuoteColumn() {
		ExpressionField expr = ExpressionField.count("*");

		assertThat(expr.expression()).isEqualTo("COUNT(*)");
		assertThat(expr.alias()).isNull();
	}

	@Test
	void count_withColumn_shouldQuoteColumn() {
		ExpressionField expr = ExpressionField.count("id");

		assertThat(expr.expression()).isEqualTo("COUNT(\"id\")");
		assertThat(expr.alias()).isNull();
	}

	@Test
	void count_withNull_shouldThrowException() {
		assertThatThrownBy(() -> ExpressionField.count(null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Column must not be null or blank");
	}

	@Test
	void count_withBlank_shouldThrowException() {
		assertThatThrownBy(() -> ExpressionField.count("  "))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Column must not be null or blank");
	}

	@Test
	void count_withEmpty_shouldThrowException() {
		assertThatThrownBy(() -> ExpressionField.count(""))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Column must not be null or blank");
	}

	@Test
	void count_withAlias_shouldRenderCorrectly() {
		ExpressionField expr = ExpressionField.count("amount").as("total");

		assertThat(expr.renderWithAlias()).isEqualTo("COUNT(\"amount\") AS \"total\"");
	}

	// === countDistinct() ===

	@Test
	void countDistinct_shouldQuoteColumn() {
		ExpressionField expr = ExpressionField.countDistinct("status");

		assertThat(expr.expression()).isEqualTo("COUNT(DISTINCT \"status\")");
		assertThat(expr.alias()).isNull();
	}

	@Test
	void countDistinct_withNull_shouldThrowException() {
		assertThatThrownBy(() -> ExpressionField.countDistinct(null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Column must not be null or blank");
	}

	@Test
	void countDistinct_withBlank_shouldThrowException() {
		assertThatThrownBy(() -> ExpressionField.countDistinct("  "))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Column must not be null or blank");
	}

	@Test
	void countDistinct_withEmpty_shouldThrowException() {
		assertThatThrownBy(() -> ExpressionField.countDistinct(""))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Column must not be null or blank");
	}

	@Test
	void countDistinct_withAlias_shouldRenderCorrectly() {
		ExpressionField expr = ExpressionField.countDistinct("status").as("unique_statuses");

		assertThat(expr.renderWithAlias()).isEqualTo("COUNT(DISTINCT \"status\") AS \"unique_statuses\"");
	}

	// === sum() ===

	@Test
	void sum_shouldQuoteColumn() {
		ExpressionField expr = ExpressionField.sum("price");

		assertThat(expr.expression()).isEqualTo("SUM(\"price\")");
		assertThat(expr.alias()).isNull();
	}

	@Test
	void sum_withNull_shouldThrowException() {
		assertThatThrownBy(() -> ExpressionField.sum(null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Column must not be null or blank");
	}

	@Test
	void sum_withBlank_shouldThrowException() {
		assertThatThrownBy(() -> ExpressionField.sum("  "))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Column must not be null or blank");
	}

	@Test
	void sum_withEmpty_shouldThrowException() {
		assertThatThrownBy(() -> ExpressionField.sum(""))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Column must not be null or blank");
	}

	@Test
	void sum_withAlias_shouldRenderCorrectly() {
		ExpressionField expr = ExpressionField.sum("price").as("total_price");

		assertThat(expr.renderWithAlias()).isEqualTo("SUM(\"price\") AS \"total_price\"");
	}

	// === avg() ===

	@Test
	void avg_shouldQuoteColumn() {
		ExpressionField expr = ExpressionField.avg("score");

		assertThat(expr.expression()).isEqualTo("AVG(\"score\")");
		assertThat(expr.alias()).isNull();
	}

	@Test
	void avg_withNull_shouldThrowException() {
		assertThatThrownBy(() -> ExpressionField.avg(null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Column must not be null or blank");
	}

	@Test
	void avg_withBlank_shouldThrowException() {
		assertThatThrownBy(() -> ExpressionField.avg("  "))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Column must not be null or blank");
	}

	@Test
	void avg_withEmpty_shouldThrowException() {
		assertThatThrownBy(() -> ExpressionField.avg(""))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Column must not be null or blank");
	}

	@Test
	void avg_withAlias_shouldRenderCorrectly() {
		ExpressionField expr = ExpressionField.avg("score").as("avg_score");

		assertThat(expr.renderWithAlias()).isEqualTo("AVG(\"score\") AS \"avg_score\"");
	}

	// === max() ===

	@Test
	void max_shouldQuoteColumn() {
		ExpressionField expr = ExpressionField.max("created_at");

		assertThat(expr.expression()).isEqualTo("MAX(\"created_at\")");
		assertThat(expr.alias()).isNull();
	}

	@Test
	void max_withNull_shouldThrowException() {
		assertThatThrownBy(() -> ExpressionField.max(null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Column must not be null or blank");
	}

	@Test
	void max_withBlank_shouldThrowException() {
		assertThatThrownBy(() -> ExpressionField.max("  "))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Column must not be null or blank");
	}

	@Test
	void max_withEmpty_shouldThrowException() {
		assertThatThrownBy(() -> ExpressionField.max(""))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Column must not be null or blank");
	}

	@Test
	void max_withAlias_shouldRenderCorrectly() {
		ExpressionField expr = ExpressionField.max("created_at").as("latest");

		assertThat(expr.renderWithAlias()).isEqualTo("MAX(\"created_at\") AS \"latest\"");
	}

	// === min() ===

	@Test
	void min_shouldQuoteColumn() {
		ExpressionField expr = ExpressionField.min("id");

		assertThat(expr.expression()).isEqualTo("MIN(\"id\")");
		assertThat(expr.alias()).isNull();
	}

	@Test
	void min_withNull_shouldThrowException() {
		assertThatThrownBy(() -> ExpressionField.min(null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Column must not be null or blank");
	}

	@Test
	void min_withBlank_shouldThrowException() {
		assertThatThrownBy(() -> ExpressionField.min("  "))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Column must not be null or blank");
	}

	@Test
	void min_withEmpty_shouldThrowException() {
		assertThatThrownBy(() -> ExpressionField.min(""))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Column must not be null or blank");
	}

	@Test
	void min_withAlias_shouldRenderCorrectly() {
		ExpressionField expr = ExpressionField.min("id").as("smallest_id");

		assertThat(expr.renderWithAlias()).isEqualTo("MIN(\"id\") AS \"smallest_id\"");
	}
}
