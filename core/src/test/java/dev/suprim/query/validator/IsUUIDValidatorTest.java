package dev.suprim.query.validator;

import dev.suprim.query.exception.DbException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class IsUUIDValidatorTest {
    private IsUUIDValidator validator;

    @BeforeEach
    void setUp() {
        validator = new IsUUIDValidator();
    }

    @Test
    void validate_withValidUUID_shouldNotThrow() {
        String validUUID = UUID.randomUUID().toString();
        assertDoesNotThrow(() -> validator.validate(validUUID, "testField"));
    }

    @Test
    void validate_withNullValue_shouldThrowDbException() {
        assertThatThrownBy(() -> validator.validate(null, "testField"))
                .isInstanceOf(DbException.class)
                .hasMessageContaining("testField is not a valid UUID");
    }

    @Test
    void validate_withNonStringValue_shouldThrowDbException() {
        assertThatThrownBy(() -> validator.validate(123, "testField"))
                .isInstanceOf(DbException.class)
                .hasMessageContaining("testField is not a valid UUID");
    }

    @Test
    void validate_withInvalidUUIDString_shouldThrowDbException() {
        assertThatThrownBy(() -> validator.validate("not-a-uuid", "testField"))
                .isInstanceOf(DbException.class)
                .hasMessageContaining("testField is not a valid UUID");
    }

    @Test
    void validate_withEmptyString_shouldThrowDbException() {
        assertThatThrownBy(() -> validator.validate("", "testField"))
                .isInstanceOf(DbException.class)
                .hasMessageContaining("testField is not a valid UUID");
    }

    @Test
    void validate_withPartialUUID_shouldThrowDbException() {
        assertThatThrownBy(() -> validator.validate("550e8400-e29b-41d4", "testField"))
                .isInstanceOf(DbException.class)
                .hasMessageContaining("testField is not a valid UUID");
    }
}
