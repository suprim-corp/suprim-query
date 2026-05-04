package dev.suprim.query.jdbc.executor.read;

import dev.suprim.query.exception.DbException;
import dev.suprim.query.model.context.ReadContext;

import java.util.List;
import java.util.Map;

public interface ReadService {
    List<Map<String, Object>> findAll(ReadContext readContext) throws DbException;

    Map<String, Object> findOne(ReadContext readContext) throws DbException;

    long count(ReadContext readContext) throws DbException;
}
