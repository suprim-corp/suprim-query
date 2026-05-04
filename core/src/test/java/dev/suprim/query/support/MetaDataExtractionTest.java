package dev.suprim.query.support;

import dev.suprim.query.model.DbTable;
import dev.suprim.query.model.MetaDataTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.RowIdLifetime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MetaDataExtractionTest {

    private TestMetaDataExtraction extraction;

    @BeforeEach
    void setUp() {
        extraction = new TestMetaDataExtraction();
    }

    @Test
    void include_withIncludedSchema_shouldReturnTrue() {
        List<String> excluded = List.of("information_schema", "pg_catalog");

        boolean result = extraction.include("public", excluded);

        assertThat(result).isTrue();
    }

    @Test
    void include_withExcludedSchema_shouldReturnFalse() {
        List<String> excluded = List.of("information_schema", "pg_catalog");

        boolean result = extraction.include("information_schema", excluded);

        assertThat(result).isFalse();
    }

    @Test
    void include_withCaseInsensitiveMatch_shouldReturnFalse() {
        List<String> excluded = List.of("INFORMATION_SCHEMA");

        boolean result = extraction.include("information_schema", excluded);

        assertThat(result).isFalse();
    }

    @Test
    void include_withEmptyExcludeList_shouldReturnTrue() {
        boolean result = extraction.include("any_schema", List.of());

        assertThat(result).isTrue();
    }

    @Test
    void canHandle_withTestDB_shouldReturnTrue() {
        assertThat(extraction.canHandle("TestDB")).isTrue();
    }

    @Test
    void canHandle_withOtherDB_shouldReturnFalse() {
        assertThat(extraction.canHandle("PostgreSQL")).isFalse();
    }

    @Test
    void getTables_shouldReturnEmptyList() {
        List<DbTable> result = extraction.getTables(null, false, List.of());

        assertThat(result).isEmpty();
    }

    @Test
    void getAllCatalogs_shouldReturnFilteredCatalogs() throws Exception {
        StubDatabaseMetaData metaData = new StubDatabaseMetaData();
        metaData.setCatalogs(new String[]{"mydb", "information_schema", "postgres"});

        List<String> excluded = List.of("information_schema");
        List<String> result = extraction.getAllCatalogs(metaData, excluded);

        assertThat(result).containsExactly("mydb", "postgres");
    }

    @Test
    void getAllCatalogs_withEmptyResult_shouldReturnEmptyList() throws Exception {
        StubDatabaseMetaData metaData = new StubDatabaseMetaData();
        metaData.setCatalogs(new String[]{});

        List<String> result = extraction.getAllCatalogs(metaData, List.of());

        assertThat(result).isEmpty();
    }

    @Test
    void getAllSchemas_shouldReturnFilteredSchemas() throws Exception {
        StubDatabaseMetaData metaData = new StubDatabaseMetaData();
        metaData.setSchemas(new String[]{"public", "pg_catalog", "myschema"});

        List<String> excluded = List.of("pg_catalog");
        List<String> result = extraction.getAllSchemas(metaData, excluded);

        assertThat(result).containsExactly("public", "myschema");
    }

    @Test
    void getAllSchemas_withEmptyResult_shouldReturnEmptyList() throws Exception {
        StubDatabaseMetaData metaData = new StubDatabaseMetaData();
        metaData.setSchemas(new String[]{});

        List<String> result = extraction.getAllSchemas(metaData, List.of());

        assertThat(result).isEmpty();
    }

    @Test
    void getMetaTables_shouldReturnTableList() throws SQLException {
        StubDatabaseMetaData metaData = new StubDatabaseMetaData();
        metaData.setTables(new String[][]{
                {"users", "mydb", "public", "TABLE"},
                {"orders", "mydb", "public", "TABLE"}
        });

        List<MetaDataTable> result = extraction.getMetaTables(metaData, "mydb", "public");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).tableName()).isEqualTo("users");
        assertThat(result.get(1).tableName()).isEqualTo("orders");
    }

    @Test
    void getMetaTables_shouldGenerateAlias() throws SQLException {
        StubDatabaseMetaData metaData = new StubDatabaseMetaData();
        metaData.setTables(new String[][]{
                {"users", "mydb", "public", "TABLE"}
        });

        List<MetaDataTable> result = extraction.getMetaTables(metaData, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).tableAlias()).startsWith("user_");
    }

    @Test
    void getMetaTables_withEmptyResult_shouldReturnEmptyList() throws SQLException {
        StubDatabaseMetaData metaData = new StubDatabaseMetaData();
        metaData.setTables(new String[][]{});

        List<MetaDataTable> result = extraction.getMetaTables(metaData, null, null);

        assertThat(result).isEmpty();
    }

    @Test
    void getAllPrimaryKeys_shouldReturnPkColumns() throws SQLException {
        StubDatabaseMetaData metaData = new StubDatabaseMetaData();
        metaData.setPrimaryKeys(new String[]{"id", "tenant_id"});

        List<String> result = extraction.getAllPrimaryKeys(metaData, "mydb", "public", "users");

        assertThat(result).containsExactly("id", "tenant_id");
    }

    @Test
    void getAllPrimaryKeys_withNoPks_shouldReturnEmptyList() throws SQLException {
        StubDatabaseMetaData metaData = new StubDatabaseMetaData();
        metaData.setPrimaryKeys(new String[]{});

        List<String> result = extraction.getAllPrimaryKeys(metaData, "mydb", "public", "users");

        assertThat(result).isEmpty();
    }

    // Concrete implementation for testing
    private static class TestMetaDataExtraction implements MetaDataExtraction {
        @Override
        public boolean canHandle(String database) {
            return "TestDB".equals(database);
        }

        @Override
        public List<DbTable> getTables(java.sql.DatabaseMetaData databaseMetaData, boolean includeAllSchemas, List<String> includedSchemas) {
            return List.of();
        }
    }

    // Stub implementation of DatabaseMetaData for testing without Mockito
    private static class StubDatabaseMetaData implements DatabaseMetaData {
        private String[] catalogs = {};
        private String[] schemas = {};
        private String[][] tables = {};
        private String[] primaryKeys = {};

        public void setCatalogs(String[] catalogs) { this.catalogs = catalogs; }
        public void setSchemas(String[] schemas) { this.schemas = schemas; }
        public void setTables(String[][] tables) { this.tables = tables; }
        public void setPrimaryKeys(String[] primaryKeys) { this.primaryKeys = primaryKeys; }

        @Override
        public ResultSet getCatalogs() throws SQLException {
            return new ArrayResultSet(catalogs, "TABLE_CAT");
        }

        @Override
        public ResultSet getSchemas() throws SQLException {
            return new ArrayResultSet(schemas, "TABLE_SCHEM");
        }

        @Override
        public ResultSet getTables(String catalog, String schemaPattern, String tableNamePattern, String[] types) throws SQLException {
            return new TableResultSet(tables);
        }

        @Override
        public ResultSet getPrimaryKeys(String catalog, String schema, String table) throws SQLException {
            return new ArrayResultSet(primaryKeys, "COLUMN_NAME");
        }

        // All other methods throw UnsupportedOperationException or return defaults
        @Override public boolean allProceduresAreCallable() { return false; }
        @Override public boolean allTablesAreSelectable() { return false; }
        @Override public String getURL() { return null; }
        @Override public String getUserName() { return null; }
        @Override public boolean isReadOnly() { return false; }
        @Override public boolean nullsAreSortedHigh() { return false; }
        @Override public boolean nullsAreSortedLow() { return false; }
        @Override public boolean nullsAreSortedAtStart() { return false; }
        @Override public boolean nullsAreSortedAtEnd() { return false; }
        @Override public String getDatabaseProductName() { return null; }
        @Override public String getDatabaseProductVersion() { return null; }
        @Override public String getDriverName() { return null; }
        @Override public String getDriverVersion() { return null; }
        @Override public int getDriverMajorVersion() { return 0; }
        @Override public int getDriverMinorVersion() { return 0; }
        @Override public boolean usesLocalFiles() { return false; }
        @Override public boolean usesLocalFilePerTable() { return false; }
        @Override public boolean supportsMixedCaseIdentifiers() { return false; }
        @Override public boolean storesUpperCaseIdentifiers() { return false; }
        @Override public boolean storesLowerCaseIdentifiers() { return false; }
        @Override public boolean storesMixedCaseIdentifiers() { return false; }
        @Override public boolean supportsMixedCaseQuotedIdentifiers() { return false; }
        @Override public boolean storesUpperCaseQuotedIdentifiers() { return false; }
        @Override public boolean storesLowerCaseQuotedIdentifiers() { return false; }
        @Override public boolean storesMixedCaseQuotedIdentifiers() { return false; }
        @Override public String getIdentifierQuoteString() { return null; }
        @Override public String getSQLKeywords() { return null; }
        @Override public String getNumericFunctions() { return null; }
        @Override public String getStringFunctions() { return null; }
        @Override public String getSystemFunctions() { return null; }
        @Override public String getTimeDateFunctions() { return null; }
        @Override public String getSearchStringEscape() { return null; }
        @Override public String getExtraNameCharacters() { return null; }
        @Override public boolean supportsAlterTableWithAddColumn() { return false; }
        @Override public boolean supportsAlterTableWithDropColumn() { return false; }
        @Override public boolean supportsColumnAliasing() { return false; }
        @Override public boolean nullPlusNonNullIsNull() { return false; }
        @Override public boolean supportsConvert() { return false; }
        @Override public boolean supportsConvert(int fromType, int toType) { return false; }
        @Override public boolean supportsTableCorrelationNames() { return false; }
        @Override public boolean supportsDifferentTableCorrelationNames() { return false; }
        @Override public boolean supportsExpressionsInOrderBy() { return false; }
        @Override public boolean supportsOrderByUnrelated() { return false; }
        @Override public boolean supportsGroupBy() { return false; }
        @Override public boolean supportsGroupByUnrelated() { return false; }
        @Override public boolean supportsGroupByBeyondSelect() { return false; }
        @Override public boolean supportsLikeEscapeClause() { return false; }
        @Override public boolean supportsMultipleResultSets() { return false; }
        @Override public boolean supportsMultipleTransactions() { return false; }
        @Override public boolean supportsNonNullableColumns() { return false; }
        @Override public boolean supportsMinimumSQLGrammar() { return false; }
        @Override public boolean supportsCoreSQLGrammar() { return false; }
        @Override public boolean supportsExtendedSQLGrammar() { return false; }
        @Override public boolean supportsANSI92EntryLevelSQL() { return false; }
        @Override public boolean supportsANSI92IntermediateSQL() { return false; }
        @Override public boolean supportsANSI92FullSQL() { return false; }
        @Override public boolean supportsIntegrityEnhancementFacility() { return false; }
        @Override public boolean supportsOuterJoins() { return false; }
        @Override public boolean supportsFullOuterJoins() { return false; }
        @Override public boolean supportsLimitedOuterJoins() { return false; }
        @Override public String getSchemaTerm() { return null; }
        @Override public String getProcedureTerm() { return null; }
        @Override public String getCatalogTerm() { return null; }
        @Override public boolean isCatalogAtStart() { return false; }
        @Override public String getCatalogSeparator() { return null; }
        @Override public boolean supportsSchemasInDataManipulation() { return false; }
        @Override public boolean supportsSchemasInProcedureCalls() { return false; }
        @Override public boolean supportsSchemasInTableDefinitions() { return false; }
        @Override public boolean supportsSchemasInIndexDefinitions() { return false; }
        @Override public boolean supportsSchemasInPrivilegeDefinitions() { return false; }
        @Override public boolean supportsCatalogsInDataManipulation() { return false; }
        @Override public boolean supportsCatalogsInProcedureCalls() { return false; }
        @Override public boolean supportsCatalogsInTableDefinitions() { return false; }
        @Override public boolean supportsCatalogsInIndexDefinitions() { return false; }
        @Override public boolean supportsCatalogsInPrivilegeDefinitions() { return false; }
        @Override public boolean supportsPositionedDelete() { return false; }
        @Override public boolean supportsPositionedUpdate() { return false; }
        @Override public boolean supportsSelectForUpdate() { return false; }
        @Override public boolean supportsStoredProcedures() { return false; }
        @Override public boolean supportsSubqueriesInComparisons() { return false; }
        @Override public boolean supportsSubqueriesInExists() { return false; }
        @Override public boolean supportsSubqueriesInIns() { return false; }
        @Override public boolean supportsSubqueriesInQuantifieds() { return false; }
        @Override public boolean supportsCorrelatedSubqueries() { return false; }
        @Override public boolean supportsUnion() { return false; }
        @Override public boolean supportsUnionAll() { return false; }
        @Override public boolean supportsOpenCursorsAcrossCommit() { return false; }
        @Override public boolean supportsOpenCursorsAcrossRollback() { return false; }
        @Override public boolean supportsOpenStatementsAcrossCommit() { return false; }
        @Override public boolean supportsOpenStatementsAcrossRollback() { return false; }
        @Override public int getMaxBinaryLiteralLength() { return 0; }
        @Override public int getMaxCharLiteralLength() { return 0; }
        @Override public int getMaxColumnNameLength() { return 0; }
        @Override public int getMaxColumnsInGroupBy() { return 0; }
        @Override public int getMaxColumnsInIndex() { return 0; }
        @Override public int getMaxColumnsInOrderBy() { return 0; }
        @Override public int getMaxColumnsInSelect() { return 0; }
        @Override public int getMaxColumnsInTable() { return 0; }
        @Override public int getMaxConnections() { return 0; }
        @Override public int getMaxCursorNameLength() { return 0; }
        @Override public int getMaxIndexLength() { return 0; }
        @Override public int getMaxSchemaNameLength() { return 0; }
        @Override public int getMaxProcedureNameLength() { return 0; }
        @Override public int getMaxCatalogNameLength() { return 0; }
        @Override public int getMaxRowSize() { return 0; }
        @Override public boolean doesMaxRowSizeIncludeBlobs() { return false; }
        @Override public int getMaxStatementLength() { return 0; }
        @Override public int getMaxStatements() { return 0; }
        @Override public int getMaxTableNameLength() { return 0; }
        @Override public int getMaxTablesInSelect() { return 0; }
        @Override public int getMaxUserNameLength() { return 0; }
        @Override public int getDefaultTransactionIsolation() { return 0; }
        @Override public boolean supportsTransactions() { return false; }
        @Override public boolean supportsTransactionIsolationLevel(int level) { return false; }
        @Override public boolean supportsDataDefinitionAndDataManipulationTransactions() { return false; }
        @Override public boolean supportsDataManipulationTransactionsOnly() { return false; }
        @Override public boolean dataDefinitionCausesTransactionCommit() { return false; }
        @Override public boolean dataDefinitionIgnoredInTransactions() { return false; }
        @Override public ResultSet getProcedures(String catalog, String schemaPattern, String procedureNamePattern) { return null; }
        @Override public ResultSet getProcedureColumns(String catalog, String schemaPattern, String procedureNamePattern, String columnNamePattern) { return null; }
        @Override public ResultSet getSchemas(String catalog, String schemaPattern) { return null; }
        @Override public ResultSet getTableTypes() { return null; }
        @Override public ResultSet getColumns(String catalog, String schemaPattern, String tableNamePattern, String columnNamePattern) { return null; }
        @Override public ResultSet getColumnPrivileges(String catalog, String schema, String table, String columnNamePattern) { return null; }
        @Override public ResultSet getTablePrivileges(String catalog, String schemaPattern, String tableNamePattern) { return null; }
        @Override public ResultSet getBestRowIdentifier(String catalog, String schema, String table, int scope, boolean nullable) { return null; }
        @Override public ResultSet getVersionColumns(String catalog, String schema, String table) { return null; }
        @Override public ResultSet getExportedKeys(String catalog, String schema, String table) { return null; }
        @Override public ResultSet getImportedKeys(String catalog, String schema, String table) { return null; }
        @Override public ResultSet getCrossReference(String parentCatalog, String parentSchema, String parentTable, String foreignCatalog, String foreignSchema, String foreignTable) { return null; }
        @Override public ResultSet getTypeInfo() { return null; }
        @Override public ResultSet getIndexInfo(String catalog, String schema, String table, boolean unique, boolean approximate) { return null; }
        @Override public boolean supportsResultSetType(int type) { return false; }
        @Override public boolean supportsResultSetConcurrency(int type, int concurrency) { return false; }
        @Override public boolean ownUpdatesAreVisible(int type) { return false; }
        @Override public boolean ownDeletesAreVisible(int type) { return false; }
        @Override public boolean ownInsertsAreVisible(int type) { return false; }
        @Override public boolean othersUpdatesAreVisible(int type) { return false; }
        @Override public boolean othersDeletesAreVisible(int type) { return false; }
        @Override public boolean othersInsertsAreVisible(int type) { return false; }
        @Override public boolean updatesAreDetected(int type) { return false; }
        @Override public boolean deletesAreDetected(int type) { return false; }
        @Override public boolean insertsAreDetected(int type) { return false; }
        @Override public boolean supportsBatchUpdates() { return false; }
        @Override public ResultSet getUDTs(String catalog, String schemaPattern, String typeNamePattern, int[] types) { return null; }
        @Override public Connection getConnection() { return null; }
        @Override public boolean supportsSavepoints() { return false; }
        @Override public boolean supportsNamedParameters() { return false; }
        @Override public boolean supportsMultipleOpenResults() { return false; }
        @Override public boolean supportsGetGeneratedKeys() { return false; }
        @Override public ResultSet getSuperTypes(String catalog, String schemaPattern, String typeNamePattern) { return null; }
        @Override public ResultSet getSuperTables(String catalog, String schemaPattern, String tableNamePattern) { return null; }
        @Override public ResultSet getAttributes(String catalog, String schemaPattern, String typeNamePattern, String attributeNamePattern) { return null; }
        @Override public boolean supportsResultSetHoldability(int holdability) { return false; }
        @Override public int getResultSetHoldability() { return 0; }
        @Override public int getDatabaseMajorVersion() { return 0; }
        @Override public int getDatabaseMinorVersion() { return 0; }
        @Override public int getJDBCMajorVersion() { return 0; }
        @Override public int getJDBCMinorVersion() { return 0; }
        @Override public int getSQLStateType() { return 0; }
        @Override public boolean locatorsUpdateCopy() { return false; }
        @Override public boolean supportsStatementPooling() { return false; }
        @Override public RowIdLifetime getRowIdLifetime() { return null; }
        @Override public boolean supportsStoredFunctionsUsingCallSyntax() { return false; }
        @Override public boolean autoCommitFailureClosesAllResultSets() { return false; }
        @Override public ResultSet getClientInfoProperties() { return null; }
        @Override public ResultSet getFunctions(String catalog, String schemaPattern, String functionNamePattern) { return null; }
        @Override public ResultSet getFunctionColumns(String catalog, String schemaPattern, String functionNamePattern, String columnNamePattern) { return null; }
        @Override public ResultSet getPseudoColumns(String catalog, String schemaPattern, String tableNamePattern, String columnNamePattern) { return null; }
        @Override public boolean generatedKeyAlwaysReturned() { return false; }
        @Override public <T> T unwrap(Class<T> iface) { return null; }
        @Override public boolean isWrapperFor(Class<?> iface) { return false; }
    }

    // Simple ResultSet implementation for single-column string arrays
    private static class ArrayResultSet implements ResultSet {
        private final String[] data;
        private final String columnName;
        private int index = -1;

        ArrayResultSet(String[] data, String columnName) {
            this.data = data;
            this.columnName = columnName;
        }

        @Override public boolean next() { return ++index < data.length; }
        @Override public String getString(String col) { return columnName.equals(col) ? data[index] : null; }
        @Override public void close() {}

        // All other methods throw or return null
        @Override public boolean wasNull() { return false; }
        @Override public String getString(int columnIndex) { return null; }
        @Override public boolean getBoolean(int columnIndex) { return false; }
        @Override public byte getByte(int columnIndex) { return 0; }
        @Override public short getShort(int columnIndex) { return 0; }
        @Override public int getInt(int columnIndex) { return 0; }
        @Override public long getLong(int columnIndex) { return 0; }
        @Override public float getFloat(int columnIndex) { return 0; }
        @Override public double getDouble(int columnIndex) { return 0; }
        @Override public java.math.BigDecimal getBigDecimal(int columnIndex, int scale) { return null; }
        @Override public byte[] getBytes(int columnIndex) { return null; }
        @Override public java.sql.Date getDate(int columnIndex) { return null; }
        @Override public java.sql.Time getTime(int columnIndex) { return null; }
        @Override public java.sql.Timestamp getTimestamp(int columnIndex) { return null; }
        @Override public java.io.InputStream getAsciiStream(int columnIndex) { return null; }
        @Override public java.io.InputStream getUnicodeStream(int columnIndex) { return null; }
        @Override public java.io.InputStream getBinaryStream(int columnIndex) { return null; }
        @Override public boolean getBoolean(String columnLabel) { return false; }
        @Override public byte getByte(String columnLabel) { return 0; }
        @Override public short getShort(String columnLabel) { return 0; }
        @Override public int getInt(String columnLabel) { return 0; }
        @Override public long getLong(String columnLabel) { return 0; }
        @Override public float getFloat(String columnLabel) { return 0; }
        @Override public double getDouble(String columnLabel) { return 0; }
        @Override public java.math.BigDecimal getBigDecimal(String columnLabel, int scale) { return null; }
        @Override public byte[] getBytes(String columnLabel) { return null; }
        @Override public java.sql.Date getDate(String columnLabel) { return null; }
        @Override public java.sql.Time getTime(String columnLabel) { return null; }
        @Override public java.sql.Timestamp getTimestamp(String columnLabel) { return null; }
        @Override public java.io.InputStream getAsciiStream(String columnLabel) { return null; }
        @Override public java.io.InputStream getUnicodeStream(String columnLabel) { return null; }
        @Override public java.io.InputStream getBinaryStream(String columnLabel) { return null; }
        @Override public java.sql.SQLWarning getWarnings() { return null; }
        @Override public void clearWarnings() {}
        @Override public String getCursorName() { return null; }
        @Override public ResultSetMetaData getMetaData() { return null; }
        @Override public Object getObject(int columnIndex) { return null; }
        @Override public Object getObject(String columnLabel) { return null; }
        @Override public int findColumn(String columnLabel) { return 0; }
        @Override public java.io.Reader getCharacterStream(int columnIndex) { return null; }
        @Override public java.io.Reader getCharacterStream(String columnLabel) { return null; }
        @Override public java.math.BigDecimal getBigDecimal(int columnIndex) { return null; }
        @Override public java.math.BigDecimal getBigDecimal(String columnLabel) { return null; }
        @Override public boolean isBeforeFirst() { return false; }
        @Override public boolean isAfterLast() { return false; }
        @Override public boolean isFirst() { return false; }
        @Override public boolean isLast() { return false; }
        @Override public void beforeFirst() {}
        @Override public void afterLast() {}
        @Override public boolean first() { return false; }
        @Override public boolean last() { return false; }
        @Override public int getRow() { return 0; }
        @Override public boolean absolute(int row) { return false; }
        @Override public boolean relative(int rows) { return false; }
        @Override public boolean previous() { return false; }
        @Override public void setFetchDirection(int direction) {}
        @Override public int getFetchDirection() { return 0; }
        @Override public void setFetchSize(int rows) {}
        @Override public int getFetchSize() { return 0; }
        @Override public int getType() { return 0; }
        @Override public int getConcurrency() { return 0; }
        @Override public boolean rowUpdated() { return false; }
        @Override public boolean rowInserted() { return false; }
        @Override public boolean rowDeleted() { return false; }
        @Override public void updateNull(int columnIndex) {}
        @Override public void updateBoolean(int columnIndex, boolean x) {}
        @Override public void updateByte(int columnIndex, byte x) {}
        @Override public void updateShort(int columnIndex, short x) {}
        @Override public void updateInt(int columnIndex, int x) {}
        @Override public void updateLong(int columnIndex, long x) {}
        @Override public void updateFloat(int columnIndex, float x) {}
        @Override public void updateDouble(int columnIndex, double x) {}
        @Override public void updateBigDecimal(int columnIndex, java.math.BigDecimal x) {}
        @Override public void updateString(int columnIndex, String x) {}
        @Override public void updateBytes(int columnIndex, byte[] x) {}
        @Override public void updateDate(int columnIndex, java.sql.Date x) {}
        @Override public void updateTime(int columnIndex, java.sql.Time x) {}
        @Override public void updateTimestamp(int columnIndex, java.sql.Timestamp x) {}
        @Override public void updateAsciiStream(int columnIndex, java.io.InputStream x, int length) {}
        @Override public void updateBinaryStream(int columnIndex, java.io.InputStream x, int length) {}
        @Override public void updateCharacterStream(int columnIndex, java.io.Reader x, int length) {}
        @Override public void updateObject(int columnIndex, Object x, int scaleOrLength) {}
        @Override public void updateObject(int columnIndex, Object x) {}
        @Override public void updateNull(String columnLabel) {}
        @Override public void updateBoolean(String columnLabel, boolean x) {}
        @Override public void updateByte(String columnLabel, byte x) {}
        @Override public void updateShort(String columnLabel, short x) {}
        @Override public void updateInt(String columnLabel, int x) {}
        @Override public void updateLong(String columnLabel, long x) {}
        @Override public void updateFloat(String columnLabel, float x) {}
        @Override public void updateDouble(String columnLabel, double x) {}
        @Override public void updateBigDecimal(String columnLabel, java.math.BigDecimal x) {}
        @Override public void updateString(String columnLabel, String x) {}
        @Override public void updateBytes(String columnLabel, byte[] x) {}
        @Override public void updateDate(String columnLabel, java.sql.Date x) {}
        @Override public void updateTime(String columnLabel, java.sql.Time x) {}
        @Override public void updateTimestamp(String columnLabel, java.sql.Timestamp x) {}
        @Override public void updateAsciiStream(String columnLabel, java.io.InputStream x, int length) {}
        @Override public void updateBinaryStream(String columnLabel, java.io.InputStream x, int length) {}
        @Override public void updateCharacterStream(String columnLabel, java.io.Reader x, int length) {}
        @Override public void updateObject(String columnLabel, Object x, int scaleOrLength) {}
        @Override public void updateObject(String columnLabel, Object x) {}
        @Override public void insertRow() {}
        @Override public void updateRow() {}
        @Override public void deleteRow() {}
        @Override public void refreshRow() {}
        @Override public void cancelRowUpdates() {}
        @Override public void moveToInsertRow() {}
        @Override public void moveToCurrentRow() {}
        @Override public Statement getStatement() { return null; }
        @Override public Object getObject(int columnIndex, java.util.Map<String, Class<?>> map) { return null; }
        @Override public java.sql.Ref getRef(int columnIndex) { return null; }
        @Override public java.sql.Blob getBlob(int columnIndex) { return null; }
        @Override public java.sql.Clob getClob(int columnIndex) { return null; }
        @Override public java.sql.Array getArray(int columnIndex) { return null; }
        @Override public Object getObject(String columnLabel, java.util.Map<String, Class<?>> map) { return null; }
        @Override public java.sql.Ref getRef(String columnLabel) { return null; }
        @Override public java.sql.Blob getBlob(String columnLabel) { return null; }
        @Override public java.sql.Clob getClob(String columnLabel) { return null; }
        @Override public java.sql.Array getArray(String columnLabel) { return null; }
        @Override public java.sql.Date getDate(int columnIndex, java.util.Calendar cal) { return null; }
        @Override public java.sql.Date getDate(String columnLabel, java.util.Calendar cal) { return null; }
        @Override public java.sql.Time getTime(int columnIndex, java.util.Calendar cal) { return null; }
        @Override public java.sql.Time getTime(String columnLabel, java.util.Calendar cal) { return null; }
        @Override public java.sql.Timestamp getTimestamp(int columnIndex, java.util.Calendar cal) { return null; }
        @Override public java.sql.Timestamp getTimestamp(String columnLabel, java.util.Calendar cal) { return null; }
        @Override public java.net.URL getURL(int columnIndex) { return null; }
        @Override public java.net.URL getURL(String columnLabel) { return null; }
        @Override public void updateRef(int columnIndex, java.sql.Ref x) {}
        @Override public void updateRef(String columnLabel, java.sql.Ref x) {}
        @Override public void updateBlob(int columnIndex, java.sql.Blob x) {}
        @Override public void updateBlob(String columnLabel, java.sql.Blob x) {}
        @Override public void updateClob(int columnIndex, java.sql.Clob x) {}
        @Override public void updateClob(String columnLabel, java.sql.Clob x) {}
        @Override public void updateArray(int columnIndex, java.sql.Array x) {}
        @Override public void updateArray(String columnLabel, java.sql.Array x) {}
        @Override public java.sql.RowId getRowId(int columnIndex) { return null; }
        @Override public java.sql.RowId getRowId(String columnLabel) { return null; }
        @Override public void updateRowId(int columnIndex, java.sql.RowId x) {}
        @Override public void updateRowId(String columnLabel, java.sql.RowId x) {}
        @Override public int getHoldability() { return 0; }
        @Override public boolean isClosed() { return false; }
        @Override public void updateNString(int columnIndex, String nString) {}
        @Override public void updateNString(String columnLabel, String nString) {}
        @Override public void updateNClob(int columnIndex, java.sql.NClob nClob) {}
        @Override public void updateNClob(String columnLabel, java.sql.NClob nClob) {}
        @Override public java.sql.NClob getNClob(int columnIndex) { return null; }
        @Override public java.sql.NClob getNClob(String columnLabel) { return null; }
        @Override public java.sql.SQLXML getSQLXML(int columnIndex) { return null; }
        @Override public java.sql.SQLXML getSQLXML(String columnLabel) { return null; }
        @Override public void updateSQLXML(int columnIndex, java.sql.SQLXML xmlObject) {}
        @Override public void updateSQLXML(String columnLabel, java.sql.SQLXML xmlObject) {}
        @Override public String getNString(int columnIndex) { return null; }
        @Override public String getNString(String columnLabel) { return null; }
        @Override public java.io.Reader getNCharacterStream(int columnIndex) { return null; }
        @Override public java.io.Reader getNCharacterStream(String columnLabel) { return null; }
        @Override public void updateNCharacterStream(int columnIndex, java.io.Reader x, long length) {}
        @Override public void updateNCharacterStream(String columnLabel, java.io.Reader reader, long length) {}
        @Override public void updateAsciiStream(int columnIndex, java.io.InputStream x, long length) {}
        @Override public void updateBinaryStream(int columnIndex, java.io.InputStream x, long length) {}
        @Override public void updateCharacterStream(int columnIndex, java.io.Reader x, long length) {}
        @Override public void updateAsciiStream(String columnLabel, java.io.InputStream x, long length) {}
        @Override public void updateBinaryStream(String columnLabel, java.io.InputStream x, long length) {}
        @Override public void updateCharacterStream(String columnLabel, java.io.Reader reader, long length) {}
        @Override public void updateBlob(int columnIndex, java.io.InputStream inputStream, long length) {}
        @Override public void updateBlob(String columnLabel, java.io.InputStream inputStream, long length) {}
        @Override public void updateClob(int columnIndex, java.io.Reader reader, long length) {}
        @Override public void updateClob(String columnLabel, java.io.Reader reader, long length) {}
        @Override public void updateNClob(int columnIndex, java.io.Reader reader, long length) {}
        @Override public void updateNClob(String columnLabel, java.io.Reader reader, long length) {}
        @Override public void updateNCharacterStream(int columnIndex, java.io.Reader x) {}
        @Override public void updateNCharacterStream(String columnLabel, java.io.Reader reader) {}
        @Override public void updateAsciiStream(int columnIndex, java.io.InputStream x) {}
        @Override public void updateBinaryStream(int columnIndex, java.io.InputStream x) {}
        @Override public void updateCharacterStream(int columnIndex, java.io.Reader x) {}
        @Override public void updateAsciiStream(String columnLabel, java.io.InputStream x) {}
        @Override public void updateBinaryStream(String columnLabel, java.io.InputStream x) {}
        @Override public void updateCharacterStream(String columnLabel, java.io.Reader reader) {}
        @Override public void updateBlob(int columnIndex, java.io.InputStream inputStream) {}
        @Override public void updateBlob(String columnLabel, java.io.InputStream inputStream) {}
        @Override public void updateClob(int columnIndex, java.io.Reader reader) {}
        @Override public void updateClob(String columnLabel, java.io.Reader reader) {}
        @Override public void updateNClob(int columnIndex, java.io.Reader reader) {}
        @Override public void updateNClob(String columnLabel, java.io.Reader reader) {}
        @Override public <T> T getObject(int columnIndex, Class<T> type) { return null; }
        @Override public <T> T getObject(String columnLabel, Class<T> type) { return null; }
        @Override public <T> T unwrap(Class<T> iface) { return null; }
        @Override public boolean isWrapperFor(Class<?> iface) { return false; }
    }

    // ResultSet for table metadata
    private static class TableResultSet extends ArrayResultSet {
        private final String[][] tables;
        private int idx = -1;

        TableResultSet(String[][] tables) {
            super(new String[0], "");
            this.tables = tables;
        }

        @Override public boolean next() { return ++idx < tables.length; }

        @Override
        public String getString(String col) {
            if (idx < 0 || idx >= tables.length) return null;
            return switch (col) {
                case "TABLE_NAME" -> tables[idx][0];
                case "TABLE_CAT" -> tables[idx][1];
                case "TABLE_SCHEM" -> tables[idx][2];
                case "TABLE_TYPE" -> tables[idx][3];
                default -> null;
            };
        }
    }
}
