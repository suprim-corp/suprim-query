package dev.suprim.query.autoconfigure;

import dev.suprim.query.jdbc.config.DatabaseConnectionDetail;
import dev.suprim.query.model.SoftDeleteProperties;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for DbProperties configuration.
 */
class DbPropertiesTest {

    @Test
    void constructor_shouldInstantiate() {
        DbProperties properties = new DbProperties();
        assertThat(properties).isNotNull();
    }

    @Test
    void setDatabases_withNullList_shouldNotSetDefaultDatabaseId() {
        DbProperties properties = new DbProperties();

        properties.setDatabases(null);

        assertThat(properties.getDatabases()).isNull();
        assertThat(properties.getDefaultDatabaseId()).isNull();
    }

    @Test
    void setDatabases_withEmptyList_shouldNotSetDefaultDatabaseId() {
        DbProperties properties = new DbProperties();

        properties.setDatabases(Collections.emptyList());

        assertThat(properties.getDatabases()).isEmpty();
        assertThat(properties.getDefaultDatabaseId()).isNull();
    }

    @Test
    void setDatabases_withNonEmptyList_shouldAutoSetDefaultDatabaseId() {
        DbProperties properties = new DbProperties();
        DatabaseConnectionDetail db1 = createConnectionDetail("primary-db");
        DatabaseConnectionDetail db2 = createConnectionDetail("secondary-db");

        properties.setDatabases(List.of(db1, db2));

        assertThat(properties.getDatabases()).hasSize(2);
        assertThat(properties.getDefaultDatabaseId()).isEqualTo("primary-db");
    }

    @Test
    void setDatabases_whenDefaultDatabaseIdAlreadySet_shouldNotOverride() {
        DbProperties properties = new DbProperties();
        properties.setDefaultDatabaseId("existing-db");
        DatabaseConnectionDetail db1 = createConnectionDetail("new-db");

        properties.setDatabases(List.of(db1));

        assertThat(properties.getDefaultDatabaseId()).isEqualTo("existing-db");
    }

    @Test
    void setDatabases_multipleCalls_shouldUpdateDatabases() {
        DbProperties properties = new DbProperties();
        DatabaseConnectionDetail db1 = createConnectionDetail("first-db");
        DatabaseConnectionDetail db2 = createConnectionDetail("second-db");

        properties.setDatabases(List.of(db1));
        assertThat(properties.getDefaultDatabaseId()).isEqualTo("first-db");

        // Second call should not override defaultDatabaseId
        properties.setDatabases(List.of(db2));
        assertThat(properties.getDatabases()).hasSize(1);
        assertThat(properties.getDatabases().get(0).id()).isEqualTo("second-db");
        assertThat(properties.getDefaultDatabaseId()).isEqualTo("first-db");
    }

    @Test
    void resolveSoftDeleteProperties_nullConfig_returnsDisabled() {
        DbProperties properties = new DbProperties();
        properties.setSoftDelete(null);

        SoftDeleteProperties result = properties.resolveSoftDeleteProperties();

        assertThat(result.enabled()).isFalse();
    }

    @Test
    void resolveSoftDeleteProperties_disabledConfig_returnsDisabled() {
        DbProperties properties = new DbProperties();
        DbProperties.SoftDeleteConfig config = new DbProperties.SoftDeleteConfig();
        config.setEnabled(false);
        properties.setSoftDelete(config);

        SoftDeleteProperties result = properties.resolveSoftDeleteProperties();

        assertThat(result.enabled()).isFalse();
    }

    @Test
    void resolveSoftDeleteProperties_enabledConfig_returnsConfiguredProperties() {
        DbProperties properties = new DbProperties();
        DbProperties.SoftDeleteConfig config = new DbProperties.SoftDeleteConfig();
        config.setEnabled(true);
        config.setColumn("removed_at");
        config.setTables(List.of("exam", "question"));
        properties.setSoftDelete(config);

        SoftDeleteProperties result = properties.resolveSoftDeleteProperties();

        assertThat(result.enabled()).isTrue();
        assertThat(result.column()).isEqualTo("removed_at");
        assertThat(result.tables()).containsExactly("exam", "question");
    }

    @Test
    void softDeleteConfig_defaults() {
        DbProperties.SoftDeleteConfig config = new DbProperties.SoftDeleteConfig();

        assertThat(config.isEnabled()).isFalse();
        assertThat(config.getColumn()).isEqualTo("deleted_at");
        assertThat(config.getTables()).isNull();
    }

    @Test
    void softDeleteConfig_settersAndGetters() {
        DbProperties.SoftDeleteConfig config = new DbProperties.SoftDeleteConfig();
        config.setEnabled(true);
        config.setColumn("archived_at");
        config.setTables(List.of("users", "orders"));

        assertThat(config.isEnabled()).isTrue();
        assertThat(config.getColumn()).isEqualTo("archived_at");
        assertThat(config.getTables()).containsExactly("users", "orders");
    }

    private DatabaseConnectionDetail createConnectionDetail(String id) {
        return new DatabaseConnectionDetail(
                id,
                "POSTGRESQL",
                "jdbc:postgresql://localhost:5432/test",
                "user",
                "password",
                "test",
                null,
                null,
                null,
                null,
                null,
                10
        );
    }
}
