package dev.suprim.query.jdbc.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.suprim.query.dialect.Dialect;
import dev.suprim.query.model.DbMeta;
import dev.suprim.query.model.DbTable;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@Testcontainers
class ConfigurationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    private static DataSource dataSource1;
    private static DataSource dataSource2;

    @BeforeAll
    static void setUp() {
        HikariConfig config1 = new HikariConfig();
        config1.setJdbcUrl(postgres.getJdbcUrl());
        config1.setUsername(postgres.getUsername());
        config1.setPassword(postgres.getPassword());
        config1.setMaximumPoolSize(2);
        config1.setPoolName("pool1");
        dataSource1 = new HikariDataSource(config1);

        HikariConfig config2 = new HikariConfig();
        config2.setJdbcUrl(postgres.getJdbcUrl());
        config2.setUsername(postgres.getUsername());
        config2.setPassword(postgres.getPassword());
        config2.setMaximumPoolSize(2);
        config2.setPoolName("pool2");
        dataSource2 = new HikariDataSource(config2);
    }

    @AfterAll
    static void tearDown() {
        if (dataSource1 instanceof HikariDataSource hikari) {
            hikari.close();
        }
        if (dataSource2 instanceof HikariDataSource hikari) {
            hikari.close();
        }
    }

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

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> new RoutingDataSource(emptyTargets, "default"));

            assertEquals("No data sources configured", ex.getMessage());
        }

        @Test
        void constructor_missingDefaultId_throwsIllegalArgumentException() {
            Map<String, DataSource> targets = Map.of("db1", dataSource1);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> new RoutingDataSource(targets, "nonexistent"));

            assertTrue(ex.getMessage().contains("nonexistent"));
            assertTrue(ex.getMessage().contains("not found in targets"));
        }

        @Test
        void constructor_validConfig_createsRoutingDataSource() {
            Map<String, DataSource> targets = Map.of(
                    "db1", dataSource1,
                    "db2", dataSource2
            );

            RoutingDataSource routingDs = new RoutingDataSource(targets, "db1");

            assertEquals("db1", routingDs.getDefaultId());
            assertEquals(2, routingDs.getKnownIds().size());
            assertTrue(routingDs.getKnownIds().contains("db1"));
            assertTrue(routingDs.getKnownIds().contains("db2"));
        }

        @Test
        void determineCurrentLookupKey_nullKey_returnsDefaultId() {
            Map<String, DataSource> targets = new HashMap<>();
            targets.put("default", dataSource1);
            RoutingDataSource routingDs = new RoutingDataSource(targets, "default");

            DatabaseContextHolder.clear();
            Object key = routingDs.determineCurrentLookupKey();

            assertEquals("default", key);
        }

        @Test
        void determineCurrentLookupKey_unknownKey_returnsDefaultId() {
            Map<String, DataSource> targets = Map.of("default", dataSource1);
            RoutingDataSource routingDs = new RoutingDataSource(targets, "default");

            DatabaseContextHolder.setCurrentDbId("unknown_db");
            Object key = routingDs.determineCurrentLookupKey();

            assertEquals("default", key);
        }

        @Test
        void determineCurrentLookupKey_validKey_returnsKey() {
            Map<String, DataSource> targets = Map.of(
                    "db1", dataSource1,
                    "db2", dataSource2
            );
            RoutingDataSource routingDs = new RoutingDataSource(targets, "db1");

            DatabaseContextHolder.setCurrentDbId("db2");
            Object key = routingDs.determineCurrentLookupKey();

            assertEquals("db2", key);
        }

        @Test
        void knownIds_isUnmodifiable() {
            Map<String, DataSource> targets = new HashMap<>();
            targets.put("db1", dataSource1);
            RoutingDataSource routingDs = new RoutingDataSource(targets, "db1");

            assertThrows(UnsupportedOperationException.class,
                    () -> routingDs.getKnownIds().add("newDb"));
        }
    }

    @Nested
    @DisplayName("DatabaseContextHolder Tests")
    class DatabaseContextHolderTests {

        @Test
        void setAndGet_returnsSetValue() {
            DatabaseContextHolder.setCurrentDbId("testDb");

            assertEquals("testDb", DatabaseContextHolder.getCurrentDbId());
        }

        @Test
        void clear_removesValue() {
            DatabaseContextHolder.setCurrentDbId("testDb");
            DatabaseContextHolder.clear();

            assertNull(DatabaseContextHolder.getCurrentDbId());
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

            assertEquals("mainThread", DatabaseContextHolder.getCurrentDbId());
            assertEquals("thread2", thread2Value.get());
        }

        @Test
        void initialValue_isNull() {
            DatabaseContextHolder.clear();
            assertNull(DatabaseContextHolder.getCurrentDbId());
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

            assertTrue(result.isPresent());
            assertEquals("testDb", result.get().id());
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

            assertTrue(result.isPresent());
        }

        @Test
        void getDatabase_missingId_returnsEmpty() {
            DatabaseProperties props = new DatabaseProperties();
            props.setDatabases(List.of(new DatabaseConnectionDetail(
                    "db1", "postgresql", "jdbc:postgresql://localhost/test",
                    "user", "pass", "testdb", null, null, null, null, null, 5
            )));

            Optional<DatabaseConnectionDetail> result = props.getDatabase("nonexistent");

            assertTrue(result.isEmpty());
        }

        @Test
        void getDatabase_nullDatabases_returnsEmpty() {
            DatabaseProperties props = new DatabaseProperties();
            props.setDatabases(null);

            Optional<DatabaseConnectionDetail> result = props.getDatabase("anyId");

            assertTrue(result.isEmpty());
        }

        @Test
        void getDatabase_emptyDatabases_returnsEmpty() {
            DatabaseProperties props = new DatabaseProperties();
            props.setDatabases(List.of());

            Optional<DatabaseConnectionDetail> result = props.getDatabase("anyId");

            assertTrue(result.isEmpty());
        }

        @Test
        void isRdbmsConfigured_nullDatabases_returnsFalse() {
            DatabaseProperties props = new DatabaseProperties();
            props.setDatabases(null);

            assertFalse(props.isRdbmsConfigured());
        }

        @Test
        void isRdbmsConfigured_noJdbcUrl_returnsFalse() {
            DatabaseProperties props = new DatabaseProperties();
            props.setDatabases(List.of(new DatabaseConnectionDetail(
                    "db1", "postgresql", null,
                    "user", "pass", "testdb", null, null, null, null, null, 5
            )));

            assertFalse(props.isRdbmsConfigured());
        }

        @Test
        void isRdbmsConfigured_withJdbcUrl_returnsTrue() {
            DatabaseProperties props = new DatabaseProperties();
            props.setDatabases(List.of(new DatabaseConnectionDetail(
                    "db1", "postgresql", "jdbc:postgresql://localhost/test",
                    "user", "pass", "testdb", null, null, null, null, null, 5
            )));

            assertTrue(props.isRdbmsConfigured());
        }

        @Test
        void isRdbmsConfigured_blankJdbcUrl_returnsFalse() {
            DatabaseProperties props = new DatabaseProperties();
            props.setDatabases(List.of(new DatabaseConnectionDetail(
                    "db1", "postgresql", "   ",
                    "user", "pass", "testdb", null, null, null, null, null, 5
            )));

            assertFalse(props.isRdbmsConfigured());
        }

        @Test
        void setDefaultDatabaseId_setsValue() {
            DatabaseProperties props = new DatabaseProperties();
            props.setDefaultDatabaseId("myDefault");

            assertEquals("myDefault", props.getDefaultDatabaseId());
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

            assertTrue(detail.isMongo());
        }

        @Test
        void isMongo_mongoTypeLowerCase_returnsTrue() {
            DatabaseConnectionDetail detail = new DatabaseConnectionDetail(
                    "mongo1", "mongo", null, "user", "pass", "testdb",
                    null, null, null, null, null, 5
            );

            assertTrue(detail.isMongo());
        }

        @Test
        void isMongo_postgresqlType_returnsFalse() {
            DatabaseConnectionDetail detail = new DatabaseConnectionDetail(
                    "pg1", "postgresql", "jdbc:postgresql://localhost/test",
                    "user", "pass", "testdb", null, null, null, null, null, 5
            );

            assertFalse(detail.isMongo());
        }

        @Test
        void isJdbcPresent_withUrl_returnsTrue() {
            DatabaseConnectionDetail detail = new DatabaseConnectionDetail(
                    "pg1", "postgresql", "jdbc:postgresql://localhost/test",
                    "user", "pass", "testdb", null, null, null, null, null, 5
            );

            assertTrue(detail.isJdbcPresent());
        }

        @Test
        void isJdbcPresent_nullUrl_returnsFalse() {
            DatabaseConnectionDetail detail = new DatabaseConnectionDetail(
                    "pg1", "postgresql", null, "user", "pass", "testdb",
                    null, null, null, null, null, 5
            );

            assertFalse(detail.isJdbcPresent());
        }

        @Test
        void isJdbcPresent_blankUrl_returnsFalse() {
            DatabaseConnectionDetail detail = new DatabaseConnectionDetail(
                    "pg1", "postgresql", "   ", "user", "pass", "testdb",
                    null, null, null, null, null, 5
            );

            assertFalse(detail.isJdbcPresent());
        }

        @Test
        void includeAllSchemas_nullSchemas_returnsTrue() {
            DatabaseConnectionDetail detail = new DatabaseConnectionDetail(
                    "pg1", "postgresql", "jdbc:postgresql://localhost/test",
                    "user", "pass", "testdb", null, null, null, null, null, 5
            );

            assertTrue(detail.includeAllSchemas());
        }

        @Test
        void includeAllSchemas_emptySchemas_returnsTrue() {
            DatabaseConnectionDetail detail = new DatabaseConnectionDetail(
                    "pg1", "postgresql", "jdbc:postgresql://localhost/test",
                    "user", "pass", "testdb", null, List.of(), null, null, null, 5
            );

            assertTrue(detail.includeAllSchemas());
        }

        @Test
        void includeAllSchemas_withSchemas_returnsFalse() {
            DatabaseConnectionDetail detail = new DatabaseConnectionDetail(
                    "pg1", "postgresql", "jdbc:postgresql://localhost/test",
                    "user", "pass", "testdb", null, List.of("public", "app"),
                    null, null, null, 5
            );

            assertFalse(detail.includeAllSchemas());
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

            assertEquals("pg1", detail.id());
            assertEquals("postgresql", detail.type());
            assertEquals("jdbc:postgresql://localhost/test", detail.url());
            assertEquals("user", detail.username());
            assertEquals("pass", detail.password());
            assertEquals("testdb", detail.database());
            assertEquals(List.of("catalog1"), detail.catalog());
            assertEquals(List.of("public"), detail.schemas());
            assertEquals(List.of("users"), detail.tables());
            assertEquals(connProps, detail.connectionProperties());
            assertEquals(envProps, detail.envProperties());
            assertEquals(10, detail.maxConnections());
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

            assertEquals("db1", holder.dbId());
            assertEquals(dbMeta, holder.dbMeta());
            assertEquals(tableMap, holder.dbTableMap());
            assertEquals(dialect, holder.dialect());
        }

        @Test
        void recordEquality_sameValues_areEqual() {
            DbMeta dbMeta = mock(DbMeta.class);
            Map<String, DbTable> tableMap = Map.of();
            Dialect dialect = mock(Dialect.class);

            DbDetailHolder holder1 = new DbDetailHolder("db1", dbMeta, tableMap, dialect);
            DbDetailHolder holder2 = new DbDetailHolder("db1", dbMeta, tableMap, dialect);

            assertEquals(holder1, holder2);
            assertEquals(holder1.hashCode(), holder2.hashCode());
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

            assertTrue(props.enableDatetimeFormatting());
            assertEquals("HH:mm:ss", props.timeFormat());
            assertEquals("yyyy-MM-dd", props.dateFormat());
            assertEquals("yyyy-MM-dd HH:mm:ss", props.dateTimeFormat());
            assertEquals(100, props.defaultFetchLimit());
        }

        @Test
        void recordEquality_sameValues_areEqual() {
            EnvironmentProperties props1 = new EnvironmentProperties(true, "HH:mm", "yyyy-MM-dd", "yyyy-MM-dd HH:mm", 50);
            EnvironmentProperties props2 = new EnvironmentProperties(true, "HH:mm", "yyyy-MM-dd", "yyyy-MM-dd HH:mm", 50);

            assertEquals(props1, props2);
            assertEquals(props1.hashCode(), props2.hashCode());
        }

        @Test
        void recordEquality_differentValues_areNotEqual() {
            EnvironmentProperties props1 = new EnvironmentProperties(true, "HH:mm", "yyyy-MM-dd", "yyyy-MM-dd HH:mm", 50);
            EnvironmentProperties props2 = new EnvironmentProperties(false, "HH:mm", "yyyy-MM-dd", "yyyy-MM-dd HH:mm", 50);

            assertNotEquals(props1, props2);
        }

        @Test
        void toString_containsFieldValues() {
            EnvironmentProperties props = new EnvironmentProperties(true, "HH:mm", "yyyy-MM-dd", "yyyy-MM-dd HH:mm", 50);
            String str = props.toString();

            assertTrue(str.contains("true"));
            assertTrue(str.contains("HH:mm"));
            assertTrue(str.contains("50"));
        }
    }
}
