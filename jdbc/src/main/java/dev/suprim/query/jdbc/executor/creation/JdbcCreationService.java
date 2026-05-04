package dev.suprim.query.jdbc.executor.creation;

import dev.suprim.query.exception.DbErrorCode;
import dev.suprim.query.exception.DbException;
import dev.suprim.query.exception.DbRuntimeException;
import dev.suprim.query.jdbc.operation.DbOperationService;
import dev.suprim.query.jdbc.operation.JdbcManager;
import dev.suprim.query.jdbc.operation.SqlCreatorTemplate;
import dev.suprim.query.jdbc.processor.TSIDProcessor;
import dev.suprim.query.model.DbColumn;
import dev.suprim.query.model.DbTable;
import dev.suprim.query.model.context.CreateContext;
import dev.suprim.query.model.context.InsertableColumn;
import dev.suprim.query.model.dto.CreationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import static org.springframework.util.CollectionUtils.isEmpty;

@Slf4j
@RequiredArgsConstructor
public class JdbcCreationService implements CreationService {
    private final TSIDProcessor tsidProcessor;
    private final SqlCreatorTemplate sqlCreatorTemplate;
    private final JdbcManager jdbcManager;
    private final DbOperationService dbOperationService;

    @Override
    public CreationResponse execute(
            String dbId,
            String schema,
            String table,
            List<String> columns,
            Map<String, Object> data,
            boolean tsIdEnabled,
            List<String> sequences
    ) {
        try {
            // 1. Get actual table
            DbTable dbTable = jdbcManager.getTable(dbId, schema, table);

            // 2. Determine the columns to be included in an insert statement
            List<String> insertableColumns = isEmpty(columns)
                    ? new ArrayList<>(data.keySet().stream().toList())
                    : new ArrayList<>(columns);

            // 3. Check if tsId is enabled and add those values for PK.
            Map<String, Object> tsIdMap = null;

            if (tsIdEnabled) {
                List<DbColumn> pkColumns = dbTable.buildPkColumns();

                for (DbColumn pkColumn : pkColumns) {
                    // Guard against duplicate PK column if caller already included it
                    if (!insertableColumns.contains(pkColumn.name())) {
                        log.debug("Adding primary key columns - {}", pkColumn.name());
                        insertableColumns.add(pkColumn.name());
                    }
                }

                tsIdMap = tsidProcessor.processTsId(data, pkColumns);
            }

            // 4. Convert to an insertable column object
            List<InsertableColumn> insertableColumnList = new ArrayList<>();

            for (String colName : insertableColumns) {
                insertableColumnList.add(new InsertableColumn(colName, null));
            }

            log.debug("Sequences - {}", sequences);

            // Handle oracle sequence
            if (nonNull(sequences)) {
                for (String sequence : sequences) {
                    String[] colSeq = sequence.split(":");
                    // Check if size = 2, else ignore, fall at insert
                    if (colSeq.length == 2) {
                        insertableColumnList.add(new InsertableColumn(
                                colSeq[0],
                                dbTable.schema() + "." + colSeq[1] + ".nextval"
                        ));
                    }
                }
            }

            jdbcManager.getDialect(dbId).processTypes(
                    dbTable,
                    insertableColumns,
                    data
            );

            CreateContext context = new CreateContext(
                    dbId,
                    dbTable,
                    insertableColumns,
                    insertableColumnList
            );
            String sql = sqlCreatorTemplate.create(context);

            log.debug("SQL - {}", sql);
            log.debug("Data - {}", data);

            CreationResponse createResponse = jdbcManager.getTxnTemplate(dbId).execute(status -> {
                try {
                    return dbOperationService.create(
                            jdbcManager.getNamedParameterJdbcTemplate(dbId),
                            data, sql, dbTable
                    );
                } catch (Exception e) {
                    status.setRollbackOnly();
                    // Log full exception to capture root cause
                    Throwable rootCause = e;
                    while (nonNull(rootCause.getCause())) {
                        rootCause = rootCause.getCause();
                    }
                    log.error("INSERT failed - Root cause: {}", rootCause.getMessage());
                    log.error("Full exception:", e);
                    throw new RuntimeException(
                            "ERROR DURING INSERTION -> " + e.getMessage() + " | Root cause: " + rootCause.getMessage(), e);
                }
            });

            if (tsIdEnabled) {
                if (createResponse == null) {
                    throw new DbRuntimeException(
                            new DbException(DbErrorCode.SERVER_ERROR, "Creation returned null response"));
                }
                // Priority: DB-returned keys > pre-generated TSID keys.
                // Rationale: DB keys may contain sequence/trigger values unknown before insert.
                // TSID values were already written into `data` during processing, so the caller
                // already has them regardless of what we return here.
                if (isNull(createResponse.keys())) {
                    return new CreationResponse(createResponse.row(), tsIdMap);
                }
            }

            return createResponse;
        } catch (DataAccessException | DbException e) {
            log.error("ERROR -> {}", e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }
}
