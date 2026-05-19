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

	// === coalesce() ===

	@Test
	void coalesce_withMultipleArgs_shouldRenderCorrectly() {
		ExpressionField expr = ExpressionField.coalesce("u.name", "'unknown'");

		assertThat(expr.expression()).isEqualTo("COALESCE(u.name, 'unknown')");
		assertThat(expr.alias()).isNull();
	}

	@Test
	void coalesce_withSingleArg_shouldRenderCorrectly() {
		ExpressionField expr = ExpressionField.coalesce("u.name");

		assertThat(expr.expression()).isEqualTo("COALESCE(u.name)");
		assertThat(expr.alias()).isNull();
	}

	@Test
	void coalesce_withThreeArgs_shouldRenderCorrectly() {
		ExpressionField expr = ExpressionField.coalesce("a.value", "b.value", "0");

		assertThat(expr.expression()).isEqualTo("COALESCE(a.value, b.value, 0)");
		assertThat(expr.alias()).isNull();
	}

	@Test
	void coalesce_withNull_shouldThrowException() {
		assertThatThrownBy(() -> ExpressionField.coalesce((String[]) null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Arguments must not be null or empty");
	}

	@Test
	void coalesce_withEmptyArray_shouldThrowException() {
		assertThatThrownBy(() -> ExpressionField.coalesce())
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Arguments must not be null or empty");
	}

	@Test
	void coalesce_withNullElement_shouldThrowException() {
		assertThatThrownBy(() -> ExpressionField.coalesce("a.value", null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Argument at index 1 must not be null or blank");
	}

	@Test
	void coalesce_withBlankElement_shouldThrowException() {
		assertThatThrownBy(() -> ExpressionField.coalesce("a.value", "  "))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Argument at index 1 must not be null or blank");
	}

	@Test
	void coalesce_withEmptyElement_shouldThrowException() {
		assertThatThrownBy(() -> ExpressionField.coalesce(""))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Argument at index 0 must not be null or blank");
	}

	@Test
	void coalesce_withAlias_shouldRenderCorrectly() {
		ExpressionField expr = ExpressionField.coalesce("u.name", "'unknown'").as("display_name");

		assertThat(expr.renderWithAlias()).isEqualTo("COALESCE(u.name, 'unknown') AS \"display_name\"");
	}

	// === nullif() ===

	@Test
	void nullif_shouldRenderCorrectly() {
		ExpressionField expr = ExpressionField.nullif("u.status", "'inactive'");

		assertThat(expr.expression()).isEqualTo("NULLIF(u.status, 'inactive')");
		assertThat(expr.alias()).isNull();
	}

	@Test
	void nullif_withNumericValue_shouldRenderCorrectly() {
		ExpressionField expr = ExpressionField.nullif("amount", "0");

		assertThat(expr.expression()).isEqualTo("NULLIF(amount, 0)");
		assertThat(expr.alias()).isNull();
	}

	@Test
	void nullif_withNullExpr_shouldThrowException() {
		assertThatThrownBy(() -> ExpressionField.nullif(null, "'value'"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Expression argument must not be null or blank");
	}

	@Test
	void nullif_withBlankExpr_shouldThrowException() {
		assertThatThrownBy(() -> ExpressionField.nullif("  ", "'value'"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Expression argument must not be null or blank");
	}

	@Test
	void nullif_withEmptyExpr_shouldThrowException() {
		assertThatThrownBy(() -> ExpressionField.nullif("", "'value'"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Expression argument must not be null or blank");
	}

	@Test
	void nullif_withNullValue_shouldThrowException() {
		assertThatThrownBy(() -> ExpressionField.nullif("u.status", null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Value argument must not be null or blank");
	}

	@Test
	void nullif_withBlankValue_shouldThrowException() {
		assertThatThrownBy(() -> ExpressionField.nullif("u.status", "  "))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Value argument must not be null or blank");
	}

	@Test
	void nullif_withEmptyValue_shouldThrowException() {
		assertThatThrownBy(() -> ExpressionField.nullif("u.status", ""))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Value argument must not be null or blank");
	}

	@Test
	void nullif_withAlias_shouldRenderCorrectly() {
		ExpressionField expr = ExpressionField.nullif("amount", "0").as("safe_amount");

		assertThat(expr.renderWithAlias()).isEqualTo("NULLIF(amount, 0) AS \"safe_amount\"");
	}

	// === dateTrunc() ===

	@Test
	void dateTrunc_shouldQuotePrecisionAndColumn() {
		ExpressionField expr = ExpressionField.dateTrunc("month", "created_at");

		assertThat(expr.expression()).isEqualTo("DATE_TRUNC('month', \"created_at\")");
		assertThat(expr.alias()).isNull();
	}

	@Test
	void dateTrunc_withDayPrecision_shouldRenderCorrectly() {
		ExpressionField expr = ExpressionField.dateTrunc("day", "updated_at");

		assertThat(expr.expression()).isEqualTo("DATE_TRUNC('day', \"updated_at\")");
	}

	@Test
	void dateTrunc_withNullPrecision_shouldThrowException() {
		assertThatThrownBy(() -> ExpressionField.dateTrunc(null, "created_at"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Precision must not be null or blank");
	}

	@Test
	void dateTrunc_withBlankPrecision_shouldThrowException() {
		assertThatThrownBy(() -> ExpressionField.dateTrunc("  ", "created_at"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Precision must not be null or blank");
	}

	@Test
	void dateTrunc_withEmptyPrecision_shouldThrowException() {
		assertThatThrownBy(() -> ExpressionField.dateTrunc("", "created_at"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Precision must not be null or blank");
	}

	@Test
	void dateTrunc_withNullColumn_shouldThrowException() {
		assertThatThrownBy(() -> ExpressionField.dateTrunc("month", null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Column must not be null or blank");
	}

	@Test
	void dateTrunc_withBlankColumn_shouldThrowException() {
		assertThatThrownBy(() -> ExpressionField.dateTrunc("month", "  "))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Column must not be null or blank");
	}

	@Test
	void dateTrunc_withEmptyColumn_shouldThrowException() {
		assertThatThrownBy(() -> ExpressionField.dateTrunc("month", ""))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Column must not be null or blank");
	}

	@Test
	void dateTrunc_withAlias_shouldRenderCorrectly() {
		ExpressionField expr = ExpressionField.dateTrunc("year", "created_at").as("truncated");

		assertThat(expr.renderWithAlias()).isEqualTo("DATE_TRUNC('year', \"created_at\") AS \"truncated\"");
	}

	// === extract() ===

	@Test
	void extract_shouldRenderFieldAndQuoteSource() {
		ExpressionField expr = ExpressionField.extract("YEAR", "created_at");

		assertThat(expr.expression()).isEqualTo("EXTRACT(YEAR FROM \"created_at\")");
		assertThat(expr.alias()).isNull();
	}

	@Test
	void extract_withMonth_shouldRenderCorrectly() {
		ExpressionField expr = ExpressionField.extract("MONTH", "birth_date");

		assertThat(expr.expression()).isEqualTo("EXTRACT(MONTH FROM \"birth_date\")");
	}

	@Test
	void extract_withDay_shouldRenderCorrectly() {
		ExpressionField expr = ExpressionField.extract("DAY", "event_date");

		assertThat(expr.expression()).isEqualTo("EXTRACT(DAY FROM \"event_date\")");
	}

	@Test
	void extract_withHour_shouldRenderCorrectly() {
		ExpressionField expr = ExpressionField.extract("HOUR", "logged_at");

		assertThat(expr.expression()).isEqualTo("EXTRACT(HOUR FROM \"logged_at\")");
	}

	@Test
	void extract_withNullField_shouldThrowException() {
		assertThatThrownBy(() -> ExpressionField.extract(null, "created_at"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Field must not be null or blank");
	}

	@Test
	void extract_withBlankField_shouldThrowException() {
		assertThatThrownBy(() -> ExpressionField.extract("  ", "created_at"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Field must not be null or blank");
	}

	@Test
	void extract_withEmptyField_shouldThrowException() {
		assertThatThrownBy(() -> ExpressionField.extract("", "created_at"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Field must not be null or blank");
	}

	@Test
	void extract_withNullSource_shouldThrowException() {
		assertThatThrownBy(() -> ExpressionField.extract("YEAR", null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Source must not be null or blank");
	}

	@Test
	void extract_withBlankSource_shouldThrowException() {
		assertThatThrownBy(() -> ExpressionField.extract("YEAR", "  "))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Source must not be null or blank");
	}

	@Test
	void extract_withEmptySource_shouldThrowException() {
		assertThatThrownBy(() -> ExpressionField.extract("YEAR", ""))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Source must not be null or blank");
	}

	@Test
	void extract_withAlias_shouldRenderCorrectly() {
		ExpressionField expr = ExpressionField.extract("MONTH", "created_at").as("birth_month");

		assertThat(expr.renderWithAlias()).isEqualTo("EXTRACT(MONTH FROM \"created_at\") AS \"birth_month\"");
	}

	// === concat() ===

	@Test
	void concat_withMultipleArgs_shouldRenderCorrectly() {
		ExpressionField expr = ExpressionField.concat("first_name", "' '", "last_name");

		assertThat(expr.expression()).isEqualTo("CONCAT(first_name, ' ', last_name)");
		assertThat(expr.alias()).isNull();
	}

	@Test
	void concat_withSingleArg_shouldRenderCorrectly() {
		ExpressionField expr = ExpressionField.concat("u.name");

		assertThat(expr.expression()).isEqualTo("CONCAT(u.name)");
		assertThat(expr.alias()).isNull();
	}

	@Test
	void concat_withTwoArgs_shouldRenderCorrectly() {
		ExpressionField expr = ExpressionField.concat("u.first_name", "u.last_name");

		assertThat(expr.expression()).isEqualTo("CONCAT(u.first_name, u.last_name)");
		assertThat(expr.alias()).isNull();
	}

	@Test
	void concat_withNull_shouldThrowException() {
		assertThatThrownBy(() -> ExpressionField.concat((String[]) null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Arguments must not be null or empty");
	}

	@Test
	void concat_withEmptyArray_shouldThrowException() {
		assertThatThrownBy(() -> ExpressionField.concat())
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Arguments must not be null or empty");
	}

	@Test
	void concat_withNullElement_shouldThrowException() {
		assertThatThrownBy(() -> ExpressionField.concat("a.value", null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Argument at index 1 must not be null or blank");
	}

	@Test
	void concat_withBlankElement_shouldThrowException() {
		assertThatThrownBy(() -> ExpressionField.concat("a.value", "  "))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Argument at index 1 must not be null or blank");
	}

	@Test
	void concat_withEmptyElement_shouldThrowException() {
		assertThatThrownBy(() -> ExpressionField.concat(""))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Argument at index 0 must not be null or blank");
	}

	@Test
	void concat_withAlias_shouldRenderCorrectly() {
		ExpressionField expr = ExpressionField.concat("first_name", "' '", "last_name").as("full_name");

		assertThat(expr.renderWithAlias()).isEqualTo("CONCAT(first_name, ' ', last_name) AS \"full_name\"");
	}

	// === toChar() ===

	@Test
	void toChar_shouldQuoteColumnAndWrapFormat() {
		ExpressionField expr = ExpressionField.toChar("created_at", "YYYY-MM-DD");

		assertThat(expr.expression()).isEqualTo("TO_CHAR(\"created_at\", 'YYYY-MM-DD')");
		assertThat(expr.alias()).isNull();
	}

	@Test
	void toChar_withTimeFormat_shouldRenderCorrectly() {
		ExpressionField expr = ExpressionField.toChar("logged_at", "HH24:MI:SS");

		assertThat(expr.expression()).isEqualTo("TO_CHAR(\"logged_at\", 'HH24:MI:SS')");
	}

	@Test
	void toChar_withFullTimestampFormat_shouldRenderCorrectly() {
		ExpressionField expr = ExpressionField.toChar("event_time", "YYYY-MM-DD HH24:MI:SS");

		assertThat(expr.expression()).isEqualTo("TO_CHAR(\"event_time\", 'YYYY-MM-DD HH24:MI:SS')");
	}

	@Test
	void toChar_withNullColumn_shouldThrowException() {
		assertThatThrownBy(() -> ExpressionField.toChar(null, "YYYY-MM-DD"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Column must not be null or blank");
	}

	@Test
	void toChar_withBlankColumn_shouldThrowException() {
		assertThatThrownBy(() -> ExpressionField.toChar("  ", "YYYY-MM-DD"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Column must not be null or blank");
	}

	@Test
	void toChar_withEmptyColumn_shouldThrowException() {
		assertThatThrownBy(() -> ExpressionField.toChar("", "YYYY-MM-DD"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Column must not be null or blank");
	}

	@Test
	void toChar_withNullFormat_shouldThrowException() {
		assertThatThrownBy(() -> ExpressionField.toChar("created_at", null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Format must not be null or blank");
	}

	@Test
	void toChar_withBlankFormat_shouldThrowException() {
		assertThatThrownBy(() -> ExpressionField.toChar("created_at", "  "))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Format must not be null or blank");
	}

	@Test
	void toChar_withEmptyFormat_shouldThrowException() {
		assertThatThrownBy(() -> ExpressionField.toChar("created_at", ""))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Format must not be null or blank");
	}

	@Test
	void toChar_withAlias_shouldRenderCorrectly() {
		ExpressionField expr = ExpressionField.toChar("created_at", "YYYY-MM-DD").as("date_str");

		assertThat(expr.renderWithAlias()).isEqualTo("TO_CHAR(\"created_at\", 'YYYY-MM-DD') AS \"date_str\"");
	}
}
