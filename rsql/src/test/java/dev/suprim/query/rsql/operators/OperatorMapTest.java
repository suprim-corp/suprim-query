package dev.suprim.query.rsql.operators;

import dev.suprim.query.exception.DbException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for OperatorMap.
 */
class OperatorMapTest {

    @Test
    void constructor_shouldInstantiate() {
        // Cover the implicit constructor
        OperatorMap map = new OperatorMap();
        assertThat(map).isNotNull();
    }

    @Test
    void getSQLOperator_equal_shouldReturnEquals() {
        String result = OperatorMap.getSQLOperator("==");

        assertThat(result).isEqualTo("=");
    }

    @Test
    void getSQLOperator_greaterThan_shouldReturnGreaterThan() {
        String result = OperatorMap.getSQLOperator("=gt=");

        assertThat(result).isEqualTo(">");
    }

    @Test
    void getSQLOperator_greaterThanOrEqual_shouldReturnGreaterThanOrEqual() {
        String result = OperatorMap.getSQLOperator("=gte=");

        assertThat(result).isEqualTo(">=");
    }

    @Test
    void getSQLOperator_lessThan_shouldReturnLessThan() {
        String result = OperatorMap.getSQLOperator("=lt=");

        assertThat(result).isEqualTo("<");
    }

    @Test
    void getSQLOperator_lessThanOrEqual_shouldReturnLessThanOrEqual() {
        String result = OperatorMap.getSQLOperator("=lte=");

        assertThat(result).isEqualTo("<=");
    }

    @Test
    void getSQLOperator_notNull_shouldReturnIsNotNull() {
        String result = OperatorMap.getSQLOperator("=notnull=");

        assertThat(result).isEqualTo("IS NOT NULL");
    }

    @Test
    void getSQLOperator_isNull_shouldReturnIsNull() {
        String result = OperatorMap.getSQLOperator("=isnull=");

        assertThat(result).isEqualTo("IS NULL");
    }

    @Test
    void getSQLOperator_unknownOperator_shouldReturnNull() {
        String result = OperatorMap.getSQLOperator("=unknown=");

        assertThat(result).isNull();
    }

    @Test
    void getRSQLOperator_containsEqual_shouldReturnEqualOperator() throws DbException {
        String result = OperatorMap.getRSQLOperator("name==john");

        assertThat(result).isEqualTo("==");
    }

    @Test
    void getRSQLOperator_containsGreaterThan_shouldReturnGreaterThanOperator() throws DbException {
        String result = OperatorMap.getRSQLOperator("age=gt=18");

        assertThat(result).isEqualTo("=gt=");
    }

    @Test
    void getRSQLOperator_containsGreaterThanOrEqual_shouldReturnGreaterThanOrEqualOperator() throws DbException {
        String result = OperatorMap.getRSQLOperator("age=gte=18");

        assertThat(result).isEqualTo("=gte=");
    }

    @Test
    void getRSQLOperator_containsLessThan_shouldReturnLessThanOperator() throws DbException {
        String result = OperatorMap.getRSQLOperator("age=lt=65");

        assertThat(result).isEqualTo("=lt=");
    }

    @Test
    void getRSQLOperator_containsLessThanOrEqual_shouldReturnLessThanOrEqualOperator() throws DbException {
        String result = OperatorMap.getRSQLOperator("age=lte=65");

        assertThat(result).isEqualTo("=lte=");
    }

    @Test
    void getRSQLOperator_containsNotNull_shouldReturnNotNullOperator() throws DbException {
        String result = OperatorMap.getRSQLOperator("name=notnull=true");

        assertThat(result).isEqualTo("=notnull=");
    }

    @Test
    void getRSQLOperator_containsIsNull_shouldReturnIsNullOperator() throws DbException {
        String result = OperatorMap.getRSQLOperator("name=isnull=true");

        assertThat(result).isEqualTo("=isnull=");
    }

    @Test
    void getRSQLOperator_caseInsensitive_shouldWork() throws DbException {
        String result = OperatorMap.getRSQLOperator("NAME=GT=18");

        assertThat(result).isEqualTo("=gt=");
    }

    @Test
    void getRSQLOperator_unknownOperator_shouldThrowException() {
        assertThatThrownBy(() -> OperatorMap.getRSQLOperator("name=unknown=value"))
                .isInstanceOf(DbException.class)
                .hasMessageContaining("Operator not supported");
    }

    // Prefix ambiguity regression tests — =gt= is a substring of =gte=
    @Test
    void getRSQLOperator_gteExpression_shouldNotMatchGt() throws DbException {
        // =gte= contains =gt= as substring — longer operator must win
        String result = OperatorMap.getRSQLOperator("age=gte=18");

        assertThat(result).isEqualTo("=gte=");
    }

    @Test
    void getRSQLOperator_lteExpression_shouldNotMatchLt() throws DbException {
        // =lte= contains =lt= as substring — longer operator must win
        String result = OperatorMap.getRSQLOperator("age=lte=65");

        assertThat(result).isEqualTo("=lte=");
    }

    @Test
    void getRSQLOperator_notnullExpression_shouldNotMatchIsNull() throws DbException {
        // =notnull= contains =null= pattern — must match exact longer operator
        String result = OperatorMap.getRSQLOperator("name=notnull=true");

        assertThat(result).isEqualTo("=notnull=");
    }
}
