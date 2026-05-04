package dev.suprim.query.rsql.builder;

import dev.suprim.query.model.JoinDetail;
import dev.suprim.query.model.JoinType;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Fluent builder for {@link JoinDetail}.
 *
 * <pre>{@code
 * JoinBuilder.inner("workspace_members")
 *     .on(JoinCondition.of("id", JoinOperator.EQ, "workspace_id"))
 *     .fields(JoinField.of("member_id"), JoinField.aliased("role", "member_role"))
 *     .filter(FilterBuilder.and().eq("status", "ACTIVE"))
 *     .build();
 * }</pre>
 */
public class JoinBuilder {

    // ==================== TYPES ====================

    /**
     * Typed join ON condition: leftField JoinOperator rightField.
     * Serializes to RSQL expression consumed by JoinProcessor.
     */
    public record JoinCondition(String left, JoinOperator operator, String right) {

        public static JoinCondition of(String left, JoinOperator operator, String right) {
            return new JoinCondition(left, operator, right);
        }

        public static JoinCondition eq(String left, String right) {
            return new JoinCondition(left, JoinOperator.EQ, right);
        }

        public String toExpression() {
            return left + operator.symbol() + right;
        }
    }

    /**
     * Typed join field selector with optional alias.
     * Serializes to column expression consumed by JoinProcessor.
     */
    public record JoinField(String column, String alias) {

        public static JoinField of(String column) {
            return new JoinField(column, null);
        }

        public static JoinField aliased(String column, String alias) {
            return new JoinField(column, alias);
        }

        public String toExpression() {
            return Objects.nonNull(alias) && !alias.isBlank()
                ? column + ":" + alias
                : column;
        }
    }

    // ==================== BUILDER ====================

    private final String table;
    private String schemaName;
    private String withTable;
    private final JoinType joinType;
    private final List<JoinField> fields = new ArrayList<>();
    private final List<JoinCondition> conditions = new ArrayList<>();
    private String filter;

    private JoinBuilder(String table, JoinType joinType) {
        this.table = table;
        this.joinType = joinType;
    }

    public static JoinBuilder inner(String table) {
        return new JoinBuilder(table, JoinType.INNER);
    }

    public static JoinBuilder left(String table) {
        return new JoinBuilder(table, JoinType.LEFT);
    }

    public static JoinBuilder right(String table) {
        return new JoinBuilder(table, JoinType.RIGHT);
    }

    public static JoinBuilder full(String table) {
        return new JoinBuilder(table, JoinType.FULL);
    }

    public JoinBuilder schema(String schemaName) {
        this.schemaName = schemaName;
        return this;
    }

    public JoinBuilder with(String withTable) {
        this.withTable = withTable;
        return this;
    }

    public JoinBuilder on(JoinCondition condition) {
        this.conditions.add(condition);
        return this;
    }

    public JoinBuilder fields(JoinField... fields) {
        this.fields.addAll(List.of(fields));
        return this;
    }

    public JoinBuilder filter(FilterBuilder filterBuilder) {
        this.filter = filterBuilder.build();
        return this;
    }

    public JoinDetail build() {
        List<String> onExpressions = conditions.stream()
            .map(JoinCondition::toExpression)
            .toList();

        List<String> fieldExpressions = fields.stream()
            .map(JoinField::toExpression)
            .toList();

        return new JoinDetail(
            schemaName,
            table,
            withTable,
            fieldExpressions.isEmpty() ? null : fieldExpressions,
            onExpressions.isEmpty() ? null : onExpressions,
            filter,
            joinType
        );
    }
}
