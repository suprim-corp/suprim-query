package dev.suprim.query.autoconfigure;

import dev.suprim.query.jdbc.config.DatabaseConnectionDetail;
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
