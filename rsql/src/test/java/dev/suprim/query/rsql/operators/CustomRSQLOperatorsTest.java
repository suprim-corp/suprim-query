package dev.suprim.query.rsql.operators;

import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import cz.jirutka.rsql.parser.ast.RSQLOperators;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for CustomRSQLOperators.
 */
class CustomRSQLOperatorsTest {

    @Test
    void constructor_shouldInstantiate() {
        // Cover the implicit constructor
        CustomRSQLOperators operators = new CustomRSQLOperators();
        assertThat(operators).isNotNull();
    }

    @Test
    void like_shouldHaveCorrectSymbol() {
        assertThat(CustomRSQLOperators.LIKE.getSymbol()).isEqualTo("=like=");
        assertThat(CustomRSQLOperators.LIKE.isMultiValue()).isFalse();
    }

    @Test
    void ilike_shouldHaveCorrectSymbol() {
        assertThat(CustomRSQLOperators.ILIKE.getSymbol()).isEqualTo("=ilike=");
        assertThat(CustomRSQLOperators.ILIKE.isMultiValue()).isFalse();
    }

    @Test
    void startWith_shouldHaveCorrectSymbol() {
        assertThat(CustomRSQLOperators.START_WITH.getSymbol()).isEqualTo("=startWith=");
        assertThat(CustomRSQLOperators.START_WITH.isMultiValue()).isFalse();
    }

    @Test
    void endWith_shouldHaveCorrectSymbol() {
        assertThat(CustomRSQLOperators.END_WITH.getSymbol()).isEqualTo("=endWith=");
        assertThat(CustomRSQLOperators.END_WITH.isMultiValue()).isFalse();
    }

    @Test
    void isNull_shouldHaveMultipleSymbols() {
        assertThat(CustomRSQLOperators.IS_NULL.getSymbols()).contains("=isnull=", "=na=");
        assertThat(CustomRSQLOperators.IS_NULL.isMultiValue()).isFalse();
    }

    @Test
    void notNull_shouldHaveMultipleSymbols() {
        assertThat(CustomRSQLOperators.NOT_NULL.getSymbols()).contains("=nn=", "=isnotnull=");
        assertThat(CustomRSQLOperators.NOT_NULL.isMultiValue()).isFalse();
    }

    @Test
    void jsonbContain_shouldHaveMultipleSymbols() {
        assertThat(CustomRSQLOperators.JSONB_CONTAIN.getSymbols()).contains("=jsonbContain=", "=jbc=");
        assertThat(CustomRSQLOperators.JSONB_CONTAIN.isMultiValue()).isFalse();
    }

    @Test
    void jsonContain_shouldHaveMultipleSymbols() {
        assertThat(CustomRSQLOperators.JSON_CONTAIN.getSymbols()).contains("=jsonContain=", "=jc=");
        assertThat(CustomRSQLOperators.JSON_CONTAIN.isMultiValue()).isFalse();
    }

    @Test
    void jsonbEqual_shouldHaveCorrectSymbol() {
        assertThat(CustomRSQLOperators.JSONB_EQUAL.getSymbols()).contains("=jbe=");
        assertThat(CustomRSQLOperators.JSONB_EQUAL.isMultiValue()).isFalse();
    }

    @Test
    void jsonbKeyExists_shouldHaveCorrectSymbol() {
        assertThat(CustomRSQLOperators.JSONB_KEY_EXISTS.getSymbols()).contains("=jbKeyExist=");
        assertThat(CustomRSQLOperators.JSONB_KEY_EXISTS.isMultiValue()).isFalse();
    }

    @Test
    void jsonContainsInArray_shouldHaveCorrectSymbol() {
        assertThat(CustomRSQLOperators.JSON_CONTAINS_IN_ARRAY.getSymbols()).contains("=jcInArray=");
        assertThat(CustomRSQLOperators.JSON_CONTAINS_IN_ARRAY.isMultiValue()).isFalse();
    }

    @Test
    void notLike_shouldHaveMultipleSymbols() {
        assertThat(CustomRSQLOperators.NOT_LIKE.getSymbols()).contains("=notlike=", "=nk=");
        assertThat(CustomRSQLOperators.NOT_LIKE.isMultiValue()).isFalse();
    }

    @Test
    void jsonbArrow_shouldHaveCorrectSymbol() {
        assertThat(CustomRSQLOperators.JSONB_ARROW.getSymbols()).contains("=jba=");
        assertThat(CustomRSQLOperators.JSONB_ARROW.isMultiValue()).isFalse();
    }

    @Test
    void customOperators_shouldIncludeAllDefaultOperators() {
        Set<ComparisonOperator> customOps = CustomRSQLOperators.customOperators();
        Set<ComparisonOperator> defaultOps = RSQLOperators.defaultOperators();

        assertThat(customOps).containsAll(defaultOps);
    }

    @Test
    void customOperators_shouldIncludeAllCustomOperators() {
        Set<ComparisonOperator> customOps = CustomRSQLOperators.customOperators();

        assertThat(customOps).contains(
                CustomRSQLOperators.LIKE,
                CustomRSQLOperators.ILIKE,
                CustomRSQLOperators.START_WITH,
                CustomRSQLOperators.END_WITH,
                CustomRSQLOperators.IS_NULL,
                CustomRSQLOperators.NOT_NULL,
                CustomRSQLOperators.JSONB_CONTAIN,
                CustomRSQLOperators.JSON_CONTAIN,
                CustomRSQLOperators.JSONB_EQUAL,
                CustomRSQLOperators.JSONB_KEY_EXISTS,
                CustomRSQLOperators.JSON_CONTAINS_IN_ARRAY,
                CustomRSQLOperators.NOT_LIKE,
                CustomRSQLOperators.JSONB_ARROW
        );
    }

    @Test
    void customOperators_shouldHaveCorrectSize() {
        Set<ComparisonOperator> customOps = CustomRSQLOperators.customOperators();
        Set<ComparisonOperator> defaultOps = RSQLOperators.defaultOperators();

        // 13 custom operators + default operators
        assertThat(customOps).hasSize(defaultOps.size() + 13);
    }
}
