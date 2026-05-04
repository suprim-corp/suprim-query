package dev.suprim.query.validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CustomPlaceholderValidatorsTest {
    private CustomPlaceholderValidators validators;

    @BeforeEach
    void setUp() {
        validators = new CustomPlaceholderValidators();
    }

    @Test
    void getValidator_withIsRequired_shouldReturnIsRequiredValidator() {
        ConstraintValidator validator = validators.getValidator(CustomPlaceholderValidators.IS_REQUIRED);
        assertThat(validator).isInstanceOf(IsRequiredValidator.class);
    }

    @Test
    void getValidator_withIsUUID_shouldReturnIsUUIDValidator() {
        ConstraintValidator validator = validators.getValidator(CustomPlaceholderValidators.IS_UUID);
        assertThat(validator).isInstanceOf(IsUUIDValidator.class);
    }

    @Test
    void getValidator_withUnknownConstraint_shouldReturnNull() {
        ConstraintValidator validator = validators.getValidator("unknown_constraint");
        assertThat(validator).isNull();
    }

    @Test
    void constants_shouldHaveCorrectValues() {
        assertThat(CustomPlaceholderValidators.IS_REQUIRED).isEqualTo("is_required");
        assertThat(CustomPlaceholderValidators.IS_UUID).isEqualTo("is_uuid");
    }
}
