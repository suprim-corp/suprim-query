package dev.suprim.query.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JoinDetailTest {

    @Test
    void getJoinType_withNull_shouldReturnInner() {
        JoinDetail detail = new JoinDetail("public", "orders", "users", List.of(), List.of(), null, null);

        assertThat(detail.getJoinType()).isEqualTo("INNER");
    }

    @Test
    void getJoinType_withBlank_shouldReturnInner() {
        JoinDetail detail = new JoinDetail("public", "orders", "users", List.of(), List.of(), null, JoinType.INNER);

        assertThat(detail.getJoinType()).isEqualTo("INNER");
    }

    @Test
    void getJoinType_withLeft_shouldReturnLeft() {
        JoinDetail detail = new JoinDetail("public", "orders", "users", List.of(), List.of(), null, JoinType.LEFT);

        assertThat(detail.getJoinType()).isEqualTo("LEFT");
    }

    @Test
    void getJoinType_withRight_shouldReturnRight() {
        JoinDetail detail = new JoinDetail("public", "orders", "users", List.of(), List.of(), null, JoinType.RIGHT);

        assertThat(detail.getJoinType()).isEqualTo("RIGHT");
    }

    @Test
    void hasWith_withNonNull_shouldReturnTrue() {
        JoinDetail detail = new JoinDetail("public", "orders", "users", List.of(), List.of(), null, null);

        assertThat(detail.hasWith()).isTrue();
    }

    @Test
    void hasWith_withNull_shouldReturnFalse() {
        JoinDetail detail = new JoinDetail("public", "orders", null, List.of(), List.of(), null, null);

        assertThat(detail.hasWith()).isFalse();
    }

    @Test
    void hasWith_withBlank_shouldReturnFalse() {
        JoinDetail detail = new JoinDetail("public", "orders", "  ", List.of(), List.of(), null, null);

        assertThat(detail.hasWith()).isFalse();
    }

    @Test
    void hasOn_withNonEmptyList_shouldReturnTrue() {
        JoinDetail detail = new JoinDetail("public", "orders", "users", List.of(), List.of("id=user_id"), null, null);

        assertThat(detail.hasOn()).isTrue();
    }

    @Test
    void hasOn_withNull_shouldReturnFalse() {
        JoinDetail detail = new JoinDetail("public", "orders", "users", List.of(), null, null, null);

        assertThat(detail.hasOn()).isFalse();
    }

    @Test
    void hasOn_withEmptyList_shouldReturnFalse() {
        JoinDetail detail = new JoinDetail("public", "orders", "users", List.of(), List.of(), null, null);

        assertThat(detail.hasOn()).isFalse();
    }

    @Test
    void hasFilter_withNonBlank_shouldReturnTrue() {
        JoinDetail detail = new JoinDetail("public", "orders", "users", List.of(), List.of(), "status=active", null);

        assertThat(detail.hasFilter()).isTrue();
    }

    @Test
    void hasFilter_withNull_shouldReturnFalse() {
        JoinDetail detail = new JoinDetail("public", "orders", "users", List.of(), List.of(), null, null);

        assertThat(detail.hasFilter()).isFalse();
    }

    @Test
    void hasFilter_withBlank_shouldReturnFalse() {
        JoinDetail detail = new JoinDetail("public", "orders", "users", List.of(), List.of(), "   ", null);

        assertThat(detail.hasFilter()).isFalse();
    }

    @Test
    void recordAccessors_shouldReturnCorrectValues() {
        List<String> fields = List.of("id", "name");
        List<String> on = List.of("id=user_id");
        JoinDetail detail = new JoinDetail("public", "orders", "users", fields, on, "active=true", JoinType.LEFT);

        assertThat(detail.schemaName()).isEqualTo("public");
        assertThat(detail.table()).isEqualTo("orders");
        assertThat(detail.withTable()).isEqualTo("users");
        assertThat(detail.fields()).containsExactly("id", "name");
        assertThat(detail.on()).containsExactly("id=user_id");
        assertThat(detail.filter()).isEqualTo("active=true");
        assertThat(detail.joinType()).isEqualTo(JoinType.LEFT);
    }
}
