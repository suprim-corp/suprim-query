package dev.suprim.query.jdbc.executor;

import dev.suprim.query.dialect.Dialect;
import dev.suprim.query.exception.DbErrorCode;
import dev.suprim.query.exception.DbException;
import dev.suprim.query.jdbc.executor.creation.JdbcCreationService;
import dev.suprim.query.jdbc.executor.deletion.JdbcDeleteService;
import dev.suprim.query.jdbc.executor.read.JdbcReadService;
import dev.suprim.query.jdbc.executor.update.JdbcUpdateService;
import dev.suprim.query.jdbc.operation.DbOperationService;
import dev.suprim.query.jdbc.operation.JdbcManager;
import dev.suprim.query.jdbc.operation.SqlCreatorTemplate;
import dev.suprim.query.jdbc.processor.ReadProcessor;
import dev.suprim.query.jdbc.processor.TSIDProcessor;
import dev.suprim.query.model.DbColumn;
import dev.suprim.query.model.DbTable;
import dev.suprim.query.model.context.ReadContext;
import dev.suprim.query.model.dto.CountResponse;
import dev.suprim.query.model.dto.CreationResponse;
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
    }
}
