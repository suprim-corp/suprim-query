package dev.suprim.query.autoconfigure;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.suprim.query.dialect.Dialect;
import dev.suprim.query.jdbc.config.DataSourceMetadataConfig;
import dev.suprim.query.jdbc.config.DatabaseConnectionDetail;
import dev.suprim.query.jdbc.config.RoutingDataSource;
import dev.suprim.query.jdbc.executor.creation.CreationService;
import dev.suprim.query.jdbc.executor.creation.JdbcCreationService;
import dev.suprim.query.jdbc.executor.deletion.DeleteService;
import dev.suprim.query.jdbc.executor.deletion.JdbcDeleteService;
import dev.suprim.query.model.SoftDeleteProperties;
import dev.suprim.query.jdbc.executor.raw.JdbcRawQueryService;
import dev.suprim.query.jdbc.executor.raw.RawQueryService;
import dev.suprim.query.jdbc.executor.read.JdbcReadService;
import dev.suprim.query.jdbc.executor.read.ReadService;
import dev.suprim.query.jdbc.executor.update.JdbcUpdateService;
import dev.suprim.query.jdbc.executor.update.UpdateService;
import dev.suprim.query.jdbc.operation.DbOperationService;
import dev.suprim.query.jdbc.operation.JdbcManager;
import dev.suprim.query.jdbc.operation.JdbcOperationService;
import dev.suprim.query.jdbc.operation.SqlCreatorTemplate;
import dev.suprim.query.jdbc.processor.*;
import dev.suprim.query.postgresql.PostGreSQLDialect;
import dev.suprim.query.postgresql.PostgreSQLDataExclusion;
import dev.suprim.query.support.MetaDataExtraction;
import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.ObjectMapper;

import javax.sql.DataSource;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(DbProperties.class)
@ConditionalOnProperty(prefix = "db", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class DbAutoConfiguration {

    private final DbProperties dbProperties;
    private final ApplicationContext applicationContext;

    @Bean
    @ConditionalOnMissingBean(ObjectMapper.class)
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    @ConditionalOnMissingBean(RoutingDataSource.class)
    public RoutingDataSource dataSource() {
        // Priority 1: explicit db.databases config
        if (dbProperties.isRdbmsConfigured()) {
            Map<String, DataSource> targets = buildDataSources();

            if (targets.isEmpty()) {
                throw new IllegalStateException("No data-sources configured");
            }

            log.info("Data-sources (explicit config) - {}", targets.keySet());
            log.info("Default database - {}", dbProperties.getDefaultDatabaseId());

            return new RoutingDataSource(targets, dbProperties.getDefaultDatabaseId());
        }

        // Priority 2: auto-detect DataSource beans annotated with @SuprimSchemas
        Map<String, DataSource> detected = detectAnnotatedDataSources();

        if (detected.isEmpty()) {
            throw new IllegalStateException(
                    "No data-sources configured: neither db.databases nor @SuprimSchemas-annotated DataSource beans found"
            );
        }

        String defaultDbId = resolveDefaultDatabaseId(detected);
        log.info("Data-sources (auto-detected) - {}", detected.keySet());
        log.info("Default database - {}", defaultDbId);

        dbProperties.setDefaultDatabaseId(defaultDbId);

        return new RoutingDataSource(detected, defaultDbId);
    }

    private Map<String, DataSource> buildDataSources() {
        final Map<String, DataSource> result = new HashMap<>();

        log.debug("Databases - {}", dbProperties.getDatabases());

        if (!dbProperties.isRdbmsConfigured()) {
            log.info("*** No RDBMS configured.");
            return result;
        }

        for (DatabaseConnectionDetail connectionDetail : dbProperties.getDatabases()) {
            if (connectionDetail.isJdbcPresent()) {
                result.put(connectionDetail.id(), buildDataSource(connectionDetail));
            }
        }

        return result;
    }

    private DataSource buildDataSource(DatabaseConnectionDetail connectionDetail) {
        final HikariConfig config = new HikariConfig();

        config.setJdbcUrl(connectionDetail.url());
        config.setUsername(connectionDetail.username());
        config.setPassword(connectionDetail.password());
        config.setMaximumPoolSize(connectionDetail.maxConnections() > 0 ? connectionDetail.maxConnections() : 10);
        config.setAutoCommit(false);

        return new HikariDataSource(config);
    }

    @Bean
    @ConditionalOnMissingBean
    public JdbcManager jdbcManager(RoutingDataSource dataSource, ObjectMapper objectMapper) {
        // NOTE: Only PostgreSQL is currently supported.
        // To add support for another database type: implement Dialect and MetaDataExtraction
        // for the target DB, then override this jdbcManager() bean in your application context.
        List<Dialect> dialects = List.of(
                new PostGreSQLDialect(objectMapper)
        );

        List<MetaDataExtraction> metaDataExtractions = List.of(
                new PostgreSQLDataExclusion()
        );

        Map<String, DataSourceMetadataConfig> metadataConfigMap = buildMetadataConfigMap();

        return new JdbcManager(dataSource, dialects, dbProperties, metaDataExtractions, metadataConfigMap);
    }

    @Bean
    @ConditionalOnMissingBean
    public DbOperationService dbOperationService() {
        return new JdbcOperationService();
    }

    @Bean(name = "dbTemplateEngine")
    @ConditionalOnMissingBean(name = "dbTemplateEngine")
    public TemplateEngine templateEngine() {
        return TemplateEngine.createPrecompiled(ContentType.Plain);
    }

    @Bean
    @ConditionalOnMissingBean
    public SqlCreatorTemplate sqlCreatorTemplate(
            @Qualifier("dbTemplateEngine") TemplateEngine templateEngine,
            JdbcManager jdbcManager
    ) {
        return new SqlCreatorTemplate(templateEngine, jdbcManager);
    }

    @Bean
    @ConditionalOnMissingBean
    public TSIDProcessor tsidProcessor() {
        return new TSIDProcessor();
    }

    @Bean
    @ConditionalOnMissingBean
    public JoinProcessor joinProcessor(JdbcManager jdbcManager) {
        return new JoinProcessor(jdbcManager);
    }

    @Bean
    @ConditionalOnMissingBean
    public OrderByProcessor orderByProcessor() {
        return new OrderByProcessor();
    }

    @Bean
    @ConditionalOnMissingBean
    public RootTableFieldProcessor rootTableFieldProcessor() {
        return new RootTableFieldProcessor();
    }

    @Bean
    @ConditionalOnMissingBean
    public RootTableProcessor rootTableProcessor(JdbcManager jdbcManager) {
        return new RootTableProcessor(jdbcManager);
    }

    @Bean
    @ConditionalOnMissingBean
    public RootWhereProcessor rootWhereProcessor(JdbcManager jdbcManager) {
        return new RootWhereProcessor(jdbcManager);
    }

    @Bean
    @ConditionalOnMissingBean
    public SoftDeleteProperties softDeleteProperties() {
        return dbProperties.resolveSoftDeleteProperties();
    }

    @Bean
    @ConditionalOnMissingBean
    public SoftDeleteProcessor softDeleteProcessor(SoftDeleteProperties softDeleteProperties) {
        return new SoftDeleteProcessor(softDeleteProperties);
    }

    @Bean
    @ConditionalOnMissingBean
    public ReadService readService(
            JdbcManager jdbcManager,
            SqlCreatorTemplate sqlCreatorTemplate,
            List<ReadProcessor> processorList,
            DbOperationService dbOperationService
    ) {
        return new JdbcReadService(jdbcManager, dbOperationService, processorList, sqlCreatorTemplate);
    }

    @Bean
    @ConditionalOnMissingBean
    public CreationService creationService(
            TSIDProcessor tsidProcessor,
            SqlCreatorTemplate sqlCreatorTemplate,
            JdbcManager jdbcManager,
            DbOperationService dbOperationService
    ) {
        return new JdbcCreationService(tsidProcessor, sqlCreatorTemplate, jdbcManager, dbOperationService);
    }

    @Bean
    @ConditionalOnMissingBean
    public UpdateService updateService(
            JdbcManager jdbcManager,
            SqlCreatorTemplate sqlCreatorTemplate,
            DbOperationService dbOperationService
    ) {
        return new JdbcUpdateService(jdbcManager, sqlCreatorTemplate, dbOperationService);
    }

    @Bean
    @ConditionalOnMissingBean
    public DeleteService deleteService(
            JdbcManager jdbcManager,
            SqlCreatorTemplate sqlCreatorTemplate,
            DbOperationService dbOperationService,
            SoftDeleteProperties softDeleteProperties
    ) {
        return new JdbcDeleteService(jdbcManager, sqlCreatorTemplate, dbOperationService, softDeleteProperties);
    }

    @Bean
    @ConditionalOnMissingBean
    public RawQueryService rawQueryService(JdbcManager jdbcManager) {
        return new JdbcRawQueryService(jdbcManager);
    }

    // --- Private helpers ---

    /**
     * Detects all DataSource beans in the application context that are annotated with @SuprimSchemas.
     * Returns a map of bean name -> DataSource.
     */
    private Map<String, DataSource> detectAnnotatedDataSources() {
        Map<String, DataSource> result = new HashMap<>();
        String[] beanNames = applicationContext.getBeanNamesForType(DataSource.class);

        for (String beanName : beanNames) {
            SuprimSchemas annotation = findSuprimSchemasAnnotation(beanName);
            if (annotation != null) {
                DataSource ds = applicationContext.getBean(beanName, DataSource.class);
                result.put(beanName, ds);
                log.debug("Auto-detected @SuprimSchemas DataSource bean: {}", beanName);
            }
        }

        return result;
    }

    /**
     * Builds the metadata config map from @SuprimSchemas annotations on DataSource beans.
     * Only populated when db.databases is not configured (auto-detect mode).
     */
    private Map<String, DataSourceMetadataConfig> buildMetadataConfigMap() {
        if (dbProperties.isRdbmsConfigured()) {
            return Map.of();
        }

        Map<String, DataSourceMetadataConfig> result = new HashMap<>();
        String[] beanNames = applicationContext.getBeanNamesForType(DataSource.class);

        for (String beanName : beanNames) {
            SuprimSchemas annotation = findSuprimSchemasAnnotation(beanName);
            if (annotation != null) {
                List<String> schemas = List.of(annotation.value());
                result.put(beanName, DataSourceMetadataConfig.of(beanName, schemas));
            }
        }

        return result;
    }

    /**
     * Resolves the default database ID from auto-detected DataSource beans.
     * Uses the @Primary bean if present, otherwise the first bean found.
     */
    private String resolveDefaultDatabaseId(Map<String, DataSource> detected) {
        BeanFactory beanFactory = applicationContext.getAutowireCapableBeanFactory();

        if (beanFactory instanceof DefaultListableBeanFactory listableBeanFactory) {
            for (String beanName : detected.keySet()) {
                BeanDefinition bd = listableBeanFactory.getBeanDefinition(beanName);
                if (bd.isPrimary()) {
                    return beanName;
                }
            }
        }

        // Fallback: first entry
        return detected.keySet().iterator().next();
    }

    /**
     * Finds the @SuprimSchemas annotation on the factory method of the given bean.
     */
    private SuprimSchemas findSuprimSchemasAnnotation(String beanName) {
        BeanFactory beanFactory = applicationContext.getAutowireCapableBeanFactory();

        if (!(beanFactory instanceof DefaultListableBeanFactory listableBeanFactory)) {
            return null;
        }

        if (!listableBeanFactory.containsBeanDefinition(beanName)) {
            return null;
        }

        BeanDefinition bd = listableBeanFactory.getBeanDefinition(beanName);
        String factoryMethodName = bd.getFactoryMethodName();
        String factoryBeanName = bd.getFactoryBeanName();

        if (factoryMethodName == null || factoryBeanName == null) {
            return null;
        }

        try {
            Class<?> factoryClass = applicationContext.getType(factoryBeanName);
            if (factoryClass == null) {
                return null;
            }

            for (Method method : factoryClass.getDeclaredMethods()) {
                if (method.getName().equals(factoryMethodName)) {
                    SuprimSchemas annotation = method.getAnnotation(SuprimSchemas.class);
                    if (annotation != null) {
                        return annotation;
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Unable to inspect factory method for bean '{}': {}", beanName, e.getMessage());
        }

        return null;
    }
}
