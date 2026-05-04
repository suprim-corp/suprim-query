package dev.suprim.query.jdbc.executor.read;

import dev.suprim.query.exception.DbErrorCode;
import dev.suprim.query.exception.DbException;
import dev.suprim.query.jdbc.operation.DbOperationService;
import dev.suprim.query.jdbc.operation.JdbcManager;
import dev.suprim.query.jdbc.operation.SqlCreatorTemplate;
import dev.suprim.query.jdbc.processor.ReadProcessor;
import dev.suprim.query.model.context.ReadContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;

import java.util.List;
import java.util.Map;

@Slf4j
public record JdbcReadService(
        JdbcManager jdbcManager,
        DbOperationService dbOperationService,
        List<ReadProcessor> processorList,
        SqlCreatorTemplate sqlCreatorTemplate
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
}
