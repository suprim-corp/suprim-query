package dev.suprim.query.autoconfigure;

import com.zaxxer.hikari.HikariDataSource;
import dev.suprim.query.jdbc.config.RoutingDataSource;
import dev.suprim.query.jdbc.operation.JdbcManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the @SuprimSchemas auto-detect feature in DbAutoConfiguration.
 * Verifies that DataSource beans annotated with @SuprimSchemas are automatically
 * picked up when no explicit db.databases config is provided.
 */
@Testcontainers
class AutoDetectDataSourceTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");

    private static String jdbcUrl;
    private static String username;
    private static String password;

    @BeforeAll
    static void setUp() {
        jdbcUrl = postgres.getJdbcUrl();
        username = postgres.getUsername();
        password = postgres.getPassword();
    }

    private ApplicationContextRunner contextRunner() {
        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(DbAutoConfiguration.class));
    }

    // --- Test configuration classes simulating user @Bean definitions ---

    @Configuration(proxyBeanMethods = false)
    static class SingleAnnotatedDataSourceConfig {

        @Bean
        @SuprimSchemas({"public", "audit"})
        public DataSource mainDataSource() {
            return createDataSource();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class MultipleAnnotatedDataSourcesConfig {

        @Bean
        @Primary
        @SuprimSchemas({"public"})
        public DataSource primaryDs() {
            return createDataSource();
        }

        @Bean
        @SuprimSchemas({"memory_schema"})
        public DataSource secondaryDs() {
            return createDataSource();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class MixedAnnotatedAndUnannotatedConfig {

        @Bean
        @Primary
        @SuprimSchemas({"public"})
        public DataSource annotatedDs() {
            return createDataSource();
        }

        @Bean
        public DataSource unannotatedDs() {
            return createDataSource();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class DefaultSchemasConfig {

        @Bean
        @SuprimSchemas // defaults to {"public"}
        public DataSource defaultSchemaDs() {
            return createDataSource();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class NoPrimaryMultipleConfig {

        @Bean
        @SuprimSchemas({"schema_a"})
        public DataSource firstDs() {
            return createDataSource();
        }

        @Bean
        @SuprimSchemas({"schema_b"})
        public DataSource secondDs() {
            return createDataSource();
        }
    }

    // --- Tests ---

    @Test
    void autoDetect_singleAnnotatedBean_shouldCreateRoutingDataSource() {
        contextRunner()
                .withUserConfiguration(SingleAnnotatedDataSourceConfig.class)
                .withPropertyValues("db.enabled=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(RoutingDataSource.class);

                    RoutingDataSource routing = context.getBean(RoutingDataSource.class);
                    assertThat(routing.getKnownIds()).contains("mainDataSource");
                });
    }

    @Test
    void autoDetect_multipleAnnotatedBeans_shouldCreateRoutingWithAll() {
        contextRunner()
                .withUserConfiguration(MultipleAnnotatedDataSourcesConfig.class)
                .withPropertyValues("db.enabled=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();

                    RoutingDataSource routing = context.getBean(RoutingDataSource.class);
                    assertThat(routing.getKnownIds())
                            .contains("primaryDs", "secondaryDs")
                            .hasSize(2);
                });
    }

    @Test
    void autoDetect_primaryBean_shouldBecomeDefaultDatabaseId() {
        contextRunner()
                .withUserConfiguration(MultipleAnnotatedDataSourcesConfig.class)
                .withPropertyValues("db.enabled=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();

                    DbProperties props = context.getBean(DbProperties.class);
                    assertThat(props.getDefaultDatabaseId()).isEqualTo("primaryDs");
                });
    }

    @Test
    void autoDetect_unannotatedBeans_shouldBeIgnored() {
        contextRunner()
                .withUserConfiguration(MixedAnnotatedAndUnannotatedConfig.class)
                .withPropertyValues("db.enabled=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();

                    RoutingDataSource routing = context.getBean(RoutingDataSource.class);
                    // Only the annotated bean should be in the routing map
                    assertThat(routing.getKnownIds())
                            .contains("annotatedDs")
                            .doesNotContain("unannotatedDs")
                            .hasSize(1);
                });
    }

    @Test
    void autoDetect_defaultSchemaAnnotation_shouldUsePublicSchema() {
        contextRunner()
                .withUserConfiguration(DefaultSchemasConfig.class)
                .withPropertyValues("db.enabled=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();

                    RoutingDataSource routing = context.getBean(RoutingDataSource.class);
                    assertThat(routing.getKnownIds()).contains("defaultSchemaDs");
                });
    }

    @Test
    void autoDetect_noPrimary_shouldFallbackToFirstBean() {
        contextRunner()
                .withUserConfiguration(NoPrimaryMultipleConfig.class)
                .withPropertyValues("db.enabled=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();

                    DbProperties props = context.getBean(DbProperties.class);
                    // Should pick one of the two (first found)
                    assertThat(props.getDefaultDatabaseId())
                            .isIn("firstDs", "secondDs");
                });
    }

    @Test
    void autoDetect_explicitConfigTakesPriority() {
        contextRunner()
                .withUserConfiguration(SingleAnnotatedDataSourceConfig.class)
                .withPropertyValues(
                        "db.enabled=true",
                        "db.databases[0].id=explicit-db",
                        "db.databases[0].type=POSTGRESQL",
                        "db.databases[0].url=" + jdbcUrl,
                        "db.databases[0].username=" + username,
                        "db.databases[0].password=" + password
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();

                    RoutingDataSource routing = context.getBean(RoutingDataSource.class);
                    // Should use explicit config, not auto-detected beans
                    assertThat(routing.getKnownIds())
                            .contains("explicit-db")
                            .doesNotContain("mainDataSource");
                });
    }

    @Test
    void autoDetect_shouldCreateJdbcManager() {
        contextRunner()
                .withUserConfiguration(SingleAnnotatedDataSourceConfig.class)
                .withPropertyValues("db.enabled=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(JdbcManager.class);
                });
    }

    @Test
    void autoDetect_noAnnotatedBeans_noConfig_shouldFail() {
        contextRunner()
                .withPropertyValues("db.enabled=true")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .rootCause()
                            .hasMessageContaining("No data-sources configured");
                });
    }

    // --- Helper ---

    private static DataSource createDataSource() {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(jdbcUrl);
        ds.setUsername(username);
        ds.setPassword(password);
        ds.setMaximumPoolSize(2);
        ds.setAutoCommit(false);
        return ds;
    }
}
