package dev.suprim.query.rsql.builder;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JoinOperatorTest {

	@Test
	void eq_shouldHaveCorrectSymbol() {
		assertThat(JoinOperator.EQ.symbol()).isEqualTo("==");
	}

	@Test
	void gt_shouldHaveCorrectSymbol() {
		assertThat(JoinOperator.GT.symbol()).isEqualTo("=gt=");
	}

	@Test
	void gte_shouldHaveCorrectSymbol() {
		assertThat(JoinOperator.GTE.symbol()).isEqualTo("=gte=");
	}

	@Test
	void lt_shouldHaveCorrectSymbol() {
		assertThat(JoinOperator.LT.symbol()).isEqualTo("=lt=");
	}

	@Test
	void lte_shouldHaveCorrectSymbol() {
		assertThat(JoinOperator.LTE.symbol()).isEqualTo("=lte=");
	}

	@Test
	void isNull_shouldHaveCorrectSymbol() {
		assertThat(JoinOperator.IS_NULL.symbol()).isEqualTo("=isnull=");
	}

	@Test
	void isNotNull_shouldHaveCorrectSymbol() {
		assertThat(JoinOperator.IS_NOT_NULL.symbol()).isEqualTo("=notnull=");
	}

	@Test
	void values_shouldContainAllOperators() {
		assertThat(JoinOperator.values()).hasSize(7);
	}
}
