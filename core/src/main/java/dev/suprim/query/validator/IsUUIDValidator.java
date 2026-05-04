package dev.suprim.query.validator;

import dev.suprim.query.exception.DbErrorCode;
import dev.suprim.query.exception.DbException;

import java.util.UUID;

public class IsUUIDValidator implements ConstraintValidator {
    @Override
    public void validate(Object value, String placeholderName) throws DbException {
        if (!(value instanceof String) || !isValidUUID((String) value)) {
            throw new DbException(DbErrorCode.INVALID_REQUEST,
                    "%s is not a valid UUID.".formatted(placeholderName));
        }
    }

    private boolean isValidUUID(String value) {
        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
