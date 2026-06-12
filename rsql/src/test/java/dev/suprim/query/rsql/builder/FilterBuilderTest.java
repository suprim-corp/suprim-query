package dev.suprim.query.rsql.builder;

import dev.suprim.query.rsql.builder.FilterBuilder.Comparison;
import dev.suprim.query.rsql.builder.FilterBuilder.Group;
import dev.suprim.query.rsql.builder.FilterBuilder.LogicalOperator;
import dev.suprim.query.rsql.builder.FilterBuilder.Raw;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FilterBuilderTest {

	// ==================== LogicalOperator ====================

	@Nested
	class LogicalOperatorTest {

		@Test
		void and_shouldHaveCorrectSymbol() {
			assertThat(LogicalOperator.AND.symbol()).isEqualTo("and");
		}

		@Test
		void or_shouldHaveCorrectSymbol() {
			assertThat(LogicalOperator.OR.symbol()).isEqualTo("or");
		}
	}

	// ==================== Comparison record ====================

	@Nested
	class ComparisonTest {

		@Test
		void toRsql_singleValue_shouldProduceFieldOperatorQuotedValue() {
			var comparison = new Comparison("name", "==", List.of("John"));
			assertThat(comparison.toRsql()).isEqualTo("name=='John'");
		}

		@Test
		void toRsql_multipleValues_shouldProduceParenthesizedList() {
			var comparison = new Comparison("status", "=in=", List.of("ACTIVE", "PENDING"));
			assertThat(comparison.toRsql()).isEqualTo("status=in=(ACTIVE,PENDING)");
		}

		@Test
		void toRsql_singleValueWithSpecialChars_shouldPreserveValue() {
			var comparison = new Comparison("email", "==", List.of("user@example.com"));
			assertThat(comparison.toRsql()).isEqualTo("email=='user@example.com'");
		}
	}

	// ==================== Group record ====================

	@Nested
	class GroupTest {

		@Test
		void toRsql_emptyPredicates_shouldReturnEmptyString() {
			var group = new Group(LogicalOperator.AND, List.of());
			assertThat(group.toRsql()).isEmpty();
		}

		@Test
		void toRsql_singlePredicate_shouldNotWrapInParentheses() {
			var group = new Group(LogicalOperator.AND, List.of(
					new Comparison("name", "==", List.of("test"))
			));
			assertThat(group.toRsql()).isEqualTo("name=='test'");
		}

		@Test
		void toRsql_multiplePredicates_shouldJoinWithOperatorAndWrap() {
			var group = new Group(LogicalOperator.AND, List.of(
					new Comparison("a", "==", List.of("1")),
					new Comparison("b", "==", List.of("2"))
			));
			assertThat(group.toRsql()).isEqualTo("(a=='1' and b=='2')");
		}

		@Test
		void toRsql_orOperator_shouldUseOrSeparator() {
			var group = new Group(LogicalOperator.OR, List.of(
					new Comparison("x", "==", List.of("a")),
					new Comparison("y", "==", List.of("b"))
			));
			assertThat(group.toRsql()).isEqualTo("(x=='a' or y=='b')");
		}
	}

	// ==================== Raw record ====================

	@Nested
	class RawTest {

		@Test
		void toRsql_shouldWrapInParentheses() {
			var raw = new Raw("name==test;age=gt=18");
			assertThat(raw.toRsql()).isEqualTo("(name==test;age=gt=18)");
		}
	}

	// ==================== Builder: factory methods ====================

	@Nested
	class FactoryMethodsTest {

		@Test
		void and_shouldCreateAndBuilder() {
			String result = FilterBuilder.and().eq("a", "1").build();
			assertThat(result).isEqualTo("a=='1'");
		}

		@Test
		void or_shouldCreateOrBuilder() {
			String result = FilterBuilder.or().eq("a", "1").eq("b", "2").build();
			assertThat(result).isEqualTo("(a=='1' or b=='2')");
		}
	}

	// ==================== Builder: comparison operators ====================

	@Nested
	class ComparisonOperatorsTest {

		@Test
		void eq_shouldUseEqualOperator() {
			String result = FilterBuilder.and().eq("name", "John").build();
			assertThat(result).isEqualTo("name=='John'");
		}

		@Test
		void neq_shouldUseNotEqualOperator() {
			String result = FilterBuilder.and().neq("status", "DELETED").build();
			assertThat(result).isEqualTo("status!='DELETED'");
		}

		@Test
		void gt_shouldUseGreaterThanOperator() {
			String result = FilterBuilder.and().gt("age", "18").build();
			assertThat(result).isEqualTo("age=gt='18'");
		}

		@Test
		void gte_shouldUseGreaterThanOrEqualOperator() {
			String result = FilterBuilder.and().gte("score", "90").build();
			assertThat(result).isEqualTo("score=ge='90'");
		}

		@Test
		void lt_shouldUseLessThanOperator() {
			String result = FilterBuilder.and().lt("price", "100").build();
			assertThat(result).isEqualTo("price=lt='100'");
		}

		@Test
		void lte_shouldUseLessThanOrEqualOperator() {
			String result = FilterBuilder.and().lte("quantity", "0").build();
			assertThat(result).isEqualTo("quantity=le='0'");
		}

		@Test
		void in_shouldUseInOperatorWithMultipleValues() {
			String result = FilterBuilder.and().in("role", "ADMIN", "USER").build();
			assertThat(result).isEqualTo("role=in=(ADMIN,USER)");
		}

		@Test
		void in_singleValue_shouldStillUseParenthesizedForm() {
			String result = FilterBuilder.and().in("role", "ADMIN").build();
			// in() always uses Arrays.asList which gives size 1 → still multi-value format
			assertThat(result).contains("=in=");
		}

		@Test
		void notIn_shouldUseNotInOperator() {
			String result = FilterBuilder.and().notIn("status", "DELETED", "ARCHIVED").build();
			assertThat(result).isEqualTo("status=out=(DELETED,ARCHIVED)");
		}

		@Test
		void like_shouldUseLikeOperator() {
			String result = FilterBuilder.and().like("name", "John").build();
			assertThat(result).isEqualTo("name=like='John'");
		}

		@Test
		void ilike_shouldUseILikeOperator() {
			String result = FilterBuilder.and().iLike("name", "john").build();
			assertThat(result).isEqualTo("name=ilike='john'");
		}

		@Test
		void startWith_shouldUseStartWithOperator() {
			String result = FilterBuilder.and().startWith("name", "Jo").build();
			assertThat(result).isEqualTo("name=startWith='Jo'");
		}

		@Test
		void endWith_shouldUseEndWithOperator() {
			String result = FilterBuilder.and().endWith("email", ".com").build();
			assertThat(result).isEqualTo("email=endWith='.com'");
		}

		@Test
		void isNull_shouldUseIsNullOperatorWithTrueValue() {
			String result = FilterBuilder.and().isNull("deleted_at").build();
			assertThat(result).isEqualTo("deleted_at=isnull='true'");
		}

		@Test
		void isNotNull_shouldUseNotNullOperatorWithTrueValue() {
			String result = FilterBuilder.and().isNotNull("email").build();
			assertThat(result).isEqualTo("email=nn='true'");
		}

		@Test
		void jsonbContains_shouldUseJsonbContainOperator() {
			String result = FilterBuilder.and().jsonbContains("metadata", "{\"key\":\"val\"}").build();
			assertThat(result).isEqualTo("metadata=jsonbContain='{\"key\":\"val\"}'");
		}

		@Test
		void jsonbContains_keyValue_shouldSerializeAsJson() {
			String result = FilterBuilder.and().jsonbContains("metadata", "tier", "premium").build();
			assertThat(result).isEqualTo("metadata=jsonbContain='{\"tier\":\"premium\"}'");
		}

		@Test
		void jsonbContains_map_shouldSerializeAllEntries() {
			Map<String, Object> map = new LinkedHashMap<>();
			map.put("tier", "premium");
			map.put("active", true);
			String result = FilterBuilder.and().jsonbContains("metadata", map).build();
			assertThat(result).isEqualTo("metadata=jsonbContain='{\"tier\":\"premium\",\"active\":true}'");
		}

		@Test
		void jsonbContains_map_nestedMap_shouldSerializeRecursively() {
			Map<String, Object> nested = new LinkedHashMap<>();
			nested.put("city", "HCM");
			nested.put("zip", "70000");
			Map<String, Object> map = new LinkedHashMap<>();
			map.put("address", nested);
			String result = FilterBuilder.and().jsonbContains("metadata", map).build();
			assertThat(result).isEqualTo("metadata=jsonbContain='{\"address\":{\"city\":\"HCM\",\"zip\":\"70000\"}}'");
		}

		@Test
		void jsonbContains_map_withList_shouldSerializeAsArray() {
			Map<String, Object> map = new LinkedHashMap<>();
			map.put("tags", List.of("java", "spring"));
			String result = FilterBuilder.and().jsonbContains("metadata", map).build();
			assertThat(result).isEqualTo("metadata=jsonbContain='{\"tags\":[\"java\",\"spring\"]}'");
		}

		@Test
		void jsonbContains_map_withNumber_shouldNotQuote() {
			Map<String, Object> map = new LinkedHashMap<>();
			map.put("count", 42);
			map.put("rate", 3.14);
			String result = FilterBuilder.and().jsonbContains("metadata", map).build();
			assertThat(result).isEqualTo("metadata=jsonbContain='{\"count\":42,\"rate\":3.14}'");
		}

		@Test
		void jsonbContains_map_withNull_shouldSerializeAsNull() {
			Map<String, Object> map = new LinkedHashMap<>();
			map.put("deleted_at", null);
			String result = FilterBuilder.and().jsonbContains("metadata", map).build();
			assertThat(result).isEqualTo("metadata=jsonbContain='{\"deleted_at\":null}'");
		}

		@Test
		void jsonbContains_map_withSpecialChars_shouldEscape() {
			Map<String, Object> map = new LinkedHashMap<>();
			map.put("note", "he said \"hello\"");
			String result = FilterBuilder.and().jsonbContains("metadata", map).build();
			assertThat(result).isEqualTo("metadata=jsonbContain='{\"note\":\"he said \\\"hello\\\"\"}'");
		}

		@Test
		void jsonbContains_map_deeplyNested_shouldSerializeAllLevels() {
			Map<String, Object> level2 = new LinkedHashMap<>();
			level2.put("enabled", true);
			level2.put("threshold", 100);
			Map<String, Object> level1 = new LinkedHashMap<>();
			level1.put("config", level2);
			level1.put("items", List.of(1, 2, 3));
			Map<String, Object> root = new LinkedHashMap<>();
			root.put("settings", level1);
			String result = FilterBuilder.and().jsonbContains("data", root).build();
			assertThat(result).isEqualTo("data=jsonbContain='{\"settings\":{\"config\":{\"enabled\":true,\"threshold\":100},\"items\":[1,2,3]}}'");
		}

		@Test
		void jsonbKeyExists_shouldUseJsonbKeyExistsOperator() {
			String result = FilterBuilder.and().jsonbKeyExists("data", "name").build();
			assertThat(result).isEqualTo("data=jbKeyExist='name'");
		}

		@Test
		void arrayContains_shouldUseArrayContainsOperator() {
			String result = FilterBuilder.and().arrayContains("question_types", "CLOZE").build();
			assertThat(result).isEqualTo("question_types=arrayContains='CLOZE'");
		}
	}

	// ==================== Builder: nesting ====================

	@Nested
	class NestingTest {

		@Test
		void and_nested_shouldNestAndGroup() {
			String result = FilterBuilder.or()
					.eq("a", "1")
					.and(FilterBuilder.and().eq("b", "2").eq("c", "3"))
					.build();
			assertThat(result).isEqualTo("(a=='1' or (b=='2' and c=='3'))");
		}

		@Test
		void or_nested_shouldNestOrGroup() {
			String result = FilterBuilder.and()
					.eq("type", "SECRET")
					.or(FilterBuilder.or().eq("kind", "A").eq("kind", "B"))
					.build();
			assertThat(result).isEqualTo("(type=='SECRET' and (kind=='A' or kind=='B'))");
		}

		@Test
		void deeplyNested_shouldProduceCorrectRsql() {
			String result = FilterBuilder.and()
					.eq("type", "SECRET")
					.or(FilterBuilder.or()
							.eq("principal_kind", "GROUP")
							.and(FilterBuilder.and()
									.eq("principal_kind", "MEMBER")
									.eq("principal_id", "123")
							)
					)
					.build();
			assertThat(result).isEqualTo(
					"(type=='SECRET' and (principal_kind=='GROUP' or (principal_kind=='MEMBER' and principal_id=='123')))"
			);
		}
	}

	// ==================== Builder: raw ====================

	@Nested
	class RawBuilderTest {

		@Test
		void raw_nonBlankString_shouldAddRawPredicate() {
			String result = FilterBuilder.and()
					.eq("a", "1")
					.raw("custom==filter")
					.build();
			assertThat(result).isEqualTo("(a=='1' and (custom==filter))");
		}

		@Test
		void raw_nullString_shouldBeIgnored() {
			String result = FilterBuilder.and()
					.eq("a", "1")
					.raw(null)
					.build();
			assertThat(result).isEqualTo("a=='1'");
		}

		@Test
		void raw_blankString_shouldBeIgnored() {
			String result = FilterBuilder.and()
					.eq("a", "1")
					.raw("   ")
					.build();
			assertThat(result).isEqualTo("a=='1'");
		}

		@Test
		void raw_emptyString_shouldBeIgnored() {
			String result = FilterBuilder.and()
					.eq("a", "1")
					.raw("")
					.build();
			assertThat(result).isEqualTo("a=='1'");
		}
	}

	// ==================== Builder: build edge cases ====================

	@Nested
	class BuildEdgeCasesTest {

		@Test
		void build_noPredicates_shouldReturnEmptyString() {
			String result = FilterBuilder.and().build();
			assertThat(result).isEmpty();
		}

		@Test
		void build_multipleComparisons_shouldJoinWithAnd() {
			String result = FilterBuilder.and()
					.eq("a", "1")
					.eq("b", "2")
					.eq("c", "3")
					.build();
			assertThat(result).isEqualTo("(a=='1' and b=='2' and c=='3')");
		}
	}

	// ==================== Conditional (IfPresent) ====================

	@Nested
	class IfPresentTest {

		@Test
		void eqIfPresent_nonNull_shouldAddPredicate() {
			String result = FilterBuilder.and()
					.eqIfPresent("status", "active")
					.build();
			assertThat(result).isEqualTo("status=='active'");
		}

		@Test
		void eqIfPresent_null_shouldSkip() {
			String result = FilterBuilder.and()
					.eqIfPresent("status", null)
					.build();
			assertThat(result).isEmpty();
		}

		@Test
		void eqIfPresent_nonStringObject_shouldCallToString() {
			java.util.UUID uuid = java.util.UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
			String result = FilterBuilder.and()
					.eqIfPresent("id", uuid)
					.build();
			assertThat(result).isEqualTo("id=='550e8400-e29b-41d4-a716-446655440000'");
		}

		@Test
		void neqIfPresent_nonNull_shouldAddPredicate() {
			String result = FilterBuilder.and()
					.neqIfPresent("role", "guest")
					.build();
			assertThat(result).isEqualTo("role!='guest'");
		}

		@Test
		void neqIfPresent_null_shouldSkip() {
			String result = FilterBuilder.and()
					.neqIfPresent("role", null)
					.build();
			assertThat(result).isEmpty();
		}

		@Test
		void gtIfPresent_nonNull_shouldAddPredicate() {
			String result = FilterBuilder.and()
					.gtIfPresent("age", "18")
					.build();
			assertThat(result).isEqualTo("age=gt='18'");
		}

		@Test
		void gtIfPresent_null_shouldSkip() {
			String result = FilterBuilder.and()
					.gtIfPresent("age", null)
					.build();
			assertThat(result).isEmpty();
		}

		@Test
		void gteIfPresent_nonNull_shouldAddPredicate() {
			String result = FilterBuilder.and()
					.gteIfPresent("price", "100")
					.build();
			assertThat(result).isEqualTo("price=ge='100'");
		}

		@Test
		void gteIfPresent_null_shouldSkip() {
			String result = FilterBuilder.and()
					.gteIfPresent("price", null)
					.build();
			assertThat(result).isEmpty();
		}

		@Test
		void ltIfPresent_nonNull_shouldAddPredicate() {
			String result = FilterBuilder.and()
					.ltIfPresent("stock", "5")
					.build();
			assertThat(result).isEqualTo("stock=lt='5'");
		}

		@Test
		void ltIfPresent_null_shouldSkip() {
			String result = FilterBuilder.and()
					.ltIfPresent("stock", null)
					.build();
			assertThat(result).isEmpty();
		}

		@Test
		void lteIfPresent_nonNull_shouldAddPredicate() {
			String result = FilterBuilder.and()
					.lteIfPresent("rating", "3")
					.build();
			assertThat(result).isEqualTo("rating=le='3'");
		}

		@Test
		void lteIfPresent_null_shouldSkip() {
			String result = FilterBuilder.and()
					.lteIfPresent("rating", null)
					.build();
			assertThat(result).isEmpty();
		}

		@Test
		void iLikeIfPresent_nonBlank_shouldAddPredicate() {
			String result = FilterBuilder.and()
					.iLikeIfPresent("name", "john")
					.build();
			assertThat(result).isEqualTo("name=ilike='john'");
		}

		@Test
		void iLikeIfPresent_null_shouldSkip() {
			String result = FilterBuilder.and()
					.iLikeIfPresent("name", null)
					.build();
			assertThat(result).isEmpty();
		}

		@Test
		void iLikeIfPresent_blank_shouldSkip() {
			String result = FilterBuilder.and()
					.iLikeIfPresent("name", "   ")
					.build();
			assertThat(result).isEmpty();
		}

		@Test
		void likeIfPresent_nonBlank_shouldAddPredicate() {
			String result = FilterBuilder.and()
					.likeIfPresent("name", "john")
					.build();
			assertThat(result).isEqualTo("name=like='john'");
		}

		@Test
		void likeIfPresent_null_shouldSkip() {
			String result = FilterBuilder.and()
					.likeIfPresent("name", null)
					.build();
			assertThat(result).isEmpty();
		}

		@Test
		void likeIfPresent_blank_shouldSkip() {
			String result = FilterBuilder.and()
					.likeIfPresent("name", "")
					.build();
			assertThat(result).isEmpty();
		}

		@Test
		void startWithIfPresent_nonBlank_shouldAddPredicate() {
			String result = FilterBuilder.and()
					.startWithIfPresent("name", "Jo")
					.build();
			assertThat(result).isEqualTo("name=startWith='Jo'");
		}

		@Test
		void startWithIfPresent_null_shouldSkip() {
			String result = FilterBuilder.and()
					.startWithIfPresent("name", null)
					.build();
			assertThat(result).isEmpty();
		}

		@Test
		void startWithIfPresent_blank_shouldSkip() {
			String result = FilterBuilder.and()
					.startWithIfPresent("name", "   ")
					.build();
			assertThat(result).isEmpty();
		}

		@Test
		void endWithIfPresent_nonBlank_shouldAddPredicate() {
			String result = FilterBuilder.and()
					.endWithIfPresent("email", ".com")
					.build();
			assertThat(result).isEqualTo("email=endWith='.com'");
		}

		@Test
		void endWithIfPresent_null_shouldSkip() {
			String result = FilterBuilder.and()
					.endWithIfPresent("email", null)
					.build();
			assertThat(result).isEmpty();
		}

		@Test
		void endWithIfPresent_blank_shouldSkip() {
			String result = FilterBuilder.and()
					.endWithIfPresent("email", "  ")
					.build();
			assertThat(result).isEmpty();
		}

		@Test
		void inIfPresent_nonEmpty_shouldAddPredicate() {
			String result = FilterBuilder.and()
					.inIfPresent("status", "active", "pending")
					.build();
			assertThat(result).isEqualTo("status=in=(active,pending)");
		}

		@Test
		void inIfPresent_null_shouldSkip() {
			String result = FilterBuilder.and()
					.inIfPresent("status", (String[]) null)
					.build();
			assertThat(result).isEmpty();
		}

		@Test
		void inIfPresent_empty_shouldSkip() {
			String result = FilterBuilder.and()
					.inIfPresent("status")
					.build();
			assertThat(result).isEmpty();
		}

		@Test
		void notInIfPresent_nonEmpty_shouldAddPredicate() {
			String result = FilterBuilder.and()
					.notInIfPresent("type", "draft", "archived")
					.build();
			assertThat(result).isEqualTo("type=out=(draft,archived)");
		}

		@Test
		void notInIfPresent_null_shouldSkip() {
			String result = FilterBuilder.and()
					.notInIfPresent("type", (String[]) null)
					.build();
			assertThat(result).isEmpty();
		}

		@Test
		void notInIfPresent_empty_shouldSkip() {
			String result = FilterBuilder.and()
					.notInIfPresent("type")
					.build();
			assertThat(result).isEmpty();
		}

		@Test
		void arrayContainsIfPresent_nonBlank_shouldAddPredicate() {
			String result = FilterBuilder.and()
					.arrayContainsIfPresent("question_types", "CLOZE")
					.build();
			assertThat(result).isEqualTo("question_types=arrayContains='CLOZE'");
		}

		@Test
		void arrayContainsIfPresent_null_shouldSkip() {
			String result = FilterBuilder.and()
					.arrayContainsIfPresent("question_types", null)
					.build();
			assertThat(result).isEmpty();
		}

		@Test
		void arrayContainsIfPresent_blank_shouldSkip() {
			String result = FilterBuilder.and()
					.arrayContainsIfPresent("question_types", "   ")
					.build();
			assertThat(result).isEmpty();
		}

		@Test
		void combined_mixedNullAndNonNull_shouldOnlyIncludePresent() {
			String result = FilterBuilder.and()
					.eqIfPresent("folder_id", "abc-123")
					.eqIfPresent("status", null)
					.eqIfPresent("product", "IELTS")
					.iLikeIfPresent("name", null)
					.build();
			assertThat(result).isEqualTo("(folder_id=='abc-123' and product=='IELTS')");
		}

		@Test
		void combined_allNull_shouldReturnEmpty() {
			String result = FilterBuilder.and()
					.eqIfPresent("a", null)
					.eqIfPresent("b", null)
					.iLikeIfPresent("c", null)
					.build();
			assertThat(result).isEmpty();
		}
	}
}
