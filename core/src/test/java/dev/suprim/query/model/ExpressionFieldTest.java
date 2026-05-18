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
}
