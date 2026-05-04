package dev.suprim.query.validator;

import dev.suprim.query.exception.DbException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class IsRequiredValidatorTest {
    private IsRequiredValidator validator;

    @BeforeEach
    void setUp() {
        validator = new IsRequiredValidator();
    }

    @Test
    void validate_withValidString_shouldNotThrow() {
        assertDoesNotThrow(() -> validator.validate("value", "testField"));
    }

    @Test
    void validate_withValidNumber_shouldNotThrow() {
        assertDoesNotThrow(() -> validator.validate(123, "testField"));
    }

    @Test
    void validate_withValidObject_shouldNotThrow() {
        assertDoesNotThrow(() -> validator.validate(new Object(), "testField"));
    }

    @Test
    void validate_withNullValue_shouldThrowDbException() {
        assertThatThrownBy(() -> validator.validate(null, "testField"))
                .isInstanceOf(DbException.class)
                .hasMessageContaining("testField is required and cannot be null");
    }

    @Test
    void validate_withEmptyString_shouldThrowDbException() {
        assertThatThrownBy(() -> validator.validate("", "testField"))
                .isInstanceOf(DbException.class)
                .hasMessageContaining("testField is required and cannot be null");
    }

    @Test
    void validate_withWhitespaceString_shouldThrowDbException() {
        assertThatThrownBy(() -> validator.validate("   ", "testField"))
                .isInstanceOf(DbException.class)
                .hasMessageContaining("testField is required and cannot be null");
    }

    @Test
    void validate_withTabString_shouldThrowDbException() {
        assertThatThrownBy(() -> validator.validate("\t\n", "testField"))
                .isInstanceOf(DbException.class)
                .hasMessageContaining("testField is required and cannot be null");
    }
}
