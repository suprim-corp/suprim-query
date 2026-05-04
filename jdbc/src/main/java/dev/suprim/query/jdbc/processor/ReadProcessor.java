package dev.suprim.query.jdbc.processor;

import dev.suprim.query.exception.DbException;
import dev.suprim.query.model.context.ReadContext;

public interface ReadProcessor {
    void process(ReadContext readContext) throws DbException;
}
