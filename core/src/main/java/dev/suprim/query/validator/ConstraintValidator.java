package dev.suprim.query.validator;

import dev.suprim.query.exception.DbException;

public interface ConstraintValidator {
    void validate(Object value, String placeholderName) throws DbException;
}
