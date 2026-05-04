package dev.suprim.query.jdbc.config;

import dev.suprim.query.dialect.Dialect;
import dev.suprim.query.model.DbMeta;
import dev.suprim.query.model.DbTable;
import org.junit.jupiter.api.*;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;

class ConfigurationTest {

    @AfterEach
    void cleanUp() {
        DatabaseContextHolder.clear();
    }

    @Nested
    @DisplayName("RoutingDataSource Tests")
    class RoutingDataSourceTests {

        @Test
        void constructor_emptyTargets_throwsIllegalStateException() {
            Map<String, DataSource> emptyTargets = new HashMap<>();

            assertThatThrownBy(() -> new RoutingDataSource(emptyTargets, "default"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("No data sources configured");
        }

        @Test
        void constructor_missingDefaultId_throwsIllegalArgumentException() {
            DataSource ds = mock(DataSource.class);
            Map<String, DataSource> targets = Map.of("db1", ds);

            assertThatThrownBy(() -> new RoutingDataSource(targets, "nonexistent"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("nonexistent")
                    .hasMessageContaining("not found in targets");
        }

        @Test
        void constructor_validConfig_createsRoutingDataSource() {
            DataSource ds1 = mock(DataSource.class);
            DataSource ds2 = mock(DataSource.class);
            Map<String, DataSource> targets = new HashMap<>();
            targets.put("db1", ds1);
            targets.put("db2", ds2);

            RoutingDataSource routingDs = new RoutingDataSource(targets, "db1");

            assertThat(routingDs.getDefaultId()).isEqualTo("db1");
            assertThat(routingDs.getKnownIds()).containsExactlyInAnyOrder("db1", "db2");
        }

        @Test
        void determineCurrentLookupKey_nullKey_returnsDefaultId() {
            DataSource ds = mock(DataSource.class);
            Map<String, DataSource> targets = new HashMap<>();
            targets.put("default", ds);
            RoutingDataSource routingDs = new RoutingDataSource(targets, "default");

            DatabaseContextHolder.clear();
            Object key = routingDs.determineCurrentLookupKey();

            assertThat(key).isEqualTo("default");
        }

        @Test
        void determineCurrentLookupKey_unknownKey_returnsDefaultId() {
            DataSource ds = mock(DataSource.class);
            Map<String, DataSource> targets = new HashMap<>();
            targets.put("default", ds);
            RoutingDataSource routingDs = new RoutingDataSource(targets, "default");

            DatabaseContextHolder.setCurrentDbId("unknown_db");
            Object key = routingDs.determineCurrentLookupKey();

            assertThat(key).isEqualTo("default");
        }

        @Test
        void determineCurrentLookupKey_validKey_returnsKey() {
            DataSource ds1 = mock(DataSource.class);
            DataSource ds2 = mock(DataSource.class);
            Map<String, DataSource> targets = new HashMap<>();
            targets.put("db1", ds1);
            targets.put("db2", ds2);
            RoutingDataSource routingDs = new RoutingDataSource(targets, "db1");

            DatabaseContextHolder.setCurrentDbId("db2");
            Object key = routingDs.determineCurrentLookupKey();

            assertThat(key).isEqualTo("db2");
        }

        @Test
        void knownIds_isUnmodifiable() {
            DataSource ds = mock(DataSource.class);
            Map<String, DataSource> targets = new HashMap<>();
            targets.put("db1", ds);
            RoutingDataSource routingDs = new RoutingDataSource(targets, "db1");

            assertThatThrownBy(() -> routingDs.getKnownIds().add("newDb"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("DatabaseContextHolder Tests")
    class DatabaseContextHolderTests {

        @Test
        void setAndGet_returnsSetValue() {
            DatabaseContextHolder.setCurrentDbId("testDb");

            assertThat(DatabaseContextHolder.getCurrentDbId()).isEqualTo("testDb");
        }

        @Test
        void clear_removesValue() {
            DatabaseContextHolder.setCurrentDbId("testDb");
            DatabaseContextHolder.clear();

            assertThat(DatabaseContextHolder.getCurrentDbId()).isNull();
        }

        @Test
        void threadIsolation_differentThreadsDifferentValues() throws InterruptedException {
            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<String> thread2Value = new AtomicReference<>();

            DatabaseContextHolder.setCurrentDbId("mainThread");

            Thread thread2 = new Thread(() -> {
                DatabaseContextHolder.setCurrentDbId("thread2");
                thread2Value.set(DatabaseContextHolder.getCurrentDbId());
                latch.countDown();
            });
            thread2.start();
            latch.await();

            assertThat(DatabaseContextHolder.getCurrentDbId()).isEqualTo("mainThread");
            assertThat(thread2Value.get()).isEqualTo("thread2");
        }

        @Test
        void initialValue_isNull() {
            DatabaseContextHolder.clear();
            assertThat(DatabaseContextHolder.getCurrentDbId()).isNull();
        }
    }

    @Nested
    @DisplayName("DatabaseProperties Tests")
    class DatabasePropertiesTests {

        @Test
        void getDatabase_existingId_returnsOptionalWithValue() {
            DatabaseProperties props = new DatabaseProperties();
            DatabaseConnectionDetail detail = new DatabaseConnectionDetail(
                    "testDb", "postgresql", "jdbc:postgresql://localhost/test",
                    "user", "pass", "testdb", null, List.of("public"), null, null,
                    new EnvironmentProperties(false, "HH:mm:ss", "yyyy-MM-dd", "yyyy-MM-dd HH:mm:ss", 100),
                    5
            );
            props.setDatabases(List.of(detail));

            Optional<DatabaseConnectionDetail> result = props.getDatabase("testDb");

            assertThat(result).isPresent();
            assertThat(result.get().id()).isEqualTo("testDb");
        }

        @Test
        void getDatabase_caseInsensitive_returnsOptionalWithValue() {
            DatabaseProperties props = new DatabaseProperties();
            DatabaseConnectionDetail detail = new DatabaseConnectionDetail(
                    "TestDb", "postgresql", "jdbc:postgresql://localhost/test",
                    "user", "pass", "testdb", null, null, null, null, null, 5
            );
            props.setDatabases(List.of(detail));

            Optional<DatabaseConnectionDetail> result = props.getDatabase("TESTDB");

            assertThat(result).isPresent();
        }

        @Test
        void getDatabase_missingId_returnsEmpty() {
            DatabaseProperties props = new DatabaseProperties();
            props.setDatabases(List.of(new DatabaseConnectionDetail(
                    "db1", "postgresql", "jdbc:postgresql://localhost/test",
                    "user", "pass", "testdb", null, null, null, null, null, 5
            )));

            Optional<DatabaseConnectionDetail> result = props.getDatabase("nonexistent");

            assertThat(result).isEmpty();
        }

        @Test
        void getDatabase_nullDatabases_returnsEmpty() {
            DatabaseProperties props = new DatabaseProperties();
            props.setDatabases(null);

            Optional<DatabaseConnectionDetail> result = props.getDatabase("anyId");

            assertThat(result).isEmpty();
        }

        @Test
        void getDatabase_emptyDatabases_returnsEmpty() {
            DatabaseProperties props = new DatabaseProperties();
            props.setDatabases(List.of());

            Optional<DatabaseConnectionDetail> result = props.getDatabase("anyId");

            assertThat(result).isEmpty();
        }

        @Test
        void isRdbmsConfigured_nullDatabases_returnsFalse() {
            DatabaseProperties props = new DatabaseProperties();
            props.setDatabases(null);

            assertThat(props.isRdbmsConfigured()).isFalse();
        }

        @Test
        void isRdbmsConfigured_noJdbcUrl_returnsFalse() {
            DatabaseProperties props = new DatabaseProperties();
            props.setDatabases(List.of(new DatabaseConnectionDetail(
                    "db1", "postgresql", null,
                    "user", "pass", "testdb", null, null, null, null, null, 5
            )));

            assertThat(props.isRdbmsConfigured()).isFalse();
        }

        @Test
        void isRdbmsConfigured_withJdbcUrl_returnsTrue() {
            DatabaseProperties props = new DatabaseProperties();
            props.setDatabases(List.of(new DatabaseConnectionDetail(
                    "db1", "postgresql", "jdbc:postgresql://localhost/test",
                    "user", "pass", "testdb", null, null, null, null, null, 5
            )));

            assertThat(props.isRdbmsConfigured()).isTrue();
        }

        @Test
        void isRdbmsConfigured_blankJdbcUrl_returnsFalse() {
            DatabaseProperties props = new DatabaseProperties();
            props.setDatabases(List.of(new DatabaseConnectionDetail(
                    "db1", "postgresql", "   ",
                    "user", "pass", "testdb", null, null, null, null, null, 5
            )));

            assertThat(props.isRdbmsConfigured()).isFalse();
        }

        @Test
        void setDefaultDatabaseId_setsValue() {
            DatabaseProperties props = new DatabaseProperties();
            props.setDefaultDatabaseId("myDefault");

            assertThat(props.getDefaultDatabaseId()).isEqualTo("myDefault");
        }
    }

    @Nested
    @DisplayName("DatabaseConnectionDetail Tests")
    class DatabaseConnectionDetailTests {

        @Test
        void isMongo_mongoType_returnsTrue() {
            DatabaseConnectionDetail detail = new DatabaseConnectionDetail(
                    "mongo1", "MONGO", null, "user", "pass", "testdb",
                    null, null, null, null, null, 5
            );

            assertThat(detail.isMongo()).isTrue();
        }

        @Test
        void isMongo_mongoTypeLowerCase_returnsTrue() {
            DatabaseConnectionDetail detail = new DatabaseConnectionDetail(
                    "mongo1", "mongo", null, "user", "pass", "testdb",
                    null, null, null, null, null, 5
            );

            assertThat(detail.isMongo()).isTrue();
        }

        @Test
        void isMongo_postgresqlType_returnsFalse() {
            DatabaseConnectionDetail detail = new DatabaseConnectionDetail(
                    "pg1", "postgresql", "jdbc:postgresql://localhost/test",
                    "user", "pass", "testdb", null, null, null, null, null, 5
            );

            assertThat(detail.isMongo()).isFalse();
        }

        @Test
        void isJdbcPresent_withUrl_returnsTrue() {
            DatabaseConnectionDetail detail = new DatabaseConnectionDetail(
                    "pg1", "postgresql", "jdbc:postgresql://localhost/test",
                    "user", "pass", "testdb", null, null, null, null, null, 5
            );

            assertThat(detail.isJdbcPresent()).isTrue();
        }

        @Test
        void isJdbcPresent_nullUrl_returnsFalse() {
            DatabaseConnectionDetail detail = new DatabaseConnectionDetail(
                    "pg1", "postgresql", null, "user", "pass", "testdb",
                    null, null, null, null, null, 5
            );

            assertThat(detail.isJdbcPresent()).isFalse();
        }

        @Test
        void isJdbcPresent_blankUrl_returnsFalse() {
            DatabaseConnectionDetail detail = new DatabaseConnectionDetail(
                    "pg1", "postgresql", "   ", "user", "pass", "testdb",
                    null, null, null, null, null, 5
            );

            assertThat(detail.isJdbcPresent()).isFalse();
        }

        @Test
        void includeAllSchemas_nullSchemas_returnsTrue() {
            DatabaseConnectionDetail detail = new DatabaseConnectionDetail(
                    "pg1", "postgresql", "jdbc:postgresql://localhost/test",
                    "user", "pass", "testdb", null, null, null, null, null, 5
            );

            assertThat(detail.includeAllSchemas()).isTrue();
        }

        @Test
        void includeAllSchemas_emptySchemas_returnsTrue() {
            DatabaseConnectionDetail detail = new DatabaseConnectionDetail(
                    "pg1", "postgresql", "jdbc:postgresql://localhost/test",
                    "user", "pass", "testdb", null, List.of(), null, null, null, 5
            );

            assertThat(detail.includeAllSchemas()).isTrue();
        }

        @Test
        void includeAllSchemas_withSchemas_returnsFalse() {
            DatabaseConnectionDetail detail = new DatabaseConnectionDetail(
                    "pg1", "postgresql", "jdbc:postgresql://localhost/test",
                    "user", "pass", "testdb", null, List.of("public", "app"),
                    null, null, null, 5
            );

            assertThat(detail.includeAllSchemas()).isFalse();
        }

        @Test
        void recordAccessors_returnCorrectValues() {
            Map<String, String> connProps = Map.of("ssl", "true");
            EnvironmentProperties envProps = new EnvironmentProperties(true, "HH:mm", "yyyy-MM-dd", "yyyy-MM-dd HH:mm", 50);
            DatabaseConnectionDetail detail = new DatabaseConnectionDetail(
                    "pg1", "postgresql", "jdbc:postgresql://localhost/test",
                    "user", "pass", "testdb", List.of("catalog1"),
                    List.of("public"), List.of("users"), connProps, envProps, 10
            );

            assertThat(detail.id()).isEqualTo("pg1");
            assertThat(detail.type()).isEqualTo("postgresql");
            assertThat(detail.url()).isEqualTo("jdbc:postgresql://localhost/test");
            assertThat(detail.username()).isEqualTo("user");
            assertThat(detail.password()).isEqualTo("pass");
            assertThat(detail.database()).isEqualTo("testdb");
            assertThat(detail.catalog()).containsExactly("catalog1");
            assertThat(detail.schemas()).containsExactly("public");
            assertThat(detail.tables()).containsExactly("users");
            assertThat(detail.connectionProperties()).isEqualTo(connProps);
            assertThat(detail.envProperties()).isEqualTo(envProps);
            assertThat(detail.maxConnections()).isEqualTo(10);
        }
    }

    @Nested
    @DisplayName("DbDetailHolder Tests")
    class DbDetailHolderTests {

        @Test
        void recordAccessors_returnCorrectValues() {
            DbMeta dbMeta = mock(DbMeta.class);
            DbTable table = mock(DbTable.class);
            Dialect dialect = mock(Dialect.class);
            Map<String, DbTable> tableMap = Map.of("users", table);

            DbDetailHolder holder = new DbDetailHolder("db1", dbMeta, tableMap, dialect);

            assertThat(holder.dbId()).isEqualTo("db1");
            assertThat(holder.dbMeta()).isEqualTo(dbMeta);
            assertThat(holder.dbTableMap()).isEqualTo(tableMap);
            assertThat(holder.dialect()).isEqualTo(dialect);
        }

        @Test
        void recordEquality_sameValues_areEqual() {
            DbMeta dbMeta = mock(DbMeta.class);
            Map<String, DbTable> tableMap = Map.of();
            Dialect dialect = mock(Dialect.class);

            DbDetailHolder holder1 = new DbDetailHolder("db1", dbMeta, tableMap, dialect);
            DbDetailHolder holder2 = new DbDetailHolder("db1", dbMeta, tableMap, dialect);

            assertThat(holder1).isEqualTo(holder2);
            assertThat(holder1.hashCode()).isEqualTo(holder2.hashCode());
        }
    }

    @Nested
    @DisplayName("EnvironmentProperties Tests")
    class EnvironmentPropertiesTests {

        @Test
        void recordAccessors_returnCorrectValues() {
            EnvironmentProperties props = new EnvironmentProperties(
                    true, "HH:mm:ss", "yyyy-MM-dd", "yyyy-MM-dd HH:mm:ss", 100
            );

            assertThat(props.enableDatetimeFormatting()).isTrue();
            assertThat(props.timeFormat()).isEqualTo("HH:mm:ss");
            assertThat(props.dateFormat()).isEqualTo("yyyy-MM-dd");
            assertThat(props.dateTimeFormat()).isEqualTo("yyyy-MM-dd HH:mm:ss");
            assertThat(props.defaultFetchLimit()).isEqualTo(100);
        }

        @Test
        void recordEquality_sameValues_areEqual() {
            EnvironmentProperties props1 = new EnvironmentProperties(true, "HH:mm", "yyyy-MM-dd", "yyyy-MM-dd HH:mm", 50);
            EnvironmentProperties props2 = new EnvironmentProperties(true, "HH:mm", "yyyy-MM-dd", "yyyy-MM-dd HH:mm", 50);

            assertThat(props1).isEqualTo(props2);
            assertThat(props1.hashCode()).isEqualTo(props2.hashCode());
        }

        @Test
        void recordEquality_differentValues_areNotEqual() {
            EnvironmentProperties props1 = new EnvironmentProperties(true, "HH:mm", "yyyy-MM-dd", "yyyy-MM-dd HH:mm", 50);
            EnvironmentProperties props2 = new EnvironmentProperties(false, "HH:mm", "yyyy-MM-dd", "yyyy-MM-dd HH:mm", 50);

            assertThat(props1).isNotEqualTo(props2);
        }

        @Test
        void toString_containsFieldValues() {
            EnvironmentProperties props = new EnvironmentProperties(true, "HH:mm", "yyyy-MM-dd", "yyyy-MM-dd HH:mm", 50);
            String str = props.toString();

            assertThat(str).contains("true");
            assertThat(str).contains("HH:mm");
            assertThat(str).contains("50");
        }
    }
}
