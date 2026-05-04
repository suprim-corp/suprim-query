package dev.suprim.query.support;

import dev.suprim.query.model.DbTable;
import dev.suprim.query.model.ColumnLabel;
import dev.suprim.query.model.MetaDataTable;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static dev.suprim.query.util.AliasGenerator.getAlias;

/**
 * SPI for extracting database metadata (tables, columns, primary keys).
 * <p>
 * Implement this interface to add support for a new database type. Register the
 * implementation by overriding the {@code jdbcManager()} bean in
 * {@code DbAutoConfiguration}.
 */
public interface MetaDataExtraction {
    /**
     * Returns {@code true} if this extractor handles the given database product.
     *
     * @param database the database product name from
     *                 {@code DatabaseMetaData.getDatabaseProductName()}
     */
    boolean canHandle(String database);

    /**
     * Extracts all accessible tables for the configured schemas.
     *
     * @param databaseMetaData live JDBC metadata connection
     * @param includeAllSchemas when {@code true}, extract all non-system schemas
     * @param includedSchemas   when {@code includeAllSchemas} is {@code false},
     *                          only extract these schemas
     * @return list of {@link DbTable} with column metadata and PK info
     */
    List<DbTable> getTables(
            DatabaseMetaData databaseMetaData,
            boolean includeAllSchemas,
            List<String> includedSchemas
    );

    default boolean include(
            String schemaOrCatalog,
            List<String> excludedSchemasOrCatalogs
    ) {
        return excludedSchemasOrCatalogs.stream()
                .noneMatch(schemaOrCatalog::equalsIgnoreCase);
    }

    default List<String> getAllCatalogs(
            DatabaseMetaData databaseMetaData,
            List<String> excludedCatalogs
    ) throws Exception {
        List<String> includedCatalogs = new ArrayList<>();
        try (ResultSet resultSet = databaseMetaData.getCatalogs()) {
            while (resultSet.next()) {
                String catalog = resultSet.getString("TABLE_CAT");

                if (include(catalog, excludedCatalogs)) {
                    includedCatalogs.add(catalog);
                }
            }
        }

        return includedCatalogs;
    }

    default List<String> getAllSchemas(
            DatabaseMetaData databaseMetaData,
            List<String> excludedSchemas
    ) throws Exception {
        List<String> includedSchemas = new ArrayList<>();
        try (ResultSet resultSet = databaseMetaData.getSchemas()) {
            while (resultSet.next()) {
                String schema = resultSet.getString(ColumnLabel.TABLE_SCHEM.name());

                if (include(schema, excludedSchemas)) {
                    includedSchemas.add(schema);
                }
            }
        }

        return includedSchemas;
    }

    default List<MetaDataTable> getMetaTables(
            DatabaseMetaData databaseMetaData,
            String catalogPattern,
            String schemaPattern
    ) throws SQLException {
        List<MetaDataTable> tables = new ArrayList<>();

        try (ResultSet resultSet = databaseMetaData.getTables(
                catalogPattern,
                schemaPattern,
                null,
                new String[]{"TABLE", "VIEW"}
        )) {
            while (resultSet.next()) {
                String tableName = resultSet.getString(ColumnLabel.TABLE_NAME.name());
                String catalog = resultSet.getString(ColumnLabel.TABLE_CAT.name());
                String schema = resultSet.getString(ColumnLabel.TABLE_SCHEM.name());
                String tableType = resultSet.getString(ColumnLabel.TABLE_TYPE.name());
                String tableAlias = getAlias(tableName);
                MetaDataTable metaDataTable = new MetaDataTable(
                        tableName,
                        catalog,
                        schema,
                        tableType,
                        tableAlias
                );

                tables.add(metaDataTable);
            }
        }

        return tables;
    }

    default List<String> getAllPrimaryKeys(
            DatabaseMetaData databaseMetaData,
            String catalog,
            String schema,
            String tableName
    ) throws SQLException {
        List<String> pkColumns = new ArrayList<>();

        try (ResultSet primaryKeys = databaseMetaData.getPrimaryKeys(
                catalog,
                schema,
                tableName
        )) {
            while (primaryKeys.next()) {
                String primaryKeyColumnName = primaryKeys.getString(ColumnLabel.COLUMN_NAME.name());
                pkColumns.add(primaryKeyColumnName);
            }
        }

        return pkColumns;
    }
}
