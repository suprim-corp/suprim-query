package dev.suprim.query.jdbc.executor.read;

import dev.suprim.query.exception.DbErrorCode;
import dev.suprim.query.exception.DbException;
import dev.suprim.query.exception.DbRuntimeException;
import dev.suprim.query.jdbc.operation.DbOperationService;
import dev.suprim.query.jdbc.operation.JdbcManager;
import dev.suprim.query.jdbc.operation.SqlCreatorTemplate;
import dev.suprim.query.jdbc.processor.ReadProcessor;
import dev.suprim.query.mapping.ResultMapper;
import dev.suprim.query.model.context.ReadContext;
import dev.suprim.query.model.dto.Page;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Builder
public record JdbcReadService(
        JdbcManager jdbcManager,
        DbOperationService dbOperationService,
        List<ReadProcessor> processorList,
        SqlCreatorTemplate sqlCreatorTemplate,
        ResultMapper resultMapper
) implements ReadService {

    @Override
    public List<Map<String, Object>> findAll(ReadContext readContext) throws DbException {
        log.debug("readContext : {}", readContext);

        try {
            for (ReadProcessor processor : processorList) {
                processor.process(readContext);
            }

            String sql = sqlCreatorTemplate.query(readContext);

            log.debug("{}", sql);
            log.debug("{}", readContext.getParamMap());

            return dbOperationService.read(
                    jdbcManager.getNamedParameterJdbcTemplate(readContext.getDbId()),
                    readContext.getParamMap(),
                    sql,
                    jdbcManager.getDialect(readContext.getDbId())
            );
        } catch (DbException e) {
            throw e;
        } catch (Exception e) {
            log.error("ERROR IN READ OPERATION -> {}", e.getMessage());
            throw new DbException(DbErrorCode.SERVER_ERROR);
        }
    }

    @Override
    public Map<String, Object> findOne(ReadContext readContext) throws DbException {
        for (ReadProcessor processor : processorList) {
            processor.process(readContext);
        }

        String sql = sqlCreatorTemplate.findOne(readContext);
        Map<String, Object> bindValues = readContext.getParamMap();

        log.debug("SQL - {}", sql);
        log.debug("Params - {}", bindValues);

        try {
            return dbOperationService.findOne(
                    jdbcManager.getNamedParameterJdbcTemplate(readContext.getDbId()),
                    sql,
                    bindValues
            );
        } catch (EmptyResultDataAccessException e) {
            log.warn(e.getMessage());
            return null;
        } catch (DataAccessException e) {
            log.error("ERROR IN READ OPERATION -> {}", e.getMessage());
            throw new RuntimeException(e);
        } catch (Exception e) {
            log.error("ERROR IN READ OPERATION -> {}", e.getMessage());
            throw new DbException(DbErrorCode.SERVER_ERROR);
        }
    }

    @Override
    public long count(ReadContext readContext) throws DbException {
        for (ReadProcessor processor : processorList) {
            processor.process(readContext);
        }

        String sql = sqlCreatorTemplate.count(readContext);
        log.debug("{}", sql);
        log.debug("{}", readContext.getParamMap());

        try {
            return dbOperationService
                    .count(
                            jdbcManager.getNamedParameterJdbcTemplate(readContext.getDbId()),
                            readContext.getParamMap(),
                            sql
                    )
                    .count();
        } catch (DataAccessException e) {
            log.error("Error in read op : ", e);
            throw new DbException(DbErrorCode.SERVER_ERROR);
        }
    }

    @Override
    public Page<Map<String, Object>> findPage(ReadContext readContext) throws DbException {
        // Run processors once — they mutate the context (paramMap, cols, rootWhere, etc.)
        for (ReadProcessor processor : processorList) {
            processor.process(readContext);
        }

        // Resolve effective limit: 0 or -1 means "use defaultFetchLimit"
        int requestedLimit = readContext.getLimit();
        int effectiveLimit = (requestedLimit <= 0)
                ? readContext.getDefaultFetchLimit()
                : requestedLimit;

        String querySql = sqlCreatorTemplate.query(readContext);
        String countSql = sqlCreatorTemplate.count(readContext);

        log.debug("findPage query SQL: {}", querySql);
        log.debug("findPage count SQL: {}", countSql);

        // Wrap data + count in a single transaction for consistency
        Page<Map<String, Object>> page = jdbcManager.getTxnTemplate(readContext.getDbId()).execute(status -> {
            try {
                List<Map<String, Object>> data = dbOperationService.read(
                        jdbcManager.getNamedParameterJdbcTemplate(readContext.getDbId()),
                        readContext.getParamMap(),
                        querySql,
                        jdbcManager.getDialect(readContext.getDbId())
                );

                long total = dbOperationService
                        .count(
                                jdbcManager.getNamedParameterJdbcTemplate(readContext.getDbId()),
                                readContext.getParamMap(),
                                countSql
                        )
                        .count();

                long offset = readContext.getOffset();
                boolean hasNext = (offset + data.size()) < total;

                return Page.<Map<String, Object>>builder()
                           .data(List.copyOf(data))
                           .total(total)
                           .limit(effectiveLimit)
                           .offset(offset)
                           .hasNext(hasNext)
                           .build();
            } catch (DbException e) {
                throw new DbRuntimeException(e);
            }
        });

        if (page == null) {
            throw new DbException(DbErrorCode.SERVER_ERROR, "Transaction returned null");
        }

        return page;
    }

    @Override
    public <T> List<T> findAll(
            ReadContext readContext,
            Class<T> type
    ) throws DbException {
        // TODO: consider streaming/chunked mapping for large result sets to avoid double allocation
        List<Map<String, Object>> rows = findAll(readContext);
        return resultMapper.mapList(rows, type);
    }

    @Override
    public <T> Optional<T> findOne(
            ReadContext readContext,
            Class<T> type
    ) throws DbException {
        Map<String, Object> row = findOne(readContext);
        return resultMapper.mapOptional(row, type);
    }

    @Override
    public <T> Page<T> findPage(
            ReadContext readContext,
            Class<T> type
    ) throws DbException {
        Page<Map<String, Object>> rawPage = findPage(readContext);
        List<T> mappedData = resultMapper.mapList(rawPage.data(), type);
        return Page.<T>builder()
                   .data(mappedData)
                   .total(rawPage.total())
                   .limit(rawPage.limit())
                   .offset(rawPage.offset())
                   .hasNext(rawPage.hasNext())
                   .build();
    }
}
