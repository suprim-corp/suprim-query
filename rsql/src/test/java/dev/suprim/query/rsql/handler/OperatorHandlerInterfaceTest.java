package dev.suprim.query.rsql.handler;

import dev.suprim.query.exception.DbException;
import dev.suprim.query.model.DbColumn;
import dev.suprim.query.model.DbWhere;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for OperatorHandler interface default methods.
 */
class OperatorHandlerInterfaceTest extends OperatorHandlerTestBase {

    private TestDialect dialect;
    private DbColumn column;
    private DbWhere dbWhere;
    private Map<String, Object> paramMap;

    @BeforeEach
    void setUp() {
        dialect = createDialect();
        column = createColumn("status", "varchar", String.class);
        dbWhere = createDbWhere();
        paramMap = createParamMap();
    }

    @Test
    void reviewAndSetParam_withNewKey_shouldAddToMap() {
        // Use a concrete handler to test the default method
        EqualToOperatorHandler handler = new EqualToOperatorHandler();

        String result = handler.reviewAndSetParam("key", "value", paramMap);

        assertThat(result).isEqualTo("key");
        assertThat(paramMap).containsEntry("key", "value");
    }

    @Test
    void reviewAndSetParam_withExistingKey_shouldCreateNewKeyWithSuffix() {
        // Use a concrete handler to test the default method
        EqualToOperatorHandler handler = new EqualToOperatorHandler();
        paramMap.put("key", "existingValue");

        String result = handler.reviewAndSetParam("key", "newValue", paramMap);

        assertThat(result).isEqualTo("key_1");
        assertThat(paramMap).containsEntry("key", "existingValue");
        assertThat(paramMap).containsEntry("key_1", "newValue");
    }

    @Test
    void reviewAndSetParam_withSameKeyThreeTimes_shouldProduceDistinctKeys() {
        // Regression: random nextInt(20) could collide; counter-based suffix must not
        EqualToOperatorHandler handler = new EqualToOperatorHandler();

        String key1 = handler.reviewAndSetParam("name", "alice", paramMap);
        String key2 = handler.reviewAndSetParam("name", "bob", paramMap);
        String key3 = handler.reviewAndSetParam("name", "carol", paramMap);

        assertThat(key1).isEqualTo("name");
        assertThat(key2).isEqualTo("name_1");
        assertThat(key3).isEqualTo("name_2");
        assertThat(paramMap).containsEntry("name", "alice");
        assertThat(paramMap).containsEntry("name_1", "bob");
        assertThat(paramMap).containsEntry("name_2", "carol");
    }

    @Test
    void handleList_shouldDelegateToSingleValueHandle() throws DbException {
        // Test that default list handler delegates to single value handler
        // Use IsNullOperatorHandler which doesn't use the value
        IsNullOperatorHandler handler = new IsNullOperatorHandler();

        String result = handler.handle(dialect, column, dbWhere, List.of("ignored"), String.class, paramMap);

        assertThat(result).isEqualTo("t.status is null ");
    }

    @Test
    void prefix_shouldBeColon() {
        assertThat(OperatorHandler.PREFIX).isEqualTo(":");
    }
}
