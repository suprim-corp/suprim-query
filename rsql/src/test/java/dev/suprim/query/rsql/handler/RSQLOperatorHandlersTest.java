package dev.suprim.query.rsql.handler;

import cz.jirutka.rsql.parser.ast.RSQLOperators;
import dev.suprim.query.rsql.operators.CustomRSQLOperators;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for RSQLOperatorHandlers registry.
 */
class RSQLOperatorHandlersTest {

    @Test
    void constructor_shouldInstantiate() {
        // Cover the implicit constructor
        RSQLOperatorHandlers handlers = new RSQLOperatorHandlers();
        assertThat(handlers).isNotNull();
    }

    @Test
    void getOperatorHandler_equalOperator_shouldReturnEqualHandler() {
        OperatorHandler handler = RSQLOperatorHandlers.getOperatorHandler(RSQLOperators.EQUAL.getSymbol());

        assertThat(handler).isInstanceOf(EqualToOperatorHandler.class);
    }

    @Test
    void getOperatorHandler_notEqualOperator_shouldReturnNotEqualHandler() {
        OperatorHandler handler = RSQLOperatorHandlers.getOperatorHandler(RSQLOperators.NOT_EQUAL.getSymbol());

        assertThat(handler).isInstanceOf(NotEqualToOperatorHandler.class);
    }

    @Test
    void getOperatorHandler_inOperator_shouldReturnInHandler() {
        OperatorHandler handler = RSQLOperatorHandlers.getOperatorHandler(RSQLOperators.IN.getSymbol());

        assertThat(handler).isInstanceOf(InOperatorHandler.class);
    }

    @Test
    void getOperatorHandler_notInOperator_shouldReturnNotInHandler() {
        OperatorHandler handler = RSQLOperatorHandlers.getOperatorHandler(RSQLOperators.NOT_IN.getSymbol());

        assertThat(handler).isInstanceOf(NotInOperatorHandler.class);
    }

    @Test
    void getOperatorHandler_greaterThanOperator_shouldReturnGreaterThanHandler() {
        OperatorHandler handler = RSQLOperatorHandlers.getOperatorHandler(RSQLOperators.GREATER_THAN.getSymbol());

        assertThat(handler).isInstanceOf(GreaterThanOperatorHandler.class);
    }

    @Test
    void getOperatorHandler_greaterThanEqualOperator_shouldReturnGreaterThanEqualHandler() {
        OperatorHandler handler = RSQLOperatorHandlers.getOperatorHandler(RSQLOperators.GREATER_THAN_OR_EQUAL.getSymbol());

        assertThat(handler).isInstanceOf(GreaterThanEqualToOperatorHandler.class);
    }

    @Test
    void getOperatorHandler_lessThanOperator_shouldReturnLessThanHandler() {
        OperatorHandler handler = RSQLOperatorHandlers.getOperatorHandler(RSQLOperators.LESS_THAN.getSymbol());

        assertThat(handler).isInstanceOf(LessThanOperatorHandler.class);
    }

    @Test
    void getOperatorHandler_lessThanEqualOperator_shouldReturnLessThanEqualHandler() {
        OperatorHandler handler = RSQLOperatorHandlers.getOperatorHandler(RSQLOperators.LESS_THAN_OR_EQUAL.getSymbol());

        assertThat(handler).isInstanceOf(LessThanEqualToOperatorHandler.class);
    }

    @Test
    void getOperatorHandler_likeOperator_shouldReturnLikeHandler() {
        OperatorHandler handler = RSQLOperatorHandlers.getOperatorHandler(CustomRSQLOperators.LIKE.getSymbol());

        assertThat(handler).isInstanceOf(LikeOperatorHandler.class);
    }

    @Test
    void getOperatorHandler_ilikeOperator_shouldReturnILikeHandler() {
        OperatorHandler handler = RSQLOperatorHandlers.getOperatorHandler(CustomRSQLOperators.ILIKE.getSymbol());

        assertThat(handler).isInstanceOf(ILikeOperatorHandler.class);
    }

    @Test
    void getOperatorHandler_startWithOperator_shouldReturnStartWithHandler() {
        OperatorHandler handler = RSQLOperatorHandlers.getOperatorHandler(CustomRSQLOperators.START_WITH.getSymbol());

        assertThat(handler).isInstanceOf(StartWithOperatorHandler.class);
    }

    @Test
    void getOperatorHandler_endWithOperator_shouldReturnEndWithHandler() {
        OperatorHandler handler = RSQLOperatorHandlers.getOperatorHandler(CustomRSQLOperators.END_WITH.getSymbol());

        assertThat(handler).isInstanceOf(EndWithOperatorHandler.class);
    }

    @Test
    void getOperatorHandler_isNullOperator_shouldReturnIsNullHandler() {
        OperatorHandler handler = RSQLOperatorHandlers.getOperatorHandler(CustomRSQLOperators.IS_NULL.getSymbol());

        assertThat(handler).isInstanceOf(IsNullOperatorHandler.class);
    }

    @Test
    void getOperatorHandler_notNullOperator_shouldReturnIsNotNullHandler() {
        OperatorHandler handler = RSQLOperatorHandlers.getOperatorHandler(CustomRSQLOperators.NOT_NULL.getSymbol());

        assertThat(handler).isInstanceOf(IsNotNullOperatorHandler.class);
    }

    @Test
    void getOperatorHandler_jsonbContainOperator_shouldReturnJsonbContainHandler() {
        OperatorHandler handler = RSQLOperatorHandlers.getOperatorHandler(CustomRSQLOperators.JSONB_CONTAIN.getSymbol());

        assertThat(handler).isInstanceOf(JsonbContainOperatorHandler.class);
    }

    @Test
    void getOperatorHandler_jsonContainOperator_shouldReturnJsonContainHandler() {
        OperatorHandler handler = RSQLOperatorHandlers.getOperatorHandler(CustomRSQLOperators.JSON_CONTAIN.getSymbol());

        assertThat(handler).isInstanceOf(JsonContainOperatorHandler.class);
    }

    @Test
    void getOperatorHandler_jsonbEqualOperator_shouldReturnJsonbEqualHandler() {
        OperatorHandler handler = RSQLOperatorHandlers.getOperatorHandler(CustomRSQLOperators.JSONB_EQUAL.getSymbol());

        assertThat(handler).isInstanceOf(JsonbEqualToOperatorHandler.class);
    }

    @Test
    void getOperatorHandler_jsonbKeyExistsOperator_shouldReturnJsonbKeyExistsHandler() {
        OperatorHandler handler = RSQLOperatorHandlers.getOperatorHandler(CustomRSQLOperators.JSONB_KEY_EXISTS.getSymbol());

        assertThat(handler).isInstanceOf(JsonbKeyExistsOperatorHandler.class);
    }

    @Test
    void getOperatorHandler_jsonContainsInArrayOperator_shouldReturnJsonContainInArrayHandler() {
        OperatorHandler handler = RSQLOperatorHandlers.getOperatorHandler(CustomRSQLOperators.JSON_CONTAINS_IN_ARRAY.getSymbol());

        assertThat(handler).isInstanceOf(JsonContainInArrayOperatorHandler.class);
    }

    @Test
    void getOperatorHandler_notLikeOperator_shouldReturnNotLikeHandler() {
        OperatorHandler handler = RSQLOperatorHandlers.getOperatorHandler(CustomRSQLOperators.NOT_LIKE.getSymbol());

        assertThat(handler).isInstanceOf(NotLikeOperatorHandler.class);
    }

    @Test
    void getOperatorHandler_jsonbArrowOperator_shouldReturnJsonbArrowHandler() {
        OperatorHandler handler = RSQLOperatorHandlers.getOperatorHandler(CustomRSQLOperators.JSONB_ARROW.getSymbol());

        assertThat(handler).isInstanceOf(JsonbArrowOperatorHandler.class);
    }

    @Test
    void getOperatorHandler_unknownOperator_shouldReturnNull() {
        OperatorHandler handler = RSQLOperatorHandlers.getOperatorHandler("=unknown=");

        assertThat(handler).isNull();
    }
}
