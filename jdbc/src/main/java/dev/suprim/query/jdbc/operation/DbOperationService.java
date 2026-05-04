package dev.suprim.query.jdbc.operation;

import dev.suprim.query.dialect.Dialect;
import dev.suprim.query.exception.DbException;
import dev.suprim.query.model.DbTable;
import dev.suprim.query.model.dto.CountResponse;
import dev.suprim.query.model.dto.CreateBulkResponse;
import dev.suprim.query.model.dto.CreationResponse;
import dev.suprim.query.model.dto.ExistsResponse;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.List;
import java.util.Map;

public interface DbOperationService {
    int update(
            NamedParameterJdbcTemplate namedParameterJdbcTemplate,
            Map<String, Object> paramMap,
            String sql
    );

    List<Map<String, Object>> read(
            NamedParameterJdbcTemplate namedParameterJdbcTemplate,
            Map<String, Object> paramMap,
            String sql,
            Dialect dialect
    );

    Map<String, Object> findOne(
            NamedParameterJdbcTemplate namedParameterJdbcTemplate,
            String sql,
            Map<String, Object> paramMap
    );

    ExistsResponse exists(
            NamedParameterJdbcTemplate namedParameterJdbcTemplate,
            Map<String, Object> paramMap,
            String sql
    );

    CountResponse count(
            NamedParameterJdbcTemplate namedParameterJdbcTemplate,
            Map<String, Object> paramMap,
            String sql
    );

    Object queryCustom(
            NamedParameterJdbcTemplate namedParameterJdbcTemplate,
            boolean single,
            String sql,
            Map<String, Object> params
    );

    int delete(
            NamedParameterJdbcTemplate namedParameterJdbcTemplate,
            Map<String, Object> params,
            String sql
    );

    CreationResponse create(
            NamedParameterJdbcTemplate namedParameterJdbcTemplate,
            Map<String, Object> data,
            String sql,
            DbTable dbTable
    ) throws DbException;

    CreateBulkResponse batchUpdate(
            NamedParameterJdbcTemplate namedParameterJdbcTemplate,
            List<Map<String, Object>> dataList,
            String sql,
            DbTable dbTable
    );

    CreateBulkResponse batchUpdate(
            NamedParameterJdbcTemplate namedParameterJdbcTemplate,
            List<Map<String, Object>> dataList,
            String sql
    );
}
