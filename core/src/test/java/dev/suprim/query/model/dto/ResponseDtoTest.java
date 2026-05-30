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

    @Test
    void page_shouldHoldAllFields() {
        List<Map<String, Object>> data = List.of(Map.of("id", 1L), Map.of("id", 2L));
        Page<Map<String, Object>> page = Page.<Map<String, Object>>builder()
                .data(data)
                .total(10L)
                .limit(2)
                .offset(0L)
                .hasNext(true)
                .build();

        assertThat(page.data()).hasSize(2);
        assertThat(page.total()).isEqualTo(10L);
        assertThat(page.limit()).isEqualTo(2);
        assertThat(page.offset()).isZero();
        assertThat(page.hasNext()).isTrue();
    }

    @Test
    void page_lastPage_hasNextFalse() {
        List<Map<String, Object>> data = List.of(Map.of("id", 9L), Map.of("id", 10L));
        Page<Map<String, Object>> page = Page.<Map<String, Object>>builder()
                .data(data)
                .total(10L)
                .limit(2)
                .offset(8L)
                .hasNext(false)
                .build();

        assertThat(page.hasNext()).isFalse();
    }

    @Test
    void page_emptyData_shouldWork() {
        Page<Map<String, Object>> page = Page.<Map<String, Object>>builder()
                .data(List.of())
                .total(0L)
                .limit(10)
                .offset(0L)
                .hasNext(false)
                .build();

        assertThat(page.data()).isEmpty();
        assertThat(page.total()).isZero();
        assertThat(page.hasNext()).isFalse();
    }

    @Test
    void page_nullData_shouldDefaultToEmptyList() {
        Page<Map<String, Object>> page = Page.<Map<String, Object>>builder()
                .data(null)
                .total(0L)
                .limit(10)
                .offset(0L)
                .hasNext(false)
                .build();

        assertThat(page.data()).isNotNull().isEmpty();
    }

    @Test
    void page_dataIsUnmodifiable() {
        List<Map<String, Object>> mutableData = new java.util.ArrayList<>(List.of(Map.of("id", 1L)));
        Page<Map<String, Object>> page = Page.<Map<String, Object>>builder()
                .data(mutableData)
                .total(1L)
                .limit(10)
                .offset(0L)
                .hasNext(false)
                .build();

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> page.data().add(Map.of("id", 2L)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void bulkUpdate_shouldHoldDataAndFilter() {
        Map<String, Object> data = Map.of("status", "ACTIVE");
        BulkUpdate bulkUpdate = new BulkUpdate(data, "id==1");

        assertThat(bulkUpdate.data()).containsEntry("status", "ACTIVE");
        assertThat(bulkUpdate.filter()).isEqualTo("id==1");
    }

    @Test
    void bulkUpdate_nullDataAndFilter_shouldWork() {
        BulkUpdate bulkUpdate = new BulkUpdate(null, null);

        assertThat(bulkUpdate.data()).isNull();
        assertThat(bulkUpdate.filter()).isNull();
    }
}
