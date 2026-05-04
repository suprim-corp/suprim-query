package dev.suprim.query.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DbExceptionTest {

    @Test
    void constructor_withErrorCodeOnly_shouldUseDefaultMessage() {
        DbException ex = new DbException(DbErrorCode.INVALID_REQUEST);

        assertThat(ex.getMessage()).isEqualTo("Invalid request");
        assertThat(ex.getCode()).isEqualTo(400);
        assertThat(ex.getData()).isNull();
    }

    @Test
    void constructor_withErrorCodeAndMessage_shouldUseCustomMessage() {
        DbException ex = new DbException(DbErrorCode.NOT_FOUND, "User not found");

        assertThat(ex.getMessage()).isEqualTo("User not found");
        assertThat(ex.getCode()).isEqualTo(404);
        assertThat(ex.getData()).isNull();
    }

    @Test
    void constructor_withErrorCodeMessageAndData_shouldIncludeData() {
        Object data = new Object[]{"field1", "field2"};
        DbException ex = new DbException(DbErrorCode.CONFLICT, "Duplicate entry", data);

        assertThat(ex.getMessage()).isEqualTo("Duplicate entry");
        assertThat(ex.getCode()).isEqualTo(409);
        assertThat(ex.getData()).isEqualTo(data);
    }

    @Test
    void constructor_withErrorCodeAndCause_shouldPreserveCause() {
        RuntimeException cause = new RuntimeException("Original error");
        DbException ex = new DbException(DbErrorCode.SERVER_ERROR, cause);

        assertThat(ex.getMessage()).isEqualTo("Server error");
        assertThat(ex.getCode()).isEqualTo(99);
        assertThat(ex.getCause()).isEqualTo(cause);
        assertThat(ex.getData()).isNull();
    }

    @Test
    void allErrorCodes_shouldHaveCorrectValues() {
        assertThat(DbErrorCode.SUCCESS.getCode()).isEqualTo(1);
        assertThat(DbErrorCode.SUCCESS.getMessage()).isEqualTo("Success");

        assertThat(DbErrorCode.SERVER_ERROR.getCode()).isEqualTo(99);
        assertThat(DbErrorCode.SERVER_ERROR.getMessage()).isEqualTo("Server error");

        assertThat(DbErrorCode.INVALID_REQUEST.getCode()).isEqualTo(400);
        assertThat(DbErrorCode.INVALID_REQUEST.getMessage()).isEqualTo("Invalid request");

        assertThat(DbErrorCode.UNAUTHORIZED.getCode()).isEqualTo(401);
        assertThat(DbErrorCode.UNAUTHORIZED.getMessage()).isEqualTo("Unauthorized");

        assertThat(DbErrorCode.FORBIDDEN.getCode()).isEqualTo(403);
        assertThat(DbErrorCode.FORBIDDEN.getMessage()).isEqualTo("Forbidden");

        assertThat(DbErrorCode.NOT_FOUND.getCode()).isEqualTo(404);
        assertThat(DbErrorCode.NOT_FOUND.getMessage()).isEqualTo("Not found");

        assertThat(DbErrorCode.CONFLICT.getCode()).isEqualTo(409);
        assertThat(DbErrorCode.CONFLICT.getMessage()).isEqualTo("Conflict");

        assertThat(DbErrorCode.NOT_CONFIGURED.getCode()).isEqualTo(503);
        assertThat(DbErrorCode.NOT_CONFIGURED.getMessage()).isEqualTo("Not configured");
    }

    @Test
    void errorCode_values_shouldContainAllCodes() {
        DbErrorCode[] codes = DbErrorCode.values();
        assertThat(codes).hasSize(8);
    }

    @Test
    void errorCode_valueOf_shouldReturnCorrectEnum() {
        assertThat(DbErrorCode.valueOf("SUCCESS")).isEqualTo(DbErrorCode.SUCCESS);
        assertThat(DbErrorCode.valueOf("NOT_FOUND")).isEqualTo(DbErrorCode.NOT_FOUND);
    }

    @Test
    void dbRuntimeException_shouldWrapDbExceptionAndExposeCode() {
        DbException cause = new DbException(DbErrorCode.INVALID_REQUEST, "bad filter");
        DbRuntimeException ex = new DbRuntimeException(cause);

        assertThat(ex.getCode()).isEqualTo(400);
        assertThat(ex.getMessage()).isEqualTo("bad filter");
        assertThat(ex.getCause()).isEqualTo(cause);
    }
}
