package dev.suprim.query.model.dto;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ResponseDtoTest {

    @Test
    void updateResponse_shouldHoldRowCount() {
        UpdateResponse response = UpdateResponse.builder().rows(5).build();
        assertThat(response.rows()).isEqualTo(5);
    }

    @Test
    void updateResponse_withZeroRows_shouldWork() {
        UpdateResponse response = UpdateResponse.builder().rows(0).build();
        assertThat(response.rows()).isZero();
    }

    @Test
    void creationResponse_shouldHoldRowAndKeys() {
        Map<String, Object> keys = Map.of("id", 1L);
        CreationResponse response = new CreationResponse(1, keys);

        assertThat(response.row()).isEqualTo(1);
        assertThat(response.keys()).containsEntry("id", 1L);
    }

    @Test
    void creationResponse_withNullKeys_shouldWork() {
        CreationResponse response = new CreationResponse(1, null);

        assertThat(response.row()).isEqualTo(1);
        assertThat(response.keys()).isNull();
    }

    @Test
    void createBulkResponse_shouldHoldRowsAndKeys() {
        int[] rows = {1, 1, 1};
        List<Map<String, Object>> keys = List.of(
                Map.of("id", 1L),
                Map.of("id", 2L),
                Map.of("id", 3L)
        );
        CreateBulkResponse response = new CreateBulkResponse(rows, keys);

        assertThat(response.rows()).hasSize(3);
        assertThat(response.keys()).hasSize(3);
    }

    @Test
    void existsResponse_shouldHoldExistsFlag() {
        ExistsResponse existsTrue = new ExistsResponse(true);
        ExistsResponse existsFalse = new ExistsResponse(false);

        assertThat(existsTrue.exists()).isTrue();
        assertThat(existsFalse.exists()).isFalse();
    }

    @Test
    void countResponse_shouldHoldCount() {
        CountResponse response = new CountResponse(100L);
        assertThat(response.count()).isEqualTo(100L);
    }

    @Test
    void countResponse_withZero_shouldWork() {
        CountResponse response = new CountResponse(0L);
        assertThat(response.count()).isZero();
    }

    @Test
    void deleteResponse_shouldHoldRowCount() {
        DeleteResponse response = DeleteResponse.builder().rows(3).build();
        assertThat(response.rows()).isEqualTo(3);
    }

    @Test
    void deleteResponse_withZeroRows_shouldWork() {
        DeleteResponse response = DeleteResponse.builder().rows(0).build();
        assertThat(response.rows()).isZero();
    }
}
