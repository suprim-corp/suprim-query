package dev.suprim.query.validator;

import dev.suprim.query.exception.DbErrorCode;
import dev.suprim.query.exception.DbException;

import static java.util.Objects.isNull;

public class IsRequiredValidator implements ConstraintValidator {
    @Override
    public void validate(Object value, String placeholderName) throws DbException {
        if (isNull(value) || (value instanceof String s && s.trim().isEmpty())) {
            throw new DbException(DbErrorCode.INVALID_REQUEST,
                    "%s is required and cannot be null.".formatted(placeholderName));
        }
    }
}
