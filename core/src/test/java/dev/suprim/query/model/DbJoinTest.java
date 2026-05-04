package dev.suprim.query.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DbJoinTest {

    @Test
    void render_withBasicJoin_shouldReturnJoinClause() {
        DbJoin join = DbJoin.builder()
                .tableName("orders")
                .alias("o")
                .joinType("INNER")
                .build();

        String result = join.render();

        assertThat(result).contains("INNER JOIN orders o");
    }

    @Test
    void render_withOnCondition_shouldIncludeOnClause() {
        DbColumn leftCol = createColumn("users", "id", "u");
        DbColumn rightCol = createColumn("orders", "user_id", "o");

        DbJoin join = DbJoin.builder()
                .tableName("orders")
                .alias("o")
                .joinType("LEFT")
                .onLeft(leftCol)
                .onRight(rightCol)
                .onOperator("=")
                .build();

        String result = join.render();

        assertThat(result)
                .contains("LEFT JOIN orders o")
                .contains("ON u.\"id\" = o.\"user_id\"");
    }

    @Test
    void render_withAndConditions_shouldIncludeAndClauses() {
        DbColumn leftCol = createColumn("users", "id", "u");
        DbColumn rightCol = createColumn("orders", "user_id", "o");
        DbColumn andLeft = createColumn("users", "tenant_id", "u");
        DbColumn andRight = createColumn("orders", "tenant_id", "o");

        DbJoin join = DbJoin.builder()
                .tableName("orders")
                .alias("o")
                .joinType("INNER")
                .onLeft(leftCol)
                .onRight(rightCol)
                .onOperator("=")
                .build();

        join.addAndCondition(andLeft, "=", andRight);

        String result = join.render();

        assertThat(result)
                .contains("ON u.\"id\" = o.\"user_id\"")
                .contains("AND u.\"tenant_id\" = o.\"tenant_id\"");
    }

    @Test
    void render_withAdditionalWhere_shouldIncludeWhereClauses() {
        DbJoin join = DbJoin.builder()
                .tableName("orders")
                .alias("o")
                .joinType("INNER")
                .build();

        join.addAdditionalWhere("o.status = 'ACTIVE'");
        join.addAdditionalWhere("o.amount > 0");

        String result = join.render();

        assertThat(result)
                .contains("AND o.status = 'ACTIVE'")
                .contains("AND o.amount > 0");
    }

    @Test
    void addOn_shouldSetOnCondition() {
        DbColumn leftCol = createColumn("users", "id", "u");
        DbColumn rightCol = createColumn("orders", "user_id", "o");

        DbJoin join = new DbJoin();
        join.addOn(leftCol, "=", rightCol);

        assertThat(join.getOnLeft()).isEqualTo(leftCol);
        assertThat(join.getOnRight()).isEqualTo(rightCol);
        assertThat(join.getOnOperator()).isEqualTo("=");
    }

    @Test
    void addAndCondition_shouldInitializeListIfNull() {
        DbColumn leftCol = createColumn("users", "tenant_id", "u");
        DbColumn rightCol = createColumn("orders", "tenant_id", "o");

        DbJoin join = new DbJoin();
        join.addAndCondition(leftCol, "=", rightCol);

        assertThat(join.getAndConditions()).hasSize(1);
    }

    @Test
    void addAndCondition_shouldAppendToExisting() {
        DbColumn leftCol1 = createColumn("users", "tenant_id", "u");
        DbColumn rightCol1 = createColumn("orders", "tenant_id", "o");
        DbColumn leftCol2 = createColumn("users", "active", "u");
        DbColumn rightCol2 = createColumn("orders", "active", "o");

        DbJoin join = new DbJoin();
        join.addAndCondition(leftCol1, "=", rightCol1);
        join.addAndCondition(leftCol2, "=", rightCol2);

        assertThat(join.getAndConditions()).hasSize(2);
    }

    @Test
    void addAdditionalWhere_shouldInitializeListIfNull() {
        DbJoin join = new DbJoin();
        join.addAdditionalWhere("status = 'ACTIVE'");

        assertThat(join.getAdditionalWhere()).hasSize(1);
    }

    @Test
    void addAdditionalWhere_shouldAppendToExisting() {
        DbJoin join = new DbJoin();
        join.addAdditionalWhere("status = 'ACTIVE'");
        join.addAdditionalWhere("amount > 0");

        assertThat(join.getAdditionalWhere()).hasSize(2);
    }

    @Test
    void builder_shouldSetAllFields() {
        DbJoin join = DbJoin.builder()
                .tableName("orders")
                .alias("o")
                .joinType("RIGHT")
                .build();

        assertThat(join.getTableName()).isEqualTo("orders");
        assertThat(join.getAlias()).isEqualTo("o");
        assertThat(join.getJoinType()).isEqualTo("RIGHT");
    }

    @Test
    void render_withNullOnLeft_shouldNotIncludeOnClause() {
        DbJoin join = DbJoin.builder()
                .tableName("orders")
                .alias("o")
                .joinType("INNER")
                .build();

        String result = join.render();

        assertThat(result).contains("INNER JOIN orders o");
        assertThat(result).doesNotContain("ON");
    }

    @Test
    void render_withNullAndConditions_shouldNotIncludeAndClauses() {
        DbColumn leftCol = createColumn("users", "id", "u");
        DbColumn rightCol = createColumn("orders", "user_id", "o");

        DbJoin join = DbJoin.builder()
                .tableName("orders")
                .alias("o")
                .joinType("LEFT")
                .onLeft(leftCol)
                .onRight(rightCol)
                .onOperator("=")
                .andConditions(null)
                .build();

        String result = join.render();

        assertThat(result).contains("ON u.\"id\" = o.\"user_id\"");
    }

    @Test
    void render_withEmptyAndConditions_shouldNotIncludeAndClauses() {
        DbColumn leftCol = createColumn("users", "id", "u");
        DbColumn rightCol = createColumn("orders", "user_id", "o");

        DbJoin join = DbJoin.builder()
                .tableName("orders")
                .alias("o")
                .joinType("LEFT")
                .onLeft(leftCol)
                .onRight(rightCol)
                .onOperator("=")
                .andConditions(java.util.List.of())
                .build();

        String result = join.render();

        assertThat(result).contains("ON u.\"id\" = o.\"user_id\"");
    }

    @Test
    void render_withNullAdditionalWhere_shouldNotIncludeWhereClauses() {
        DbJoin join = DbJoin.builder()
                .tableName("orders")
                .alias("o")
                .joinType("INNER")
                .additionalWhere(null)
                .build();

        String result = join.render();

        assertThat(result).doesNotContain("AND");
    }

    @Test
    void render_withEmptyAdditionalWhere_shouldNotIncludeWhereClauses() {
        DbJoin join = DbJoin.builder()
                .tableName("orders")
                .alias("o")
                .joinType("INNER")
                .additionalWhere(java.util.List.of())
                .build();

        String result = join.render();

        assertThat(result).doesNotContain("AND");
    }

    private DbColumn createColumn(String tableName, String colName, String tableAlias) {
        return new DbColumn(tableName, colName, "", tableAlias, false, "varchar", false, false, String.class, "\"", "");
    }
}
