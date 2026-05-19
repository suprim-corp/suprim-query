package dev.suprim.query.jdbc.operation;

import dev.suprim.query.dialect.Dialect;
import dev.suprim.query.exception.DbErrorCode;
import dev.suprim.query.exception.DbException;
import dev.suprim.query.exception.DbRuntimeException;
import dev.suprim.query.jdbc.config.DataSourceMetadataConfig;
import dev.suprim.query.jdbc.config.DatabaseConnectionDetail;
import dev.suprim.query.jdbc.config.DatabaseProperties;
import dev.suprim.query.jdbc.config.DbDetailHolder;
import dev.suprim.query.jdbc.config.RoutingDataSource;
import dev.suprim.query.jdbc.operation.support.JdbcMetaDataProvider;
import dev.suprim.query.model.DbMeta;
import dev.suprim.query.model.DbTable;
import dev.suprim.query.support.MetaDataExtraction;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.MetaDataAccessException;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Slf4j
public final class JdbcManager {
    private final DataSource dataSource;
    private final List<Dialect> availableDialects;
    private final DatabaseProperties databaseProperties;
    private final List<MetaDataExtraction> metaDataExtractions;
    private final Map<String, DataSourceMetadataConfig> metadataConfigMap;
    private final Map<String, DbDetailHolder> dbDetailHolderMap = new ConcurrentHashMap<>();
    private final Map<String, NamedParameterJdbcTemplate> namedParameterJdbcTemplateMap = new ConcurrentHashMap<>();
    private final Map<String, TransactionTemplate> transactionTemplateMap = new ConcurrentHashMap<>();

    /**
     * Original constructor — backward compatible. No metadata config map (uses DatabaseConnectionDetail).
     */
    public JdbcManager(
            DataSource dataSource,
            List<Dialect> availableDialects,
            DatabaseProperties databaseProperties,
            List<MetaDataExtraction> metaDataExtractions
    ) {
        this(dataSource, availableDialects, databaseProperties, metaDataExtractions, Map.of());
    }

    /**
     * Full constructor with metadata config map for auto-detected datasources.
     */
    public JdbcManager(
            DataSource dataSource,
            List<Dialect> availableDialects,
            DatabaseProperties databaseProperties,
            List<MetaDataExtraction> metaDataExtractions,
            Map<String, DataSourceMetadataConfig> metadataConfigMap
    ) {
        this.dataSource = dataSource;
        this.availableDialects = availableDialects;
        this.databaseProperties = databaseProperties;
        this.metaDataExtractions = metaDataExtractions;
        this.metadataConfigMap = metadataConfigMap;
    }

    @PostConstruct
    public void reload() {
        log.info("Reloading JDBC meta data.");
        try {
            loadAllMetaData();
        } catch (DbRuntimeException e) {
            // Allow the ApplicationContext to start with degraded state.
            // Metadata will be absent until the next reload() call or first request triggers it.
            // This prevents a transient DB outage at startup from killing the whole application.
            log.error(
                    "Failed to load DB metadata at startup — tables will not be available " +
                    "until DB is reachable and reload() is called again. Cause: {}",
                    e.getMessage()
            );
        }
    }

    public DbMeta getDbMetaByDbId(String dbId) {
        Map<String, DbMeta> dbMetaMap = getDbMetaMap();
        return dbMetaMap.get(dbId);
    }

    public Map<String, DbMeta> getDbMetaMap() {
        Map<String, DbMeta> dbMetaMap = new HashMap<>();
        dbDetailHolderMap.forEach((k, v) -> dbMetaMap.put(k, v.dbMeta()));
        return dbMetaMap;
    }

    public List<DbTable> getTables() {
        return List.of();
    }

    /**
     * Returns all tables loaded for the given database ID.
     *
     * @param dbId the database identifier
     * @return immutable list of all tables for the given DB
     * @throws DbException if the DB ID is not configured
     */
    public List<DbTable> getTables(String dbId) throws DbException {
        DbDetailHolder holder = dbDetailHolderMap.get(dbId);
        if (isNull(holder)) {
            throw new DbException(DbErrorCode.NOT_FOUND, "DB not found: " + dbId);
        }
        return List.copyOf(holder.dbTableMap().values());
    }

    public void loadAllMetaData() {
        log.info("Attempting to load meta-data for all relational data-sources.");

        if (!(dataSource instanceof RoutingDataSource)) {
            log.info("Not routing data source. Unable to load database metadata.");
            return;
        }

        Map<Object, DataSource> dataSourceMap = ((RoutingDataSource) dataSource).getResolvedDataSources();

        for (Object dbId : dataSourceMap.keySet()) {
            DataSource ds = dataSourceMap.get(dbId);
            String dbIdStr = (String) dbId;

            // Resolve schema config: DatabaseConnectionDetail (explicit) > DataSourceMetadataConfig (auto-detect)
            DatabaseConnectionDetail databaseConnectionDetail = null;
            DataSourceMetadataConfig metadataConfig = null;

            Optional<DatabaseConnectionDetail> connectionDetail = databaseProperties.getDatabase(dbIdStr);
            if (connectionDetail.isPresent()) {
                databaseConnectionDetail = connectionDetail.get();
            } else {
                metadataConfig = metadataConfigMap.get(dbIdStr);
            }

            log.debug("Database connection details - {}, metadata config - {}", databaseConnectionDetail, metadataConfig);

            loadMetaData(dbIdStr, ds, databaseConnectionDetail, metadataConfig);

            namedParameterJdbcTemplateMap.put(
                    dbIdStr,
                    new NamedParameterJdbcTemplate(ds)
            );

            JdbcTransactionManager jdbcTransactionManager = new JdbcTransactionManager(ds);

            transactionTemplateMap.put(
                    dbIdStr,
                    new TransactionTemplate(jdbcTransactionManager)
            );
        }
    }

    private void loadMetaData(
            String dbId,
            DataSource ds,
            DatabaseConnectionDetail databaseConnectionDetail,
            DataSourceMetadataConfig metadataConfig
    ) {
        log.debug("Loading meta data - {}", ds);
        try {
            Map<String, DbTable> dbTableMap = new ConcurrentHashMap<>();

            // Resolve schema filtering: explicit config > metadata config > default (all schemas)
            boolean includeAllSchemas = true;
            List<String> schemas = null;

            if (nonNull(databaseConnectionDetail)) {
                includeAllSchemas = databaseConnectionDetail.includeAllSchemas();
                schemas = databaseConnectionDetail.schemas();
            } else if (nonNull(metadataConfig)) {
                includeAllSchemas = metadataConfig.includeAllSchemas();
                schemas = metadataConfig.schemas();
            }

            log.info("Include all schemas - {}", includeAllSchemas);
            log.info("Schemas - {}", schemas);

            JdbcMetaDataProvider metaDataProvider = new JdbcMetaDataProvider(includeAllSchemas, schemas);
            metaDataExtractions.forEach(metaDataProvider::addExtraction);

            DbMeta dbMeta = JdbcUtils.extractDatabaseMetaData(ds, metaDataProvider);

            for (final DbTable dbTable : dbMeta.dbTables()) {
                // Key by schema.name to prevent silent collision when two tables
                // share the same name across different schemas (e.g. public.users vs audit.users)
                dbTableMap.put(dbTable.schema() + "." + dbTable.name(), dbTable);
            }

            Dialect dialect = availableDialects.stream()
                    .filter(d -> d.isSupportedDb(dbMeta.productName(), dbMeta.majorVersion()))
                    .findFirst()
                    .orElseThrow(() -> new DbException(DbErrorCode.NOT_FOUND, "Dialect not found."));

            dbDetailHolderMap.put(
                    dbId,
                    new DbDetailHolder(dbId, dbMeta, dbTableMap, dialect)
            );
        } catch (MetaDataAccessException | DbException e) {
            throw new DbRuntimeException(e instanceof DbException de ? de
                    : new DbException(DbErrorCode.SERVER_ERROR, e.getMessage(), e));
        }
    }

    public DbTable getTable(String dbId, String schemaName, String tableName) throws DbException {
        if (nonNull(schemaName) && !schemaName.isBlank()) {
            return getBySchemaAndTableName(dbId, schemaName, tableName);
        }

        DbDetailHolder dbDetailHolder = this.dbDetailHolderMap.get(dbId);

        if (isNull(dbDetailHolder)) {
            throw new DbException(DbErrorCode.NOT_FOUND, "DB not found.");
        }

        // Map is keyed by "schema.name" — search by table name only when schema not provided.
        // If multiple tables share the same name across schemas, return the first match
        // (prefer using schemaName for deterministic resolution in multi-schema setups).
        DbTable table = dbDetailHolder.dbTableMap().values().stream()
                .filter(t -> tableName.equalsIgnoreCase(t.name()))
                .findFirst()
                .orElse(null);

        log.debug("Table retrieved - {}", table);

        if (isNull(table)) {
            throw new DbException(DbErrorCode.INVALID_REQUEST, "Invalid table name - " + tableName);
        }

        return table;
    }

    private DbTable getBySchemaAndTableName(
            String dbId,
            String schemaName,
            String tableName
    ) throws DbException {
        DbDetailHolder dbDetailHolder = this.dbDetailHolderMap.get(dbId);

        if (isNull(dbDetailHolder)) {
            throw new DbException(DbErrorCode.NOT_FOUND, "DB not found.");
        }

        DbMeta dbMeta = dbDetailHolder.dbMeta();

        return dbMeta.dbTables()
                .stream()
                .filter(dbTable ->
                        schemaName.equalsIgnoreCase(dbTable.schema()) &&
                        tableName.equalsIgnoreCase(dbTable.name())
                )
                .findFirst()
                .orElseThrow(() -> new DbException(
                        DbErrorCode.INVALID_REQUEST,
                        "Missing table - schema: %s, table: %s".formatted(schemaName, tableName)
                ));
    }

    public NamedParameterJdbcTemplate getNamedParameterJdbcTemplate(String dbId) {
        return this.namedParameterJdbcTemplateMap.get(dbId);
    }

    public TransactionTemplate getTxnTemplate(String dbId) {
        return this.transactionTemplateMap.get(dbId);
    }

    public Dialect getDialect(String dbId) throws DbException {
        DbDetailHolder dbDetailHolder = this.dbDetailHolderMap.get(dbId);

        if (isNull(dbDetailHolder)) {
            throw new DbException(DbErrorCode.NOT_FOUND, "DB not found.");
        }

        return dbDetailHolder.dialect();
    }
}
