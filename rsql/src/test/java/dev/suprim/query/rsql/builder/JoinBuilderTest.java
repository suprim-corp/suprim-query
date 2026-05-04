package dev.suprim.query.rsql.builder;

import dev.suprim.query.model.JoinDetail;
import dev.suprim.query.model.JoinType;
import dev.suprim.query.rsql.builder.JoinBuilder.JoinCondition;
import dev.suprim.query.rsql.builder.JoinBuilder.JoinField;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JoinBuilderTest {

	// ==================== JoinCondition ====================

	@Nested
	class JoinConditionTest {

		@Test
		void of_shouldCreateConditionWithGivenOperator() {
			var condition = JoinCondition.of("id", JoinOperator.EQ, "workspace_id");
			assertThat(condition.left()).isEqualTo("id");
			assertThat(condition.operator()).isEqualTo(JoinOperator.EQ);
			assertThat(condition.right()).isEqualTo("workspace_id");
		}

		@Test
		void eq_shouldCreateEqualCondition() {
			var condition = JoinCondition.eq("id", "foreign_id");
			assertThat(condition.operator()).isEqualTo(JoinOperator.EQ);
		}

		@Test
		void toExpression_eq_shouldProduceCorrectFormat() {
			var condition = JoinCondition.eq("id", "workspace_id");
			assertThat(condition.toExpression()).isEqualTo("id==workspace_id");
		}

		@Test
		void toExpression_gt_shouldUseGtSymbol() {
			var condition = JoinCondition.of("age", JoinOperator.GT, "min_age");
			assertThat(condition.toExpression()).isEqualTo("age=gt=min_age");
		}

		@Test
		void toExpression_gte_shouldUseGteSymbol() {
			var condition = JoinCondition.of("score", JoinOperator.GTE, "threshold");
			assertThat(condition.toExpression()).isEqualTo("score=gte=threshold");
		}

		@Test
		void toExpression_lt_shouldUseLtSymbol() {
			var condition = JoinCondition.of("price", JoinOperator.LT, "max_price");
			assertThat(condition.toExpression()).isEqualTo("price=lt=max_price");
		}

		@Test
		void toExpression_lte_shouldUseLteSymbol() {
			var condition = JoinCondition.of("qty", JoinOperator.LTE, "limit");
			assertThat(condition.toExpression()).isEqualTo("qty=lte=limit");
		}

		@Test
		void toExpression_isNull_shouldUseIsNullSymbol() {
			var condition = JoinCondition.of("deleted_at", JoinOperator.IS_NULL, "true");
			assertThat(condition.toExpression()).isEqualTo("deleted_at=isnull=true");
		}

		@Test
		void toExpression_isNotNull_shouldUseNotNullSymbol() {
			var condition = JoinCondition.of("email", JoinOperator.IS_NOT_NULL, "true");
			assertThat(condition.toExpression()).isEqualTo("email=notnull=true");
		}
	}

	// ==================== JoinField ====================

	@Nested
	class JoinFieldTest {

		@Test
		void of_shouldCreateFieldWithoutAlias() {
			var field = JoinField.of("member_id");
			assertThat(field.column()).isEqualTo("member_id");
			assertThat(field.alias()).isNull();
		}

		@Test
		void aliased_shouldCreateFieldWithAlias() {
			var field = JoinField.aliased("role", "member_role");
			assertThat(field.column()).isEqualTo("role");
			assertThat(field.alias()).isEqualTo("member_role");
		}

		@Test
		void toExpression_noAlias_shouldReturnColumnOnly() {
			var field = JoinField.of("name");
			assertThat(field.toExpression()).isEqualTo("name");
		}

		@Test
		void toExpression_withAlias_shouldReturnColumnColonAlias() {
			var field = JoinField.aliased("role", "member_role");
			assertThat(field.toExpression()).isEqualTo("role:member_role");
		}

		@Test
		void toExpression_nullAlias_shouldReturnColumnOnly() {
			var field = new JoinField("col", null);
			assertThat(field.toExpression()).isEqualTo("col");
		}

		@Test
		void toExpression_blankAlias_shouldReturnColumnOnly() {
			var field = new JoinField("col", "   ");
			assertThat(field.toExpression()).isEqualTo("col");
		}

		@Test
		void toExpression_emptyAlias_shouldReturnColumnOnly() {
			var field = new JoinField("col", "");
			assertThat(field.toExpression()).isEqualTo("col");
		}
	}

	// ==================== JoinBuilder factory methods ====================

	@Nested
	class FactoryMethodsTest {

		@Test
		void inner_shouldCreateInnerJoin() {
			JoinDetail detail = JoinBuilder.inner("users")
					.on(JoinCondition.eq("id", "user_id"))
					.build();
			assertThat(detail.joinType()).isEqualTo(JoinType.INNER);
			assertThat(detail.table()).isEqualTo("users");
		}

		@Test
		void left_shouldCreateLeftJoin() {
			JoinDetail detail = JoinBuilder.left("orders")
					.on(JoinCondition.eq("id", "order_id"))
					.build();
			assertThat(detail.joinType()).isEqualTo(JoinType.LEFT);
		}

		@Test
		void right_shouldCreateRightJoin() {
			JoinDetail detail = JoinBuilder.right("products")
					.on(JoinCondition.eq("id", "product_id"))
					.build();
			assertThat(detail.joinType()).isEqualTo(JoinType.RIGHT);
		}

		@Test
		void full_shouldCreateFullJoin() {
			JoinDetail detail = JoinBuilder.full("inventory")
					.on(JoinCondition.eq("id", "inv_id"))
					.build();
			assertThat(detail.joinType()).isEqualTo(JoinType.FULL);
		}
	}

	// ==================== JoinBuilder configuration ====================

	@Nested
	class ConfigurationTest {

		@Test
		void schema_shouldSetSchemaName() {
			JoinDetail detail = JoinBuilder.inner("users")
					.schema("public")
					.on(JoinCondition.eq("id", "user_id"))
					.build();
			assertThat(detail.schemaName()).isEqualTo("public");
		}

		@Test
		void with_shouldSetWithTable() {
			JoinDetail detail = JoinBuilder.inner("members")
					.with("workspaces")
					.on(JoinCondition.eq("id", "workspace_id"))
					.build();
			assertThat(detail.withTable()).isEqualTo("workspaces");
		}

		@Test
		void fields_shouldSetFieldExpressions() {
			JoinDetail detail = JoinBuilder.inner("users")
					.on(JoinCondition.eq("id", "user_id"))
					.fields(JoinField.of("name"), JoinField.aliased("email", "user_email"))
					.build();
			assertThat(detail.fields()).containsExactly("name", "email:user_email");
		}

		@Test
		void multipleOnConditions_shouldCollectAll() {
			JoinDetail detail = JoinBuilder.inner("users")
					.on(JoinCondition.eq("id", "user_id"))
					.on(JoinCondition.of("tenant_id", JoinOperator.EQ, "tenant_id"))
					.build();
			assertThat(detail.on()).containsExactly("id==user_id", "tenant_id==tenant_id");
		}

		@Test
		void filter_shouldSetFilterFromFilterBuilder() {
			JoinDetail detail = JoinBuilder.inner("users")
					.on(JoinCondition.eq("id", "user_id"))
					.filter(FilterBuilder.and().eq("status", "ACTIVE"))
					.build();
			assertThat(detail.filter()).isEqualTo("status=='ACTIVE'");
		}
	}

	// ==================== JoinBuilder build edge cases ====================

	@Nested
	class BuildEdgeCasesTest {

		@Test
		void build_noFields_shouldReturnNullFields() {
			JoinDetail detail = JoinBuilder.inner("users")
					.on(JoinCondition.eq("id", "user_id"))
					.build();
			assertThat(detail.fields()).isNull();
		}

		@Test
		void build_noConditions_shouldReturnNullOn() {
			JoinDetail detail = JoinBuilder.inner("users").build();
			assertThat(detail.on()).isNull();
		}

		@Test
		void build_noFilter_shouldReturnNullFilter() {
			JoinDetail detail = JoinBuilder.inner("users")
					.on(JoinCondition.eq("id", "user_id"))
					.build();
			assertThat(detail.filter()).isNull();
		}

		@Test
		void build_noSchema_shouldReturnNullSchema() {
			JoinDetail detail = JoinBuilder.inner("users")
					.on(JoinCondition.eq("id", "user_id"))
					.build();
			assertThat(detail.schemaName()).isNull();
		}

		@Test
		void build_noWithTable_shouldReturnNullWithTable() {
			JoinDetail detail = JoinBuilder.inner("users")
					.on(JoinCondition.eq("id", "user_id"))
					.build();
			assertThat(detail.withTable()).isNull();
		}

		@Test
		void build_fullConfiguration_shouldSetAllFields() {
			JoinDetail detail = JoinBuilder.left("workspace_members")
					.schema("app")
					.with("workspaces")
					.on(JoinCondition.eq("id", "workspace_id"))
					.fields(JoinField.of("member_id"), JoinField.aliased("role", "member_role"))
					.filter(FilterBuilder.and().eq("status", "ACTIVE").isNotNull("email"))
					.build();

			assertThat(detail.schemaName()).isEqualTo("app");
			assertThat(detail.table()).isEqualTo("workspace_members");
			assertThat(detail.withTable()).isEqualTo("workspaces");
			assertThat(detail.joinType()).isEqualTo(JoinType.LEFT);
			assertThat(detail.on()).containsExactly("id==workspace_id");
			assertThat(detail.fields()).containsExactly("member_id", "role:member_role");
			assertThat(detail.filter()).isEqualTo("(status=='ACTIVE' and email=nn='true')");
		}
	}
}
