package dev.suprim.query.jdbc.executor.read;

import dev.suprim.query.exception.DbException;
import dev.suprim.query.model.context.ReadContext;
import dev.suprim.query.model.dto.Page;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ReadService {
    List<Map<String, Object>> findAll(ReadContext readContext) throws DbException;

    Map<String, Object> findOne(ReadContext readContext) throws DbException;

    long count(ReadContext readContext) throws DbException;

    Page<Map<String, Object>> findPage(ReadContext readContext) throws DbException;

    /**
     * Executes the query and maps all result rows to the given type.
     *
     * @param readContext query context (filters, sorting, pagination)
     * @param type        target record or POJO class with no-arg constructor
     * @param <T>         target type
     * @return list of mapped instances (never null, may be empty)
     * @throws DbException if the query fails
     * @throws dev.suprim.query.mapping.MappingException if row mapping fails (missing constructor, type coercion error)
     */
    <T> List<T> findAll(ReadContext readContext, Class<T> type) throws DbException;

    /**
     * Executes the query expecting at most one row and maps it to the given type.
     * Returns empty if no row is found.
     *
     * @param readContext query context
     * @param type        target record or POJO class
     * @param <T>         target type
     * @return Optional containing the mapped instance, or empty if no result
     * @throws DbException if the query fails
     * @throws dev.suprim.query.mapping.MappingException if row mapping fails
     */
    <T> Optional<T> findOne(ReadContext readContext, Class<T> type) throws DbException;

    /**
     * Executes a paginated query and maps result rows to the given type.
     *
     * @param readContext query context (must include limit/offset for pagination)
     * @param type        target record or POJO class
     * @param <T>         target type
     * @return page containing mapped data, total count, and pagination metadata
     * @throws DbException if the query fails
     * @throws dev.suprim.query.mapping.MappingException if row mapping fails
     */
    <T> Page<T> findPage(ReadContext readContext, Class<T> type) throws DbException;
}
