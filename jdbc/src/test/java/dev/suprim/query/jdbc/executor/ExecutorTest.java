package dev.suprim.query.jdbc.executor;

import dev.suprim.query.dialect.Dialect;
import dev.suprim.query.exception.DbErrorCode;
import dev.suprim.query.exception.DbException;
import dev.suprim.query.jdbc.executor.creation.JdbcCreationService;
import dev.suprim.query.jdbc.executor.deletion.JdbcDeleteService;
import dev.suprim.query.jdbc.executor.raw.JdbcRawQueryService;
import dev.suprim.query.jdbc.executor.read.JdbcReadService;
import dev.suprim.query.jdbc.executor.update.JdbcUpdateService;
import dev.suprim.query.jdbc.operation.DbOperationService;
import dev.suprim.query.jdbc.operation.JdbcManager;
import dev.suprim.query.jdbc.operation.SqlCreatorTemplate;
import dev.suprim.query.jdbc.processor.ReadProcessor;
import dev.suprim.query.jdbc.processor.TSIDProcessor;
import dev.suprim.query.model.DbColumn;
import dev.suprim.query.model.DbTable;
import dev.suprim.query.model.UpsertConfig;
import dev.suprim.query.model.context.ReadContext;
import dev.suprim.query.model.dto.BulkUpdate;
import dev.suprim.query.model.dto.CountResponse;
import dev.suprim.query.model.dto.CreationResponse;
import dev.suprim.query.model.dto.Page;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExecutorTest {

    @Mock
    private JdbcManager jdbcManager;
    @Mock
    private DbOperationService dbOperationService;
    @Mock
    private SqlCreatorTemplate sqlCreatorTemplate;
    @Mock
    private TSIDProcessor tsidProcessor;
    @Mock
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    @Mock
    private TransactionTemplate transactionTemplate;
    @Mock
    private Dialect dialect;

    private static DbTable usersTable;

    @BeforeAll
    static void buildTables() {
        usersTable = new DbTable(
                "public", "users", "\"public\".\"users\"", "t0",
                List.of(
                        new DbColumn("users", "id", "", "t0", true, "int8", false, false, Long.class, "\"", ""),
                        new DbColumn("users", "name", "", "t0", false, "varchar", false, false, String.class, "\"", ""),
                        new DbColumn("users", "email", "", "t0", false, "varchar", false, false, String.class, "\"", "")
                ),
                "TABLE", "\""
        );
    }

    @Nested
    @DisplayName("JdbcCreationService Tests")
    class JdbcCreationServiceTests {

        private JdbcCreationService creationService;

        @BeforeEach
        void setup() {
            creationService = new JdbcCreationService(tsidProcessor, sqlCreatorTemplate, jdbcManager, dbOperationService);
        }

        @Test
        void execute_simpleInsert_returnsCreationResponse() throws Exception {
            when(jdbcManager.getTable("test", null, "users")).thenReturn(usersTable);
            when(jdbcManager.getDialect("test")).thenReturn(dialect);
            when(jdbcManager.getNamedParameterJdbcTemplate("test")).thenReturn(namedParameterJdbcTemplate);
            when(jdbcManager.getTxnTemplate("test")).thenReturn(transactionTemplate);
            when(sqlCreatorTemplate.create(any())).thenReturn("INSERT INTO users (name, email) VALUES (:name, :email)");

            CreationResponse expectedResponse = new CreationResponse(1, Map.of("id", 1L));
            when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
                TransactionCallback<?> callback = invocation.getArgument(0);
                return callback.doInTransaction(null);
            });
            when(dbOperationService.create(eq(namedParameterJdbcTemplate), anyMap(), anyString(), eq(usersTable)))
                    .thenReturn(expectedResponse);

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("name", "John");
            data.put("email", "john@test.com");

            CreationResponse result = creationService.execute("test", null, "users", null, data, false, null);

            assertThat(result).isNotNull();
            assertThat(result.row()).isEqualTo(1);
        }

        @Test
        void execute_withSpecificColumns_usesProvidedColumns() throws Exception {
            when(jdbcManager.getTable("test", null, "users")).thenReturn(usersTable);
            when(jdbcManager.getDialect("test")).thenReturn(dialect);
            when(jdbcManager.getNamedParameterJdbcTemplate("test")).thenReturn(namedParameterJdbcTemplate);
            when(jdbcManager.getTxnTemplate("test")).thenReturn(transactionTemplate);
            when(sqlCreatorTemplate.create(any())).thenReturn("INSERT INTO users (name) VALUES (:name)");

            CreationResponse expectedResponse = new CreationResponse(1, Map.of("id", 1L));
            when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
                TransactionCallback<?> callback = invocation.getArgument(0);
                return callback.doInTransaction(null);
            });
            when(dbOperationService.create(eq(namedParameterJdbcTemplate), anyMap(), anyString(), eq(usersTable)))
                    .thenReturn(expectedResponse);

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("name", "John");
            data.put("email", "john@test.com");

            CreationResponse result = creationService.execute("test", null, "users", List.of("name"), data, false, null);

            assertThat(result).isNotNull();
            assertThat(result.row()).isEqualTo(1);
        }

        @Test
        void execute_withTsIdEnabled_generatesTsId() throws Exception {
            when(jdbcManager.getTable("test", null, "users")).thenReturn(usersTable);
            when(jdbcManager.getDialect("test")).thenReturn(dialect);
            when(jdbcManager.getNamedParameterJdbcTemplate("test")).thenReturn(namedParameterJdbcTemplate);
            when(jdbcManager.getTxnTemplate("test")).thenReturn(transactionTemplate);
            when(sqlCreatorTemplate.create(any())).thenReturn("INSERT INTO users (id, name) VALUES (:id, :name)");
            when(tsidProcessor.processTsId(anyMap(), anyList())).thenReturn(Map.of("id", 123456L));

            CreationResponse expectedResponse = new CreationResponse(1, null);
            when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
                TransactionCallback<?> callback = invocation.getArgument(0);
                return callback.doInTransaction(null);
            });
            when(dbOperationService.create(eq(namedParameterJdbcTemplate), anyMap(), anyString(), eq(usersTable)))
                    .thenReturn(expectedResponse);

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("name", "John");

            CreationResponse result = creationService.execute("test", null, "users", null, data, true, null);

            assertThat(result).isNotNull();
            // When tsIdEnabled and keys is null, returns tsIdMap as keys
            assertThat(result.keys()).containsEntry("id", 123456L);
            verify(tsidProcessor).processTsId(anyMap(), anyList());
        }

        @Test
        void execute_duplicateKeyError_throwsRuntimeException() throws Exception {
            when(jdbcManager.getTable("test", null, "users")).thenReturn(usersTable);
            when(jdbcManager.getDialect("test")).thenReturn(dialect);
            when(jdbcManager.getNamedParameterJdbcTemplate("test")).thenReturn(namedParameterJdbcTemplate);
            when(jdbcManager.getTxnTemplate("test")).thenReturn(transactionTemplate);
            when(sqlCreatorTemplate.create(any())).thenReturn("INSERT INTO users (name) VALUES (:name)");

            when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
                TransactionCallback<?> callback = invocation.getArgument(0);
                return callback.doInTransaction(null);
            });
            when(dbOperationService.create(eq(namedParameterJdbcTemplate), anyMap(), anyString(), eq(usersTable)))
                    .thenThrow(new DuplicateKeyException("Duplicate key"));

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("name", "John");

            assertThatThrownBy(() -> creationService.execute("test", null, "users", null, data, false, null))
                    .isInstanceOf(RuntimeException.class);
        }

        @Test
        void execute_invalidTable_throwsRuntimeException() throws Exception {
            when(jdbcManager.getTable("test", null, "nonexistent"))
                    .thenThrow(new DbException(DbErrorCode.INVALID_REQUEST, "Invalid table"));

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("name", "John");

            assertThatThrownBy(() -> creationService.execute("test", null, "nonexistent", null, data, false, null))
                    .isInstanceOf(RuntimeException.class);
        }

        @Test
        void execute_withSequences_addsSequenceColumns() throws Exception {
            when(jdbcManager.getTable("test", null, "users")).thenReturn(usersTable);
            when(jdbcManager.getDialect("test")).thenReturn(dialect);
            when(jdbcManager.getNamedParameterJdbcTemplate("test")).thenReturn(namedParameterJdbcTemplate);
            when(jdbcManager.getTxnTemplate("test")).thenReturn(transactionTemplate);
            when(sqlCreatorTemplate.create(any())).thenReturn("INSERT INTO users (name, seq_col) VALUES (:name, nextval)");

            CreationResponse expectedResponse = new CreationResponse(1, Map.of("id", 1L));
            when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
                TransactionCallback<?> callback = invocation.getArgument(0);
                return callback.doInTransaction(null);
            });
            when(dbOperationService.create(eq(namedParameterJdbcTemplate), anyMap(), anyString(), eq(usersTable)))
                    .thenReturn(expectedResponse);

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("name", "John");

            CreationResponse result = creationService.execute("test", null, "users", null, data, false, List.of("seq_col:my_sequence"));

            assertThat(result).isNotNull();
        }

        @Test
        void execute_tsIdEnabled_nullResponse_throwsRuntimeException() throws Exception {
            when(jdbcManager.getTable("test", null, "users")).thenReturn(usersTable);
            when(jdbcManager.getDialect("test")).thenReturn(dialect);
            when(jdbcManager.getNamedParameterJdbcTemplate("test")).thenReturn(namedParameterJdbcTemplate);
            when(jdbcManager.getTxnTemplate("test")).thenReturn(transactionTemplate);
            when(sqlCreatorTemplate.create(any())).thenReturn("INSERT INTO users (id, name) VALUES (:id, :name)");
            when(tsidProcessor.processTsId(anyMap(), anyList())).thenReturn(Map.of("id", 123456L));

            when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
                TransactionCallback<?> callback = invocation.getArgument(0);
                return callback.doInTransaction(null);
            });
            when(dbOperationService.create(eq(namedParameterJdbcTemplate), anyMap(), anyString(), eq(usersTable)))
                    .thenReturn(null);

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("name", "John");

            assertThatThrownBy(() -> creationService.execute("test", null, "users", null, data, true, null))
                    .isInstanceOf(RuntimeException.class);
        }

        @Test
        void execute_tsIdEnabled_pkAlreadyInColumns_doesNotDuplicate() throws Exception {
            when(jdbcManager.getTable("test", null, "users")).thenReturn(usersTable);
            when(jdbcManager.getDialect("test")).thenReturn(dialect);
            when(jdbcManager.getNamedParameterJdbcTemplate("test")).thenReturn(namedParameterJdbcTemplate);
            when(jdbcManager.getTxnTemplate("test")).thenReturn(transactionTemplate);
            when(sqlCreatorTemplate.create(any())).thenReturn("INSERT INTO users (id, name) VALUES (:id, :name)");
            when(tsidProcessor.processTsId(anyMap(), anyList())).thenReturn(Map.of("id", 123456L));

            CreationResponse expectedResponse = new CreationResponse(1, null);
            when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
                TransactionCallback<?> callback = invocation.getArgument(0);
                return callback.doInTransaction(null);
            });
            when(dbOperationService.create(eq(namedParameterJdbcTemplate), anyMap(), anyString(), eq(usersTable)))
                    .thenReturn(expectedResponse);

            // Data already contains the PK column "id"
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("id", 999L);
            data.put("name", "John");

            CreationResponse result = creationService.execute("test", null, "users", null, data, true, null);

            assertThat(result).isNotNull();
            // PK "id" was already in data keys, so insertableColumns should not have duplicated it
            verify(tsidProcessor).processTsId(anyMap(), anyList());
        }

        @Test
        void execute_withInvalidSequenceFormat_ignoresInvalidEntry() throws Exception {
            when(jdbcManager.getTable("test", null, "users")).thenReturn(usersTable);
            when(jdbcManager.getDialect("test")).thenReturn(dialect);
            when(jdbcManager.getNamedParameterJdbcTemplate("test")).thenReturn(namedParameterJdbcTemplate);
            when(jdbcManager.getTxnTemplate("test")).thenReturn(transactionTemplate);
            when(sqlCreatorTemplate.create(any())).thenReturn("INSERT INTO users (name) VALUES (:name)");

            CreationResponse expectedResponse = new CreationResponse(1, Map.of("id", 1L));
            when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
                TransactionCallback<?> callback = invocation.getArgument(0);
                return callback.doInTransaction(null);
            });
            when(dbOperationService.create(eq(namedParameterJdbcTemplate), anyMap(), anyString(), eq(usersTable)))
                    .thenReturn(expectedResponse);

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("name", "John");

            // "invalid_no_colon" has no ":" so colSeq.length != 2 — should be ignored
            CreationResponse result = creationService.execute("test", null, "users", null, data, false, List.of("invalid_no_colon"));

            assertThat(result).isNotNull();
            assertThat(result.row()).isEqualTo(1);
        }

        @Test
        void execute_tsIdEnabled_withNonNullKeys_returnsDbKeys() throws Exception {
            when(jdbcManager.getTable("test", null, "users")).thenReturn(usersTable);
            when(jdbcManager.getDialect("test")).thenReturn(dialect);
            when(jdbcManager.getNamedParameterJdbcTemplate("test")).thenReturn(namedParameterJdbcTemplate);
            when(jdbcManager.getTxnTemplate("test")).thenReturn(transactionTemplate);
            when(sqlCreatorTemplate.create(any())).thenReturn("INSERT INTO users (id, name) VALUES (:id, :name)");
            when(tsidProcessor.processTsId(anyMap(), anyList())).thenReturn(Map.of("id", 123456L));

            // DB returns keys (non-null) — should use DB keys, not tsIdMap
            CreationResponse expectedResponse = new CreationResponse(1, Map.of("id", 789L));
            when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
                TransactionCallback<?> callback = invocation.getArgument(0);
                return callback.doInTransaction(null);
            });
            when(dbOperationService.create(eq(namedParameterJdbcTemplate), anyMap(), anyString(), eq(usersTable)))
                    .thenReturn(expectedResponse);

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("name", "John");

            CreationResponse result = creationService.execute("test", null, "users", null, data, true, null);

            assertThat(result).isNotNull();
            // DB-returned keys take priority over TSID keys
            assertThat(result.keys()).containsEntry("id", 789L);
        }

        @Test
        void execute_transactionException_rollsBackAndThrows() throws Exception {
            when(jdbcManager.getTable("test", null, "users")).thenReturn(usersTable);
            when(jdbcManager.getDialect("test")).thenReturn(dialect);
            when(jdbcManager.getNamedParameterJdbcTemplate("test")).thenReturn(namedParameterJdbcTemplate);
            when(jdbcManager.getTxnTemplate("test")).thenReturn(transactionTemplate);
            when(sqlCreatorTemplate.create(any())).thenReturn("INSERT INTO users (name) VALUES (:name)");

            TransactionStatus mockStatus = mock(TransactionStatus.class);
            when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
                TransactionCallback<?> callback = invocation.getArgument(0);
                return callback.doInTransaction(mockStatus);
            });
            when(dbOperationService.create(eq(namedParameterJdbcTemplate), anyMap(), anyString(), eq(usersTable)))
                    .thenThrow(new RuntimeException("DB error", new IllegalStateException("root cause")));

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("name", "John");

            assertThatThrownBy(() -> creationService.execute("test", null, "users", null, data, false, null))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("ERROR DURING INSERTION");

            verify(mockStatus).setRollbackOnly();
        }
    }

    @Nested
    @DisplayName("JdbcCreationService Upsert Tests")
    class JdbcCreationServiceUpsertTests {

        private JdbcCreationService creationService;

        @BeforeEach
        void setup() {
            creationService = new JdbcCreationService(tsidProcessor, sqlCreatorTemplate, jdbcManager, dbOperationService);
        }

        @Test
        void upsert_doUpdate_returnsCreationResponse() throws Exception {
            when(jdbcManager.getTable("test", "public", "users")).thenReturn(usersTable);
            when(jdbcManager.getDialect("test")).thenReturn(dialect);
            when(jdbcManager.getNamedParameterJdbcTemplate("test")).thenReturn(namedParameterJdbcTemplate);
            when(jdbcManager.getTxnTemplate("test")).thenReturn(transactionTemplate);
            when(dialect.renderOnConflictClause(anyList(), anyList()))
                    .thenReturn("ON CONFLICT (email) DO UPDATE SET name = EXCLUDED.name");
            when(sqlCreatorTemplate.upsert(any(), anyString()))
                    .thenReturn("INSERT INTO users (name, email) VALUES (:name, :email) ON CONFLICT (email) DO UPDATE SET name = EXCLUDED.name");

            CreationResponse expectedResponse = new CreationResponse(1, Map.of("id", 1L));
            when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
                TransactionCallback<?> callback = invocation.getArgument(0);
                return callback.doInTransaction(null);
            });
            when(dbOperationService.create(eq(namedParameterJdbcTemplate), anyMap(), anyString(), eq(usersTable)))
                    .thenReturn(expectedResponse);

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("name", "John");
            data.put("email", "john@test.com");

            UpsertConfig config = new UpsertConfig(List.of("email"), List.of("name"));
            CreationResponse result = creationService.upsert("test", "public", "users", null, data, config);

            assertThat(result).isNotNull();
            assertThat(result.row()).isEqualTo(1);
            verify(dialect).renderOnConflictClause(List.of("email"), List.of("name"));
        }

        @Test
        void upsert_doNothing_returnsCreationResponse() throws Exception {
            when(jdbcManager.getTable("test", "public", "users")).thenReturn(usersTable);
            when(jdbcManager.getDialect("test")).thenReturn(dialect);
            when(jdbcManager.getNamedParameterJdbcTemplate("test")).thenReturn(namedParameterJdbcTemplate);
            when(jdbcManager.getTxnTemplate("test")).thenReturn(transactionTemplate);
            when(dialect.renderOnConflictClause(anyList(), isNull()))
                    .thenReturn("ON CONFLICT (email) DO NOTHING");
            when(sqlCreatorTemplate.upsert(any(), anyString()))
                    .thenReturn("INSERT INTO users (name, email) VALUES (:name, :email) ON CONFLICT (email) DO NOTHING");

            CreationResponse expectedResponse = new CreationResponse(0, null);
            when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
                TransactionCallback<?> callback = invocation.getArgument(0);
                return callback.doInTransaction(null);
            });
            when(dbOperationService.create(eq(namedParameterJdbcTemplate), anyMap(), anyString(), eq(usersTable)))
                    .thenReturn(expectedResponse);

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("name", "John");
            data.put("email", "john@test.com");

            UpsertConfig config = new UpsertConfig(List.of("email"), null);
            CreationResponse result = creationService.upsert("test", "public", "users", null, data, config);

            assertThat(result).isNotNull();
            assertThat(result.row()).isEqualTo(0);
        }

        @Test
        void upsert_withSpecificColumns_usesProvidedColumns() throws Exception {
            when(jdbcManager.getTable("test", "public", "users")).thenReturn(usersTable);
            when(jdbcManager.getDialect("test")).thenReturn(dialect);
            when(jdbcManager.getNamedParameterJdbcTemplate("test")).thenReturn(namedParameterJdbcTemplate);
            when(jdbcManager.getTxnTemplate("test")).thenReturn(transactionTemplate);
            when(dialect.renderOnConflictClause(anyList(), anyList()))
                    .thenReturn("ON CONFLICT (email) DO UPDATE SET name = EXCLUDED.name");
            when(sqlCreatorTemplate.upsert(any(), anyString())).thenReturn("UPSERT SQL");

            CreationResponse expectedResponse = new CreationResponse(1, Map.of("id", 1L));
            when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
                TransactionCallback<?> callback = invocation.getArgument(0);
                return callback.doInTransaction(null);
            });
            when(dbOperationService.create(eq(namedParameterJdbcTemplate), anyMap(), anyString(), eq(usersTable)))
                    .thenReturn(expectedResponse);

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("name", "John");
            data.put("email", "john@test.com");

            UpsertConfig config = new UpsertConfig(List.of("email"), List.of("name"));
            CreationResponse result = creationService.upsert("test", "public", "users", List.of("name", "email"), data, config);

            assertThat(result).isNotNull();
            assertThat(result.row()).isEqualTo(1);
        }

        @Test
        void upsert_nullConfig_throwsNullPointerException() {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("name", "John");

            assertThatThrownBy(() -> creationService.upsert("test", "public", "users", null, data, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("UpsertConfig must not be null");
        }

        @Test
        void upsert_invalidTable_throwsRuntimeException() throws Exception {
            when(jdbcManager.getTable("test", "public", "nonexistent"))
                    .thenThrow(new DbException(DbErrorCode.INVALID_REQUEST, "Invalid table"));

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("name", "John");

            UpsertConfig config = new UpsertConfig(List.of("email"), List.of("name"));

            assertThatThrownBy(() -> creationService.upsert("test", "public", "nonexistent", null, data, config))
                    .isInstanceOf(RuntimeException.class);
        }

        @Test
        void upsert_transactionException_rollsBackAndThrows() throws Exception {
            when(jdbcManager.getTable("test", "public", "users")).thenReturn(usersTable);
            when(jdbcManager.getDialect("test")).thenReturn(dialect);
            when(jdbcManager.getNamedParameterJdbcTemplate("test")).thenReturn(namedParameterJdbcTemplate);
            when(jdbcManager.getTxnTemplate("test")).thenReturn(transactionTemplate);
            when(dialect.renderOnConflictClause(anyList(), anyList()))
                    .thenReturn("ON CONFLICT (email) DO UPDATE SET name = EXCLUDED.name");
            when(sqlCreatorTemplate.upsert(any(), anyString())).thenReturn("UPSERT SQL");

            TransactionStatus mockStatus = mock(TransactionStatus.class);
            when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
                TransactionCallback<?> callback = invocation.getArgument(0);
                return callback.doInTransaction(mockStatus);
            });
            when(dbOperationService.create(eq(namedParameterJdbcTemplate), anyMap(), anyString(), eq(usersTable)))
                    .thenThrow(new RuntimeException("DB error", new IllegalStateException("root cause")));

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("name", "John");
            data.put("email", "john@test.com");

            UpsertConfig config = new UpsertConfig(List.of("email"), List.of("name"));

            assertThatThrownBy(() -> creationService.upsert("test", "public", "users", null, data, config))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("ERROR DURING UPSERT");

            verify(mockStatus).setRollbackOnly();
        }

        @Test
        void upsert_dataAccessException_throwsRuntimeException() throws Exception {
            when(jdbcManager.getTable("test", "public", "users")).thenReturn(usersTable);
            when(jdbcManager.getDialect("test")).thenReturn(dialect);
            when(jdbcManager.getTxnTemplate("test")).thenReturn(transactionTemplate);
            when(dialect.renderOnConflictClause(anyList(), anyList()))
                    .thenReturn("ON CONFLICT (email) DO UPDATE SET name = EXCLUDED.name");
            when(sqlCreatorTemplate.upsert(any(), anyString())).thenReturn("UPSERT SQL");

            when(transactionTemplate.execute(any())).thenThrow(new DataAccessResourceFailureException("Connection lost"));

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("name", "John");
            data.put("email", "john@test.com");

            UpsertConfig config = new UpsertConfig(List.of("email"), List.of("name"));

            assertThatThrownBy(() -> creationService.upsert("test", "public", "users", null, data, config))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    @DisplayName("JdbcReadService Tests")
    class JdbcReadServiceTests {

        private JdbcReadService readService;

        @BeforeEach
        void setup() {
            readService = new JdbcReadService(jdbcManager, dbOperationService, List.of(), sqlCreatorTemplate);
        }

        @Test
        void findAll_simple_returnsList() throws DbException {
            when(jdbcManager.getNamedParameterJdbcTemplate("test")).thenReturn(namedParameterJdbcTemplate);
            when(jdbcManager.getDialect("test")).thenReturn(dialect);
            when(sqlCreatorTemplate.query(any(ReadContext.class))).thenReturn("SELECT * FROM users");
            when(dbOperationService.read(eq(namedParameterJdbcTemplate), any(), anyString(), eq(dialect)))
                    .thenReturn(List.of(Map.of("id", 1L, "name", "John")));

            ReadContext context = new ReadContext();
            context.setDbId("test");
            context.setParamMap(new HashMap<>());

            List<Map<String, Object>> result = readService.findAll(context);

            assertThat(result).hasSize(1);
            assertThat(result.get(0)).containsEntry("name", "John");
        }

        @Test
        void findAll_dbException_rethrows() throws DbException {
            when(sqlCreatorTemplate.query(any(ReadContext.class))).thenThrow(new DbException(DbErrorCode.SERVER_ERROR));

            ReadContext context = new ReadContext();
            context.setDbId("test");

            assertThatThrownBy(() -> readService.findAll(context))
                    .isInstanceOf(DbException.class);
        }

        @Test
        void findAll_genericException_wrapsInDbException() throws DbException {
            when(sqlCreatorTemplate.query(any(ReadContext.class))).thenThrow(new RuntimeException("unexpected"));

            ReadContext context = new ReadContext();
            context.setDbId("test");

            assertThatThrownBy(() -> readService.findAll(context))
                    .isInstanceOf(DbException.class);
        }

        @Test
        void findOne_existing_returnsMap() throws DbException {
            when(jdbcManager.getNamedParameterJdbcTemplate("test")).thenReturn(namedParameterJdbcTemplate);
            when(sqlCreatorTemplate.findOne(any(ReadContext.class))).thenReturn("SELECT * FROM users WHERE id = :id LIMIT 1");
            when(dbOperationService.findOne(eq(namedParameterJdbcTemplate), anyString(), any()))
                    .thenReturn(Map.of("id", 1L, "name", "John"));

            ReadContext context = new ReadContext();
            context.setDbId("test");
            context.setParamMap(new HashMap<>());

            Map<String, Object> result = readService.findOne(context);

            assertThat(result).containsEntry("name", "John");
        }

        @Test
        void findOne_nonExisting_returnsNull() throws DbException {
            when(jdbcManager.getNamedParameterJdbcTemplate("test")).thenReturn(namedParameterJdbcTemplate);
            when(sqlCreatorTemplate.findOne(any(ReadContext.class))).thenReturn("SELECT * FROM users WHERE id = :id LIMIT 1");
            when(dbOperationService.findOne(eq(namedParameterJdbcTemplate), anyString(), any()))
                    .thenThrow(new EmptyResultDataAccessException(1));

            ReadContext context = new ReadContext();
            context.setDbId("test");
            context.setParamMap(new HashMap<>());

            Map<String, Object> result = readService.findOne(context);

            assertThat(result).isNull();
        }

        @Test
        void findOne_dataAccessException_throwsRuntimeException() throws DbException {
            when(jdbcManager.getNamedParameterJdbcTemplate("test")).thenReturn(namedParameterJdbcTemplate);
            when(sqlCreatorTemplate.findOne(any(ReadContext.class))).thenReturn("SELECT * FROM users WHERE id = :id LIMIT 1");
            when(dbOperationService.findOne(eq(namedParameterJdbcTemplate), anyString(), any()))
                    .thenThrow(new DataAccessResourceFailureException("Connection lost"));

            ReadContext context = new ReadContext();
            context.setDbId("test");
            context.setParamMap(new HashMap<>());

            assertThatThrownBy(() -> readService.findOne(context))
                    .isInstanceOf(RuntimeException.class);
        }

        @Test
        void findOne_genericException_throwsDbException() throws DbException {
            when(jdbcManager.getNamedParameterJdbcTemplate("test")).thenReturn(namedParameterJdbcTemplate);
            when(sqlCreatorTemplate.findOne(any(ReadContext.class))).thenReturn("SELECT * FROM users WHERE id = :id LIMIT 1");
            when(dbOperationService.findOne(eq(namedParameterJdbcTemplate), anyString(), any()))
                    .thenThrow(new IllegalStateException("unexpected"));

            ReadContext context = new ReadContext();
            context.setDbId("test");
            context.setParamMap(new HashMap<>());

            assertThatThrownBy(() -> readService.findOne(context))
                    .isInstanceOf(DbException.class);
        }

        @Test
        void count_dataAccessException_throwsDbException() throws DbException {
            when(jdbcManager.getNamedParameterJdbcTemplate("test")).thenReturn(namedParameterJdbcTemplate);
            when(sqlCreatorTemplate.count(any(ReadContext.class))).thenReturn("SELECT COUNT(*) FROM users");
            when(dbOperationService.count(eq(namedParameterJdbcTemplate), any(), anyString()))
                    .thenThrow(new DataAccessResourceFailureException("Connection lost"));

            ReadContext context = new ReadContext();
            context.setDbId("test");
            context.setParamMap(new HashMap<>());

            assertThatThrownBy(() -> readService.count(context))
                    .isInstanceOf(DbException.class);
        }

        @Test
        void count_returnsCount() throws DbException {
            when(jdbcManager.getNamedParameterJdbcTemplate("test")).thenReturn(namedParameterJdbcTemplate);
            when(sqlCreatorTemplate.count(any(ReadContext.class))).thenReturn("SELECT COUNT(*) FROM users");
            when(dbOperationService.count(eq(namedParameterJdbcTemplate), any(), anyString()))
                    .thenReturn(new CountResponse(5));

            ReadContext context = new ReadContext();
            context.setDbId("test");
            context.setParamMap(new HashMap<>());

            long count = readService.count(context);

            assertThat(count).isEqualTo(5);
        }

        @Test
        void findOne_withProcessors_callsProcessors() throws DbException {
            ReadProcessor processor = mock(ReadProcessor.class);
            JdbcReadService serviceWithProcessors = new JdbcReadService(jdbcManager, dbOperationService, List.of(processor), sqlCreatorTemplate);

            when(jdbcManager.getNamedParameterJdbcTemplate("test")).thenReturn(namedParameterJdbcTemplate);
            when(sqlCreatorTemplate.findOne(any(ReadContext.class))).thenReturn("SELECT * FROM users LIMIT 1");
            when(dbOperationService.findOne(eq(namedParameterJdbcTemplate), anyString(), any()))
                    .thenReturn(Map.of("id", 1L));

            ReadContext context = new ReadContext();
            context.setDbId("test");
            context.setParamMap(new HashMap<>());

            Map<String, Object> result = serviceWithProcessors.findOne(context);

            assertThat(result).isNotNull();
            verify(processor).process(context);
        }

        @Test
        void count_withProcessors_callsProcessors() throws DbException {
            ReadProcessor processor = mock(ReadProcessor.class);
            JdbcReadService serviceWithProcessors = new JdbcReadService(jdbcManager, dbOperationService, List.of(processor), sqlCreatorTemplate);

            when(jdbcManager.getNamedParameterJdbcTemplate("test")).thenReturn(namedParameterJdbcTemplate);
            when(sqlCreatorTemplate.count(any(ReadContext.class))).thenReturn("SELECT COUNT(*) FROM users");
            when(dbOperationService.count(eq(namedParameterJdbcTemplate), any(), anyString()))
                    .thenReturn(new CountResponse(3));

            ReadContext context = new ReadContext();
            context.setDbId("test");
            context.setParamMap(new HashMap<>());

            long count = serviceWithProcessors.count(context);

            assertThat(count).isEqualTo(3);
            verify(processor).process(context);
        }

        @Test
        void findAll_withProcessors_callsProcessors() throws DbException {
            ReadProcessor processor = mock(ReadProcessor.class);
            JdbcReadService serviceWithProcessors = new JdbcReadService(jdbcManager, dbOperationService, List.of(processor), sqlCreatorTemplate);

            when(jdbcManager.getNamedParameterJdbcTemplate("test")).thenReturn(namedParameterJdbcTemplate);
            when(jdbcManager.getDialect("test")).thenReturn(dialect);
            when(sqlCreatorTemplate.query(any(ReadContext.class))).thenReturn("SELECT * FROM users");
            when(dbOperationService.read(eq(namedParameterJdbcTemplate), any(), anyString(), eq(dialect)))
                    .thenReturn(List.of());

            ReadContext context = new ReadContext();
            context.setDbId("test");
            context.setParamMap(new HashMap<>());

            serviceWithProcessors.findAll(context);

            verify(processor).process(context);
        }

        @Test
        void findPage_returnsPageWithMetadata() throws DbException {
            when(jdbcManager.getNamedParameterJdbcTemplate("test")).thenReturn(namedParameterJdbcTemplate);
            when(jdbcManager.getDialect("test")).thenReturn(dialect);
            when(jdbcManager.getTxnTemplate("test")).thenReturn(transactionTemplate);
            when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
                TransactionCallback<?> callback = invocation.getArgument(0);
                return callback.doInTransaction(null);
            });
            when(sqlCreatorTemplate.query(any(ReadContext.class))).thenReturn("SELECT * FROM users LIMIT 10 OFFSET 0");
            when(sqlCreatorTemplate.count(any(ReadContext.class))).thenReturn("SELECT COUNT(*) FROM users");
            when(dbOperationService.read(eq(namedParameterJdbcTemplate), any(), anyString(), eq(dialect)))
                    .thenReturn(List.of(Map.of("id", 1L), Map.of("id", 2L)));
            when(dbOperationService.count(eq(namedParameterJdbcTemplate), any(), anyString()))
                    .thenReturn(new CountResponse(25));

            ReadContext context = new ReadContext();
            context.setDbId("test");
            context.setLimit(10);
            context.setOffset(0);
            context.setParamMap(new HashMap<>());

            Page result = readService.findPage(context);

            assertThat(result.data()).hasSize(2);
            assertThat(result.total()).isEqualTo(25);
            assertThat(result.limit()).isEqualTo(10);
            assertThat(result.offset()).isZero();
            assertThat(result.hasNext()).isTrue();
        }

        @Test
        void findPage_lastPage_hasNextFalse() throws DbException {
            when(jdbcManager.getNamedParameterJdbcTemplate("test")).thenReturn(namedParameterJdbcTemplate);
            when(jdbcManager.getDialect("test")).thenReturn(dialect);
            when(jdbcManager.getTxnTemplate("test")).thenReturn(transactionTemplate);
            when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
                TransactionCallback<?> callback = invocation.getArgument(0);
                return callback.doInTransaction(null);
            });
            when(sqlCreatorTemplate.query(any(ReadContext.class))).thenReturn("SELECT * FROM users LIMIT 10 OFFSET 20");
            when(sqlCreatorTemplate.count(any(ReadContext.class))).thenReturn("SELECT COUNT(*) FROM users");
            when(dbOperationService.read(eq(namedParameterJdbcTemplate), any(), anyString(), eq(dialect)))
                    .thenReturn(List.of(Map.of("id", 21L), Map.of("id", 22L)));
            when(dbOperationService.count(eq(namedParameterJdbcTemplate), any(), anyString()))
                    .thenReturn(new CountResponse(22));

            ReadContext context = new ReadContext();
            context.setDbId("test");
            context.setLimit(10);
            context.setOffset(20);
            context.setParamMap(new HashMap<>());

            Page result = readService.findPage(context);

            assertThat(result.data()).hasSize(2);
            assertThat(result.total()).isEqualTo(22);
            assertThat(result.hasNext()).isFalse();
        }

        @Test
        void findPage_emptyResult_returnsEmptyPage() throws DbException {
            when(jdbcManager.getNamedParameterJdbcTemplate("test")).thenReturn(namedParameterJdbcTemplate);
            when(jdbcManager.getDialect("test")).thenReturn(dialect);
            when(jdbcManager.getTxnTemplate("test")).thenReturn(transactionTemplate);
            when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
                TransactionCallback<?> callback = invocation.getArgument(0);
                return callback.doInTransaction(null);
            });
            when(sqlCreatorTemplate.query(any(ReadContext.class))).thenReturn("SELECT * FROM users LIMIT 10 OFFSET 0");
            when(sqlCreatorTemplate.count(any(ReadContext.class))).thenReturn("SELECT COUNT(*) FROM users");
            when(dbOperationService.read(eq(namedParameterJdbcTemplate), any(), anyString(), eq(dialect)))
                    .thenReturn(List.of());
            when(dbOperationService.count(eq(namedParameterJdbcTemplate), any(), anyString()))
                    .thenReturn(new CountResponse(0));

            ReadContext context = new ReadContext();
            context.setDbId("test");
            context.setLimit(10);
            context.setOffset(0);
            context.setParamMap(new HashMap<>());

            Page result = readService.findPage(context);

            assertThat(result.data()).isEmpty();
            assertThat(result.total()).isZero();
            assertThat(result.hasNext()).isFalse();
        }

        @Test
        void findPage_dbException_propagates() throws DbException {
            when(sqlCreatorTemplate.query(any(ReadContext.class))).thenThrow(new DbException(DbErrorCode.SERVER_ERROR));

            ReadContext context = new ReadContext();
            context.setDbId("test");
            context.setLimit(10);
            context.setOffset(0);

            assertThatThrownBy(() -> readService.findPage(context))
                    .isInstanceOf(DbException.class);
        }

        @Test
        void findPage_zeroLimit_usesDefaultFetchLimit() throws DbException {
            when(jdbcManager.getNamedParameterJdbcTemplate("test")).thenReturn(namedParameterJdbcTemplate);
            when(jdbcManager.getDialect("test")).thenReturn(dialect);
            when(jdbcManager.getTxnTemplate("test")).thenReturn(transactionTemplate);
            when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
                TransactionCallback<?> callback = invocation.getArgument(0);
                return callback.doInTransaction(null);
            });
            when(sqlCreatorTemplate.query(any(ReadContext.class))).thenReturn("SELECT * FROM users");
            when(sqlCreatorTemplate.count(any(ReadContext.class))).thenReturn("SELECT COUNT(*) FROM users");
            when(dbOperationService.read(eq(namedParameterJdbcTemplate), any(), anyString(), eq(dialect)))
                    .thenReturn(List.of(Map.of("id", 1L)));
            when(dbOperationService.count(eq(namedParameterJdbcTemplate), any(), anyString()))
                    .thenReturn(new CountResponse(5));

            ReadContext context = new ReadContext();
            context.setDbId("test");
            context.setLimit(0);
            context.setOffset(0);
            context.setDefaultFetchLimit(50);
            context.setParamMap(new HashMap<>());

            Page result = readService.findPage(context);

            assertThat(result.limit()).isEqualTo(50);
            assertThat(result.hasNext()).isTrue();
        }

        @Test
        void findPage_negativeLimit_usesDefaultFetchLimit() throws DbException {
            when(jdbcManager.getNamedParameterJdbcTemplate("test")).thenReturn(namedParameterJdbcTemplate);
            when(jdbcManager.getDialect("test")).thenReturn(dialect);
            when(jdbcManager.getTxnTemplate("test")).thenReturn(transactionTemplate);
            when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
                TransactionCallback<?> callback = invocation.getArgument(0);
                return callback.doInTransaction(null);
            });
            when(sqlCreatorTemplate.query(any(ReadContext.class))).thenReturn("SELECT * FROM users");
            when(sqlCreatorTemplate.count(any(ReadContext.class))).thenReturn("SELECT COUNT(*) FROM users");
            when(dbOperationService.read(eq(namedParameterJdbcTemplate), any(), anyString(), eq(dialect)))
                    .thenReturn(List.of(Map.of("id", 1L)));
            when(dbOperationService.count(eq(namedParameterJdbcTemplate), any(), anyString()))
                    .thenReturn(new CountResponse(100));

            ReadContext context = new ReadContext();
            context.setDbId("test");
            context.setLimit(-1);
            context.setOffset(0);
            context.setDefaultFetchLimit(25);
            context.setParamMap(new HashMap<>());

            Page result = readService.findPage(context);

            assertThat(result.limit()).isEqualTo(25);
        }

        @Test
        void findPage_processorsCalledOnce() throws DbException {
            ReadProcessor processor = mock(ReadProcessor.class);
            JdbcReadService serviceWithProcessors = new JdbcReadService(jdbcManager, dbOperationService, List.of(processor), sqlCreatorTemplate);

            when(jdbcManager.getNamedParameterJdbcTemplate("test")).thenReturn(namedParameterJdbcTemplate);
            when(jdbcManager.getDialect("test")).thenReturn(dialect);
            when(jdbcManager.getTxnTemplate("test")).thenReturn(transactionTemplate);
            when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
                TransactionCallback<?> callback = invocation.getArgument(0);
                return callback.doInTransaction(null);
            });
            when(sqlCreatorTemplate.query(any(ReadContext.class))).thenReturn("SELECT * FROM users");
            when(sqlCreatorTemplate.count(any(ReadContext.class))).thenReturn("SELECT COUNT(*) FROM users");
            when(dbOperationService.read(eq(namedParameterJdbcTemplate), any(), anyString(), eq(dialect)))
                    .thenReturn(List.of());
            when(dbOperationService.count(eq(namedParameterJdbcTemplate), any(), anyString()))
                    .thenReturn(new CountResponse(0));

            ReadContext context = new ReadContext();
            context.setDbId("test");
            context.setLimit(10);
            context.setOffset(0);
            context.setParamMap(new HashMap<>());

            serviceWithProcessors.findPage(context);

            // Processors should be called exactly once, not twice
            verify(processor, times(1)).process(context);
        }

        @Test
        void findPage_dataIsDefensivelyCopied() throws DbException {
            when(jdbcManager.getNamedParameterJdbcTemplate("test")).thenReturn(namedParameterJdbcTemplate);
            when(jdbcManager.getDialect("test")).thenReturn(dialect);
            when(jdbcManager.getTxnTemplate("test")).thenReturn(transactionTemplate);
            when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
                TransactionCallback<?> callback = invocation.getArgument(0);
                return callback.doInTransaction(null);
            });
            when(sqlCreatorTemplate.query(any(ReadContext.class))).thenReturn("SELECT * FROM users");
            when(sqlCreatorTemplate.count(any(ReadContext.class))).thenReturn("SELECT COUNT(*) FROM users");

            List<Map<String, Object>> mutableList = new java.util.ArrayList<>(List.of(Map.of("id", 1L)));
            when(dbOperationService.read(eq(namedParameterJdbcTemplate), any(), anyString(), eq(dialect)))
                    .thenReturn(mutableList);
            when(dbOperationService.count(eq(namedParameterJdbcTemplate), any(), anyString()))
                    .thenReturn(new CountResponse(1));

            ReadContext context = new ReadContext();
            context.setDbId("test");
            context.setLimit(10);
            context.setOffset(0);
            context.setParamMap(new HashMap<>());

            Page result = readService.findPage(context);

            assertThatThrownBy(() -> result.data().add(Map.of("id", 2L)))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void findPage_transactionReturnsNull_throwsDbException() throws DbException {
            when(jdbcManager.getTxnTemplate("test")).thenReturn(transactionTemplate);
            when(sqlCreatorTemplate.query(any(ReadContext.class))).thenReturn("SELECT * FROM users");
            when(sqlCreatorTemplate.count(any(ReadContext.class))).thenReturn("SELECT COUNT(*) FROM users");
            when(transactionTemplate.execute(any())).thenReturn(null);

            ReadContext context = new ReadContext();
            context.setDbId("test");
            context.setLimit(10);
            context.setOffset(0);
            context.setParamMap(new HashMap<>());

            assertThatThrownBy(() -> readService.findPage(context))
                    .isInstanceOf(DbException.class)
                    .hasMessageContaining("Transaction returned null");
        }

        @Test
        void findPage_dbExceptionInsideTransaction_wrapsInDbRuntimeException() throws DbException {
            when(jdbcManager.getTxnTemplate("test")).thenReturn(transactionTemplate);
            when(sqlCreatorTemplate.query(any(ReadContext.class))).thenReturn("SELECT * FROM users");
            when(sqlCreatorTemplate.count(any(ReadContext.class))).thenReturn("SELECT COUNT(*) FROM users");
            when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
                TransactionCallback<?> callback = invocation.getArgument(0);
                return callback.doInTransaction(null);
            });
            when(jdbcManager.getNamedParameterJdbcTemplate("test")).thenReturn(namedParameterJdbcTemplate);
            when(jdbcManager.getDialect("test")).thenThrow(new DbException(DbErrorCode.NOT_FOUND, "DB not found."));

            ReadContext context = new ReadContext();
            context.setDbId("test");
            context.setLimit(10);
            context.setOffset(0);
            context.setParamMap(new HashMap<>());

            assertThatThrownBy(() -> readService.findPage(context))
                    .isInstanceOf(dev.suprim.query.exception.DbRuntimeException.class);
        }
    }

    @Nested
    @DisplayName("JdbcUpdateService Tests")
    class JdbcUpdateServiceTests {

        private JdbcUpdateService updateService;

        @BeforeEach
        void setup() {
            updateService = new JdbcUpdateService(jdbcManager, sqlCreatorTemplate, dbOperationService);
        }

        @Test
        void patch_simpleUpdate_returnsRowCount() throws DbException {
            when(jdbcManager.getTable("test", null, "users")).thenReturn(usersTable);
            when(jdbcManager.getDialect("test")).thenReturn(dialect);
            when(jdbcManager.getNamedParameterJdbcTemplate("test")).thenReturn(namedParameterJdbcTemplate);
            when(jdbcManager.getTxnTemplate("test")).thenReturn(transactionTemplate);
            when(dialect.supportAlias()).thenReturn(true);
            when(sqlCreatorTemplate.updateQuery(any())).thenReturn("UPDATE users SET name = :name WHERE id = :id");

            when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
                TransactionCallback<?> callback = invocation.getArgument(0);
                return callback.doInTransaction(null);
            });
            when(dbOperationService.update(eq(namedParameterJdbcTemplate), anyMap(), anyString())).thenReturn(1);

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("name", "Updated");

            int result = updateService.patch("test", null, "users", data, "id==1");

            assertThat(result).isEqualTo(1);
        }

        @Test
        void patch_withoutFilter_throwsDbException() {
            Map<String, Object> data = Map.of("name", "Updated");

            assertThatThrownBy(() -> updateService.patch("test", null, "users", data, null))
                    .isInstanceOf(DbException.class)
                    .hasMessageContaining("UPDATE without filter is not allowed");
        }

        @Test
        void patch_blankFilter_throwsDbException() {
            Map<String, Object> data = Map.of("name", "Updated");

            assertThatThrownBy(() -> updateService.patch("test", null, "users", data, "   "))
                    .isInstanceOf(DbException.class)
                    .hasMessageContaining("UPDATE without filter is not allowed");
        }

        @Test
        void patch_invalidTable_throwsDbException() throws DbException {
            when(jdbcManager.getTable("test", null, "nonexistent"))
                    .thenThrow(new DbException(DbErrorCode.INVALID_REQUEST, "Invalid table"));

            Map<String, Object> data = Map.of("name", "Updated");

            assertThatThrownBy(() -> updateService.patch("test", null, "nonexistent", data, "id==1"))
                    .isInstanceOf(DbException.class);
        }

        @Test
        void patch_noMatch_returnsZero() throws DbException {
            when(jdbcManager.getTable("test", null, "users")).thenReturn(usersTable);
            when(jdbcManager.getDialect("test")).thenReturn(dialect);
            when(jdbcManager.getNamedParameterJdbcTemplate("test")).thenReturn(namedParameterJdbcTemplate);
            when(jdbcManager.getTxnTemplate("test")).thenReturn(transactionTemplate);
            when(dialect.supportAlias()).thenReturn(true);
            when(sqlCreatorTemplate.updateQuery(any())).thenReturn("UPDATE users SET name = :name WHERE id = :id");

            when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
                TransactionCallback<?> callback = invocation.getArgument(0);
                return callback.doInTransaction(null);
            });
            when(dbOperationService.update(eq(namedParameterJdbcTemplate), anyMap(), anyString())).thenReturn(0);

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("name", "Updated");

            int result = updateService.patch("test", null, "users", data, "id==999");

            assertThat(result).isEqualTo(0);
        }

        @Test
        void patch_transactionReturnsNull_returnsZero() throws DbException {
            when(jdbcManager.getTable("test", null, "users")).thenReturn(usersTable);
            when(jdbcManager.getDialect("test")).thenReturn(dialect);
            when(jdbcManager.getTxnTemplate("test")).thenReturn(transactionTemplate);
            when(dialect.supportAlias()).thenReturn(true);
            when(sqlCreatorTemplate.updateQuery(any())).thenReturn("UPDATE users SET name = :name WHERE id = :id");

            when(transactionTemplate.execute(any())).thenReturn(null);

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("name", "Updated");

            int result = updateService.patch("test", null, "users", data, "id==1");

            assertThat(result).isEqualTo(0);
        }

        @Test
        void patch_transactionException_rollsBackAndThrows() throws DbException {
            when(jdbcManager.getTable("test", null, "users")).thenReturn(usersTable);
            when(jdbcManager.getDialect("test")).thenReturn(dialect);
            when(jdbcManager.getNamedParameterJdbcTemplate("test")).thenReturn(namedParameterJdbcTemplate);
            when(jdbcManager.getTxnTemplate("test")).thenReturn(transactionTemplate);
            when(dialect.supportAlias()).thenReturn(true);
            when(sqlCreatorTemplate.updateQuery(any())).thenReturn("UPDATE users SET name = :name WHERE id = :id");

            TransactionStatus mockStatus = mock(TransactionStatus.class);
            when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
                TransactionCallback<?> callback = invocation.getArgument(0);
                return callback.doInTransaction(mockStatus);
            });
            when(dbOperationService.update(eq(namedParameterJdbcTemplate), anyMap(), anyString()))
                    .thenThrow(new RuntimeException("DB error"));

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("name", "Updated");

            assertThatThrownBy(() -> updateService.patch("test", null, "users", data, "id==1"))
                    .isInstanceOf(RuntimeException.class);

            verify(mockStatus).setRollbackOnly();
        }

        @Test
        void patchBulk_multipleUpdates_returnsTotal() throws DbException {
            when(jdbcManager.getTable("test", null, "users")).thenReturn(usersTable);
            when(jdbcManager.getDialect("test")).thenReturn(dialect);
            when(jdbcManager.getNamedParameterJdbcTemplate("test")).thenReturn(namedParameterJdbcTemplate);
            when(jdbcManager.getTxnTemplate("test")).thenReturn(transactionTemplate);
            when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
                TransactionCallback<?> callback = invocation.getArgument(0);
                return callback.doInTransaction(null);
            });
            when(sqlCreatorTemplate.updateQuery(any())).thenReturn("UPDATE users SET name = :name WHERE id = :id");
            when(dbOperationService.update(eq(namedParameterJdbcTemplate), anyMap(), anyString()))
                    .thenReturn(2).thenReturn(3);

            List<BulkUpdate> updates = List.of(
                    new BulkUpdate(Map.of("name", "A"), "id==1"),
                    new BulkUpdate(Map.of("name", "B"), "id==2")
            );

            int result = updateService.patchBulk("test", null, "users", updates);

            assertThat(result).isEqualTo(5);
        }

        @Test
        void patchBulk_nullUpdates_throwsDbException() {
            assertThatThrownBy(() -> updateService.patchBulk("test", null, "users", null))
                    .isInstanceOf(DbException.class)
                    .hasMessageContaining("at least one operation");
        }

        @Test
        void patchBulk_emptyUpdates_throwsDbException() {
            assertThatThrownBy(() -> updateService.patchBulk("test", null, "users", List.of()))
                    .isInstanceOf(DbException.class)
                    .hasMessageContaining("at least one operation");
        }

        @Test
        void patchBulk_nullFilter_throwsDbException() throws DbException {
            when(jdbcManager.getTable("test", null, "users")).thenReturn(usersTable);
            when(jdbcManager.getTxnTemplate("test")).thenReturn(transactionTemplate);
            TransactionStatus mockStatus = mock(TransactionStatus.class);
            when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
                TransactionCallback<?> callback = invocation.getArgument(0);
                return callback.doInTransaction(mockStatus);
            });

            List<BulkUpdate> updates = List.of(
                    new BulkUpdate(Map.of("name", "A"), null)
            );

            assertThatThrownBy(() -> updateService.patchBulk("test", null, "users", updates))
                    .isInstanceOf(dev.suprim.query.exception.DbRuntimeException.class);
            verify(mockStatus).setRollbackOnly();
        }

        @Test
        void patchBulk_blankFilter_throwsDbException() throws DbException {
            when(jdbcManager.getTable("test", null, "users")).thenReturn(usersTable);
            when(jdbcManager.getTxnTemplate("test")).thenReturn(transactionTemplate);
            TransactionStatus mockStatus = mock(TransactionStatus.class);
            when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
                TransactionCallback<?> callback = invocation.getArgument(0);
                return callback.doInTransaction(mockStatus);
            });

            List<BulkUpdate> updates = List.of(
                    new BulkUpdate(Map.of("name", "A"), "   ")
            );

            assertThatThrownBy(() -> updateService.patchBulk("test", null, "users", updates))
                    .isInstanceOf(dev.suprim.query.exception.DbRuntimeException.class);
            verify(mockStatus).setRollbackOnly();
        }

        @Test
        void patchBulk_nullData_throwsDbException() throws DbException {
            when(jdbcManager.getTable("test", null, "users")).thenReturn(usersTable);
            when(jdbcManager.getTxnTemplate("test")).thenReturn(transactionTemplate);
            TransactionStatus mockStatus = mock(TransactionStatus.class);
            when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
                TransactionCallback<?> callback = invocation.getArgument(0);
                return callback.doInTransaction(mockStatus);
            });

            List<BulkUpdate> updates = List.of(
                    new BulkUpdate(null, "id==1")
            );

            assertThatThrownBy(() -> updateService.patchBulk("test", null, "users", updates))
                    .isInstanceOf(dev.suprim.query.exception.DbRuntimeException.class);
            verify(mockStatus).setRollbackOnly();
        }

        @Test
        void patchBulk_emptyData_throwsDbException() throws DbException {
            when(jdbcManager.getTable("test", null, "users")).thenReturn(usersTable);
            when(jdbcManager.getTxnTemplate("test")).thenReturn(transactionTemplate);
            TransactionStatus mockStatus = mock(TransactionStatus.class);
            when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
                TransactionCallback<?> callback = invocation.getArgument(0);
                return callback.doInTransaction(mockStatus);
            });

            List<BulkUpdate> updates = List.of(
                    new BulkUpdate(Map.of(), "id==1")
            );

            assertThatThrownBy(() -> updateService.patchBulk("test", null, "users", updates))
                    .isInstanceOf(dev.suprim.query.exception.DbRuntimeException.class);
            verify(mockStatus).setRollbackOnly();
        }

        @Test
        void patchBulk_transactionReturnsNull_returnsZero() throws DbException {
            when(jdbcManager.getTable("test", null, "users")).thenReturn(usersTable);
            when(jdbcManager.getTxnTemplate("test")).thenReturn(transactionTemplate);
            when(transactionTemplate.execute(any())).thenReturn(null);

            List<BulkUpdate> updates = List.of(
                    new BulkUpdate(Map.of("name", "A"), "id==1")
            );

            int result = updateService.patchBulk("test", null, "users", updates);

            assertThat(result).isEqualTo(0);
        }

        @Test
        void patchBulk_runtimeException_rollsBackAndThrows() throws DbException {
            when(jdbcManager.getTable("test", null, "users")).thenReturn(usersTable);
            when(jdbcManager.getDialect("test")).thenReturn(dialect);
            when(jdbcManager.getNamedParameterJdbcTemplate("test")).thenReturn(namedParameterJdbcTemplate);
            when(jdbcManager.getTxnTemplate("test")).thenReturn(transactionTemplate);
            when(sqlCreatorTemplate.updateQuery(any())).thenReturn("UPDATE users SET name = :name WHERE id = :id");

            TransactionStatus mockStatus = mock(TransactionStatus.class);
            when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
                TransactionCallback<?> callback = invocation.getArgument(0);
                return callback.doInTransaction(mockStatus);
            });
            when(dbOperationService.update(eq(namedParameterJdbcTemplate), anyMap(), anyString()))
                    .thenThrow(new RuntimeException("Connection lost"));

            List<BulkUpdate> updates = List.of(
                    new BulkUpdate(Map.of("name", "A"), "id==1")
            );

            assertThatThrownBy(() -> updateService.patchBulk("test", null, "users", updates))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Connection lost");

            verify(mockStatus).setRollbackOnly();
        }
    }

    @Nested
    @DisplayName("JdbcDeleteService Tests")
    class JdbcDeleteServiceTests {

        private JdbcDeleteService deleteService;

        @BeforeEach
        void setup() {
            deleteService = new JdbcDeleteService(jdbcManager, sqlCreatorTemplate, dbOperationService);
        }

        @Test
        void delete_single_returnsOne() throws DbException {
            when(jdbcManager.getTable("test", null, "users")).thenReturn(usersTable);
            when(jdbcManager.getDialect("test")).thenReturn(dialect);
            when(jdbcManager.getNamedParameterJdbcTemplate("test")).thenReturn(namedParameterJdbcTemplate);
            when(jdbcManager.getTxnTemplate("test")).thenReturn(transactionTemplate);
            when(sqlCreatorTemplate.deleteQuery(any())).thenReturn("DELETE FROM users WHERE id = :id");

            when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
                TransactionCallback<?> callback = invocation.getArgument(0);
                return callback.doInTransaction(null);
            });
            when(dbOperationService.delete(eq(namedParameterJdbcTemplate), anyMap(), anyString())).thenReturn(1);

            int result = deleteService.delete("test", null, "users", "id==1");

            assertThat(result).isEqualTo(1);
        }

        @Test
        void delete_withoutFilter_throwsDbException() {
            assertThatThrownBy(() -> deleteService.delete("test", null, "users", null))
                    .isInstanceOf(DbException.class)
                    .hasMessageContaining("DELETE without filter is not allowed");
        }

        @Test
        void delete_blankFilter_throwsDbException() {
            assertThatThrownBy(() -> deleteService.delete("test", null, "users", "   "))
                    .isInstanceOf(DbException.class)
                    .hasMessageContaining("DELETE without filter is not allowed");
        }

        @Test
        void delete_invalidTable_throwsDbException() throws DbException {
            when(jdbcManager.getTable("test", null, "nonexistent"))
                    .thenThrow(new DbException(DbErrorCode.INVALID_REQUEST, "Invalid table"));

            assertThatThrownBy(() -> deleteService.delete("test", null, "nonexistent", "id==1"))
                    .isInstanceOf(DbException.class);
        }

        @Test
        void delete_noMatch_returnsZero() throws DbException {
            when(jdbcManager.getTable("test", null, "users")).thenReturn(usersTable);
            when(jdbcManager.getDialect("test")).thenReturn(dialect);
            when(jdbcManager.getNamedParameterJdbcTemplate("test")).thenReturn(namedParameterJdbcTemplate);
            when(jdbcManager.getTxnTemplate("test")).thenReturn(transactionTemplate);
            when(sqlCreatorTemplate.deleteQuery(any())).thenReturn("DELETE FROM users WHERE id = :id");

            when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
                TransactionCallback<?> callback = invocation.getArgument(0);
                return callback.doInTransaction(null);
            });
            when(dbOperationService.delete(eq(namedParameterJdbcTemplate), anyMap(), anyString())).thenReturn(0);

            int result = deleteService.delete("test", null, "users", "id==999");

            assertThat(result).isEqualTo(0);
        }

        @Test
        void delete_transactionReturnsNull_returnsZero() throws DbException {
            when(jdbcManager.getTable("test", null, "users")).thenReturn(usersTable);
            when(jdbcManager.getDialect("test")).thenReturn(dialect);
            when(jdbcManager.getTxnTemplate("test")).thenReturn(transactionTemplate);
            when(sqlCreatorTemplate.deleteQuery(any())).thenReturn("DELETE FROM users WHERE id = :id");

            when(transactionTemplate.execute(any())).thenReturn(null);

            int result = deleteService.delete("test", null, "users", "id==1");

            assertThat(result).isEqualTo(0);
        }

        @Test
        void delete_transactionException_rollsBackAndThrows() throws DbException {
            when(jdbcManager.getTable("test", null, "users")).thenReturn(usersTable);
            when(jdbcManager.getDialect("test")).thenReturn(dialect);
            when(jdbcManager.getNamedParameterJdbcTemplate("test")).thenReturn(namedParameterJdbcTemplate);
            when(jdbcManager.getTxnTemplate("test")).thenReturn(transactionTemplate);
            when(sqlCreatorTemplate.deleteQuery(any())).thenReturn("DELETE FROM users WHERE id = :id");

            TransactionStatus mockStatus = mock(TransactionStatus.class);
            when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
                TransactionCallback<?> callback = invocation.getArgument(0);
                return callback.doInTransaction(mockStatus);
            });
            when(dbOperationService.delete(eq(namedParameterJdbcTemplate), anyMap(), anyString()))
                    .thenThrow(new RuntimeException("DB error"));

            assertThatThrownBy(() -> deleteService.delete("test", null, "users", "id==1"))
                    .isInstanceOf(RuntimeException.class);

            verify(mockStatus).setRollbackOnly();
        }

        @Test
        void deleteBulk_multipleFilters_returnsTotal() throws DbException {
            when(jdbcManager.getTable("test", null, "users")).thenReturn(usersTable);
            when(jdbcManager.getDialect("test")).thenReturn(dialect);
            when(jdbcManager.getNamedParameterJdbcTemplate("test")).thenReturn(namedParameterJdbcTemplate);
            when(jdbcManager.getTxnTemplate("test")).thenReturn(transactionTemplate);
            when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
                TransactionCallback<?> callback = invocation.getArgument(0);
                return callback.doInTransaction(null);
            });
            when(sqlCreatorTemplate.deleteQuery(any())).thenReturn("DELETE FROM users WHERE id = :id");
            when(dbOperationService.delete(eq(namedParameterJdbcTemplate), anyMap(), anyString()))
                    .thenReturn(1).thenReturn(3);

            List<String> filters = List.of("id==1", "id==2");

            int result = deleteService.deleteBulk("test", null, "users", filters);

            assertThat(result).isEqualTo(4);
        }

        @Test
        void deleteBulk_nullFilters_throwsDbException() {
            assertThatThrownBy(() -> deleteService.deleteBulk("test", null, "users", null))
                    .isInstanceOf(DbException.class)
                    .hasMessageContaining("at least one filter");
        }

        @Test
        void deleteBulk_emptyFilters_throwsDbException() {
            assertThatThrownBy(() -> deleteService.deleteBulk("test", null, "users", List.of()))
                    .isInstanceOf(DbException.class)
                    .hasMessageContaining("at least one filter");
        }

        @Test
        void deleteBulk_blankFilter_throwsDbException() throws DbException {
            when(jdbcManager.getTable("test", null, "users")).thenReturn(usersTable);
            when(jdbcManager.getTxnTemplate("test")).thenReturn(transactionTemplate);
            TransactionStatus mockStatus = mock(TransactionStatus.class);
            when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
                TransactionCallback<?> callback = invocation.getArgument(0);
                return callback.doInTransaction(mockStatus);
            });

            List<String> filters = List.of("   ");

            assertThatThrownBy(() -> deleteService.deleteBulk("test", null, "users", filters))
                    .isInstanceOf(dev.suprim.query.exception.DbRuntimeException.class);
            verify(mockStatus).setRollbackOnly();
        }

        @Test
        void deleteBulk_nullFilterEntry_throwsDbException() throws DbException {
            when(jdbcManager.getTable("test", null, "users")).thenReturn(usersTable);
            when(jdbcManager.getTxnTemplate("test")).thenReturn(transactionTemplate);
            TransactionStatus mockStatus = mock(TransactionStatus.class);
            when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
                TransactionCallback<?> callback = invocation.getArgument(0);
                return callback.doInTransaction(mockStatus);
            });

            List<String> filters = new ArrayList<>();
            filters.add(null);

            assertThatThrownBy(() -> deleteService.deleteBulk("test", null, "users", filters))
                    .isInstanceOf(dev.suprim.query.exception.DbRuntimeException.class);
            verify(mockStatus).setRollbackOnly();
        }

        @Test
        void deleteBulk_transactionReturnsNull_returnsZero() throws DbException {
            when(jdbcManager.getTable("test", null, "users")).thenReturn(usersTable);
            when(jdbcManager.getTxnTemplate("test")).thenReturn(transactionTemplate);
            when(transactionTemplate.execute(any())).thenReturn(null);

            List<String> filters = List.of("id==1");

            int result = deleteService.deleteBulk("test", null, "users", filters);

            assertThat(result).isEqualTo(0);
        }

        @Test
        void deleteBulk_runtimeException_rollsBackAndThrows() throws DbException {
            when(jdbcManager.getTable("test", null, "users")).thenReturn(usersTable);
            when(jdbcManager.getDialect("test")).thenReturn(dialect);
            when(jdbcManager.getNamedParameterJdbcTemplate("test")).thenReturn(namedParameterJdbcTemplate);
            when(jdbcManager.getTxnTemplate("test")).thenReturn(transactionTemplate);
            when(sqlCreatorTemplate.deleteQuery(any())).thenReturn("DELETE FROM users WHERE id = :id");

            TransactionStatus mockStatus = mock(TransactionStatus.class);
            when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
                TransactionCallback<?> callback = invocation.getArgument(0);
                return callback.doInTransaction(mockStatus);
            });
            when(dbOperationService.delete(eq(namedParameterJdbcTemplate), anyMap(), anyString()))
                    .thenThrow(new RuntimeException("Connection lost"));

            List<String> filters = List.of("id==1");

            assertThatThrownBy(() -> deleteService.deleteBulk("test", null, "users", filters))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Connection lost");

            verify(mockStatus).setRollbackOnly();
        }
    }

    @Nested
    @DisplayName("JdbcRawQueryService Tests")
    class JdbcRawQueryServiceTests {

        private JdbcRawQueryService rawQueryService;

        @BeforeEach
        void setup() {
            rawQueryService = new JdbcRawQueryService(jdbcManager);
        }

        @Nested
        @DisplayName("queryOne")
        class QueryOneTests {

            @Test
            void queryOne_returnsRow() throws DbException {
                when(jdbcManager.getNamedParameterJdbcTemplate("test")).thenReturn(namedParameterJdbcTemplate);
                Map<String, Object> expected = Map.of("id", 1L, "name", "Alice");
                when(namedParameterJdbcTemplate.queryForMap(anyString(), anyMap())).thenReturn(expected);

                Optional<Map<String, Object>> result = rawQueryService.queryOne(
                        "test", "SELECT * FROM users WHERE id = :id", Map.of("id", 1L));

                assertThat(result).isPresent().contains(expected);
            }

            @Test
            void queryOne_noResult_returnsEmpty() throws DbException {
                when(jdbcManager.getNamedParameterJdbcTemplate("test")).thenReturn(namedParameterJdbcTemplate);
                when(namedParameterJdbcTemplate.queryForMap(anyString(), anyMap()))
                        .thenThrow(new EmptyResultDataAccessException(1));

                Optional<Map<String, Object>> result = rawQueryService.queryOne(
                        "test", "SELECT * FROM users WHERE id = :id", Map.of("id", 999L));

                assertThat(result).isEmpty();
            }

            @Test
            void queryOne_dataAccessException_throwsDbException() {
                when(jdbcManager.getNamedParameterJdbcTemplate("test")).thenReturn(namedParameterJdbcTemplate);
                when(namedParameterJdbcTemplate.queryForMap(anyString(), anyMap()))
                        .thenThrow(new DataAccessResourceFailureException("Connection refused"));

                assertThatThrownBy(() -> rawQueryService.queryOne(
                        "test", "SELECT 1", Map.of()))
                        .isInstanceOf(DbException.class)
                        .hasMessageContaining("Raw query failed");
            }

            @Test
            void queryOne_dbNotFound_throwsDbException() {
                when(jdbcManager.getNamedParameterJdbcTemplate("unknown")).thenReturn(null);

                assertThatThrownBy(() -> rawQueryService.queryOne(
                        "unknown", "SELECT 1", Map.of()))
                        .isInstanceOf(DbException.class)
                        .hasMessageContaining("DB not found");
            }
        }

        @Nested
        @DisplayName("queryList")
        class QueryListTests {

            @Test
            void queryList_returnsRows() throws DbException {
                when(jdbcManager.getNamedParameterJdbcTemplate("test")).thenReturn(namedParameterJdbcTemplate);
                List<Map<String, Object>> expected = List.of(
                        Map.of("id", 1L, "name", "Alice"),
                        Map.of("id", 2L, "name", "Bob")
                );
                when(namedParameterJdbcTemplate.queryForList(anyString(), anyMap())).thenReturn(expected);

                List<Map<String, Object>> result = rawQueryService.queryList(
                        "test", "SELECT * FROM users", Map.of());

                assertThat(result).hasSize(2).isEqualTo(expected);
            }

            @Test
            void queryList_noResults_returnsEmptyList() throws DbException {
                when(jdbcManager.getNamedParameterJdbcTemplate("test")).thenReturn(namedParameterJdbcTemplate);
                when(namedParameterJdbcTemplate.queryForList(anyString(), anyMap())).thenReturn(List.of());

                List<Map<String, Object>> result = rawQueryService.queryList(
                        "test", "SELECT * FROM users WHERE 1=0", Map.of());

                assertThat(result).isEmpty();
            }

            @Test
            void queryList_dataAccessException_throwsDbException() {
                when(jdbcManager.getNamedParameterJdbcTemplate("test")).thenReturn(namedParameterJdbcTemplate);
                when(namedParameterJdbcTemplate.queryForList(anyString(), anyMap()))
                        .thenThrow(new DataAccessResourceFailureException("Timeout"));

                assertThatThrownBy(() -> rawQueryService.queryList(
                        "test", "SELECT 1", Map.of()))
                        .isInstanceOf(DbException.class)
                        .hasMessageContaining("Raw query failed");
            }

            @Test
            void queryList_dbNotFound_throwsDbException() {
                when(jdbcManager.getNamedParameterJdbcTemplate("unknown")).thenReturn(null);

                assertThatThrownBy(() -> rawQueryService.queryList(
                        "unknown", "SELECT 1", Map.of()))
                        .isInstanceOf(DbException.class)
                        .hasMessageContaining("DB not found");
            }
        }

        @Nested
        @DisplayName("execute")
        class ExecuteTests {

            @Test
            void execute_returnsAffectedRows() throws DbException {
                when(jdbcManager.getTxnTemplate("test")).thenReturn(transactionTemplate);
                when(jdbcManager.getNamedParameterJdbcTemplate("test")).thenReturn(namedParameterJdbcTemplate);
                when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
                    TransactionCallback<?> callback = invocation.getArgument(0);
                    return callback.doInTransaction(mock(TransactionStatus.class));
                });
                when(namedParameterJdbcTemplate.update(anyString(), anyMap())).thenReturn(3);

                int result = rawQueryService.execute(
                        "test", "UPDATE users SET name = :name WHERE id = :id",
                        Map.of("name", "Charlie", "id", 1L));

                assertThat(result).isEqualTo(3);
            }

            @Test
            void execute_transactionReturnsNull_returnsZero() throws DbException {
                when(jdbcManager.getTxnTemplate("test")).thenReturn(transactionTemplate);
                when(jdbcManager.getNamedParameterJdbcTemplate("test")).thenReturn(namedParameterJdbcTemplate);
                when(transactionTemplate.execute(any())).thenReturn(null);

                int result = rawQueryService.execute(
                        "test", "DELETE FROM users WHERE id = :id", Map.of("id", 1L));

                assertThat(result).isEqualTo(0);
            }

            @Test
            void execute_dataAccessException_throwsDbException() {
                when(jdbcManager.getTxnTemplate("test")).thenReturn(transactionTemplate);
                when(jdbcManager.getNamedParameterJdbcTemplate("test")).thenReturn(namedParameterJdbcTemplate);
                when(transactionTemplate.execute(any())).thenThrow(new DataAccessResourceFailureException("Disk full"));

                assertThatThrownBy(() -> rawQueryService.execute(
                        "test", "INSERT INTO users (name) VALUES (:name)", Map.of("name", "X")))
                        .isInstanceOf(DbException.class)
                        .hasMessageContaining("Raw execute failed");
            }

            @Test
            void execute_txnTemplateNotFound_throwsDbException() {
                when(jdbcManager.getTxnTemplate("unknown")).thenReturn(null);

                assertThatThrownBy(() -> rawQueryService.execute(
                        "unknown", "DELETE FROM users", Map.of()))
                        .isInstanceOf(DbException.class)
                        .hasMessageContaining("Transaction template not found");
            }

            @Test
            void execute_dbNotFound_throwsDbException() {
                when(jdbcManager.getTxnTemplate("test")).thenReturn(transactionTemplate);
                when(jdbcManager.getNamedParameterJdbcTemplate("test")).thenReturn(null);

                assertThatThrownBy(() -> rawQueryService.execute(
                        "test", "DELETE FROM users", Map.of()))
                        .isInstanceOf(DbException.class)
                        .hasMessageContaining("DB not found");
            }
        }
    }
}
