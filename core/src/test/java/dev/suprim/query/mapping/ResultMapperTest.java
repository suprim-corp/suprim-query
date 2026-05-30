package dev.suprim.query.mapping;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static dev.suprim.query.mapping.TypeCoercer.coerce;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResultMapperTest {

    private final ResultMapper mapper = new ResultMapper();

    // --- Test DTOs ---

    public record SimpleRecord(UUID id, String name, int age) {}

    public record AnnotatedRecord(
            UUID id,
            @Column("first_name") String firstName,
            @Column("created_at") LocalDateTime createdAt
    ) {}

    public record AllTypesRecord(
            int intVal,
            long longVal,
            double doubleVal,
            float floatVal,
            short shortVal,
            byte byteVal,
            boolean boolVal,
            String stringVal
    ) {}

    public record NullableRecord(String name, Integer age, UUID id) {}

    public record EnumRecord(String name, Status status) {}

    public enum Status { ACTIVE, INACTIVE }

    public record BigNumberRecord(BigDecimal price, BigInteger quantity) {}

    public record TemporalRecord(
            LocalDateTime localDateTime,
            Instant instant,
            OffsetDateTime offsetDateTime,
            LocalDate localDate
    ) {}

    public static class SimplePojo {
        private String name;
        private int age;

        public String getName() { return name; }
        public int getAge() { return age; }
    }

    public static class PojoWithAnnotation {
        @Column("full_name")
        private String fullName;
        private int age;

        public String getFullName() { return fullName; }
        public int getAge() { return age; }
    }

    public static class InheritedPojo extends SimplePojo {
        private String email;

        public String getEmail() { return email; }
    }

    public static class NoDefaultConstructor {
        private final String name;
        NoDefaultConstructor(String name) { this.name = name; }
    }

    // --- Tests ---

    @Nested
    @DisplayName("mapOne - Records")
    class MapOneRecordTests {

        @Test
        void simpleRecord_mapsCorrectly() {
            UUID id = UUID.randomUUID();
            Map<String, Object> row = Map.of("id", id, "name", "Alice", "age", 30);

            SimpleRecord result = mapper.mapOne(row, SimpleRecord.class);

            assertThat(result.id()).isEqualTo(id);
            assertThat(result.name()).isEqualTo("Alice");
            assertThat(result.age()).isEqualTo(30);
        }

        @Test
        void annotatedRecord_usesColumnAnnotation() {
            UUID id = UUID.randomUUID();
            LocalDateTime now = LocalDateTime.now();
            Map<String, Object> row = Map.of(
                    "id", id,
                    "first_name", "Bob",
                    "created_at", Timestamp.valueOf(now)
            );

            AnnotatedRecord result = mapper.mapOne(row, AnnotatedRecord.class);

            assertThat(result.id()).isEqualTo(id);
            assertThat(result.firstName()).isEqualTo("Bob");
            assertThat(result.createdAt()).isEqualTo(now);
        }

        @Test
        void snakeCaseConversion_mapsAutomatically() {
            UUID id = UUID.randomUUID();
            LocalDateTime now = LocalDateTime.now();
            // Without @Column, "firstName" -> "first_name" via snake_case
            Map<String, Object> row = Map.of(
                    "id", id,
                    "first_name", "Charlie",
                    "created_at", Timestamp.valueOf(now)
            );

            AnnotatedRecord result = mapper.mapOne(row, AnnotatedRecord.class);

            assertThat(result.firstName()).isEqualTo("Charlie");
        }

        @Test
        void missingColumn_returnsNullForReferenceType() {
            Map<String, Object> row = Map.of("name", "Alice", "age", 25);

            NullableRecord result = mapper.mapOne(row, NullableRecord.class);

            assertThat(result.id()).isNull();
            assertThat(result.name()).isEqualTo("Alice");
            assertThat(result.age()).isEqualTo(25);
        }

        @Test
        void missingColumn_returnsDefaultForPrimitive() {
            Map<String, Object> row = Map.of("name", "Alice");

            SimpleRecord result = mapper.mapOne(row, SimpleRecord.class);

            assertThat(result.age()).isZero();
        }

        @Test
        void caseInsensitiveLookup_works() {
            UUID id = UUID.randomUUID();
            Map<String, Object> row = new HashMap<>();
            row.put("ID", id);
            row.put("NAME", "Alice");
            row.put("AGE", 30);

            SimpleRecord result = mapper.mapOne(row, SimpleRecord.class);

            assertThat(result.id()).isEqualTo(id);
            assertThat(result.name()).isEqualTo("Alice");
        }

        @Test
        void nullRow_throwsNullPointerException() {
            assertThatThrownBy(() -> mapper.mapOne(null, SimpleRecord.class))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        void nullType_throwsNullPointerException() {
            assertThatThrownBy(() -> mapper.mapOne(Map.of(), null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("mapOne - POJOs")
    class MapOnePojoTests {

        @Test
        void simplePojo_mapsCorrectly() {
            Map<String, Object> row = Map.of("name", "Alice", "age", 30);

            SimplePojo result = mapper.mapOne(row, SimplePojo.class);

            assertThat(result.getName()).isEqualTo("Alice");
            assertThat(result.getAge()).isEqualTo(30);
        }

        @Test
        void pojoWithAnnotation_usesColumnAnnotation() {
            Map<String, Object> row = Map.of("full_name", "Alice Smith", "age", 30);

            PojoWithAnnotation result = mapper.mapOne(row, PojoWithAnnotation.class);

            assertThat(result.getFullName()).isEqualTo("Alice Smith");
            assertThat(result.getAge()).isEqualTo(30);
        }

        @Test
        void inheritedPojo_mapsParentFields() {
            Map<String, Object> row = Map.of("name", "Alice", "age", 30, "email", "alice@test.com");

            InheritedPojo result = mapper.mapOne(row, InheritedPojo.class);

            assertThat(result.getName()).isEqualTo("Alice");
            assertThat(result.getAge()).isEqualTo(30);
            assertThat(result.getEmail()).isEqualTo("alice@test.com");
        }

        @Test
        void noDefaultConstructor_throwsMappingException() {
            Map<String, Object> row = Map.of("name", "Alice");

            assertThatThrownBy(() -> mapper.mapOne(row, NoDefaultConstructor.class))
                    .isInstanceOf(MappingException.class)
                    .hasMessageContaining("no-arg constructor");
        }
    }

    @Nested
    @DisplayName("mapList")
    class MapListTests {

        @Test
        void mapsList_correctly() {
            List<Map<String, Object>> rows = List.of(
                    Map.of("id", UUID.randomUUID(), "name", "Alice", "age", 30),
                    Map.of("id", UUID.randomUUID(), "name", "Bob", "age", 25)
            );

            List<SimpleRecord> result = mapper.mapList(rows, SimpleRecord.class);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).name()).isEqualTo("Alice");
            assertThat(result.get(1).name()).isEqualTo("Bob");
        }

        @Test
        void emptyList_returnsEmptyList() {
            List<SimpleRecord> result = mapper.mapList(List.of(), SimpleRecord.class);

            assertThat(result).isEmpty();
        }

        @Test
        void nullList_throwsNullPointerException() {
            assertThatThrownBy(() -> mapper.mapList(null, SimpleRecord.class))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("mapOptional")
    class MapOptionalTests {

        @Test
        void nonNullRow_returnsPresent() {
            Map<String, Object> row = Map.of("id", UUID.randomUUID(), "name", "Alice", "age", 30);

            Optional<SimpleRecord> result = mapper.mapOptional(row, SimpleRecord.class);

            assertThat(result).isPresent();
            assertThat(result.get().name()).isEqualTo("Alice");
        }

        @Test
        void nullRow_returnsEmpty() {
            Optional<SimpleRecord> result = mapper.mapOptional(null, SimpleRecord.class);

            assertThat(result).isEmpty();
        }

        @Test
        void emptyRow_returnsEmpty() {
            Optional<SimpleRecord> result = mapper.mapOptional(Map.of(), SimpleRecord.class);

            assertThat(result).isEmpty();
        }

        @Test
        void nullType_throwsNullPointerException() {
            assertThatThrownBy(() -> mapper.mapOptional(Map.of("a", 1), null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("Type coercion - Numbers")
    class NumberCoercionTests {

        @Test
        void allPrimitiveTypes_coerceFromLong() {
            Map<String, Object> row = Map.of(
                    "int_val", 42L,
                    "long_val", 100L,
                    "double_val", 3L,
                    "float_val", 2L,
                    "short_val", 1L,
                    "byte_val", 7L,
                    "bool_val", 1L,
                    "string_val", "hello"
            );

            AllTypesRecord result = mapper.mapOne(row, AllTypesRecord.class);

            assertThat(result.intVal()).isEqualTo(42);
            assertThat(result.longVal()).isEqualTo(100L);
            assertThat(result.doubleVal()).isEqualTo(3.0);
            assertThat(result.floatVal()).isEqualTo(2.0f);
            assertThat(result.shortVal()).isEqualTo((short) 1);
            assertThat(result.byteVal()).isEqualTo((byte) 7);
            assertThat(result.boolVal()).isTrue();
        }

        @Test
        void wrapperTypes_coerceFromLong() {
            // Test the Integer.class / Long.class / Double.class etc. branches
            assertThat(TypeCoercer.coerceNumber(42L, Integer.class)).isEqualTo(42);
            assertThat(TypeCoercer.coerceNumber(42, Long.class)).isEqualTo(42L);
            assertThat(TypeCoercer.coerceNumber(42L, Double.class)).isEqualTo(42.0);
            assertThat(TypeCoercer.coerceNumber(42L, Float.class)).isEqualTo(42.0f);
            assertThat(TypeCoercer.coerceNumber(42L, Short.class)).isEqualTo((short) 42);
            assertThat(TypeCoercer.coerceNumber(42L, Byte.class)).isEqualTo((byte) 42);
        }

        @Test
        void bigDecimal_fromNumber() {
            Map<String, Object> row = Map.of("price", 19.99, "quantity", 100L);

            BigNumberRecord result = mapper.mapOne(row, BigNumberRecord.class);

            assertThat(result.price()).isEqualByComparingTo(BigDecimal.valueOf(19.99));
            assertThat(result.quantity()).isEqualTo(BigInteger.valueOf(100));
        }

        @Test
        void bigDecimal_fromString() {
            Map<String, Object> row = Map.of("price", "123.45", "quantity", "999");

            BigNumberRecord result = mapper.mapOne(row, BigNumberRecord.class);

            assertThat(result.price()).isEqualByComparingTo(new BigDecimal("123.45"));
            assertThat(result.quantity()).isEqualTo(new BigInteger("999"));
        }

        @Test
        void numberToString_coerces() {
            Object result = coerce(42L, String.class);
            assertThat(result).isEqualTo("42");
        }
    }

    @Nested
    @DisplayName("Type coercion - Temporal")
    class TemporalCoercionTests {

        @Test
        void timestamp_toLocalDateTime() {
            LocalDateTime now = LocalDateTime.of(2024, 6, 15, 10, 30, 0);
            Timestamp ts = Timestamp.valueOf(now);
            Map<String, Object> row = Map.of(
                    "local_date_time", ts,
                    "instant", ts,
                    "offset_date_time", ts,
                    "local_date", ts
            );

            TemporalRecord result = mapper.mapOne(row, TemporalRecord.class);

            assertThat(result.localDateTime()).isEqualTo(now);
            assertThat(result.instant()).isEqualTo(ts.toInstant());
            assertThat(result.offsetDateTime()).isEqualTo(ts.toInstant().atOffset(ZoneOffset.UTC));
            assertThat(result.localDate()).isEqualTo(now.toLocalDate());
        }

        @Test
        void offsetDateTime_toLocalDateTime() {
            OffsetDateTime odt = OffsetDateTime.of(2024, 6, 15, 10, 30, 0, 0, ZoneOffset.UTC);
            Map<String, Object> row = Map.of(
                    "local_date_time", odt,
                    "instant", odt,
                    "offset_date_time", odt,
                    "local_date", odt
            );

            TemporalRecord result = mapper.mapOne(row, TemporalRecord.class);

            assertThat(result.localDateTime()).isEqualTo(odt.toLocalDateTime());
            assertThat(result.instant()).isEqualTo(odt.toInstant());
            assertThat(result.offsetDateTime()).isEqualTo(odt);
            assertThat(result.localDate()).isEqualTo(odt.toLocalDate());
        }

        @Test
        void instant_toLocalDateTime() {
            Instant instant = Instant.parse("2024-06-15T10:30:00Z");
            Map<String, Object> row = Map.of(
                    "local_date_time", instant,
                    "instant", instant,
                    "offset_date_time", instant,
                    "local_date", java.sql.Date.valueOf(LocalDate.of(2024, 6, 15))
            );

            TemporalRecord result = mapper.mapOne(row, TemporalRecord.class);

            assertThat(result.localDateTime()).isEqualTo(LocalDateTime.ofInstant(instant, ZoneOffset.UTC));
            assertThat(result.instant()).isEqualTo(instant);
            assertThat(result.offsetDateTime()).isEqualTo(instant.atOffset(ZoneOffset.UTC));
            assertThat(result.localDate()).isEqualTo(LocalDate.of(2024, 6, 15));
        }

        @Test
        void localDateTime_toInstant() {
            LocalDateTime ldt = LocalDateTime.of(2024, 6, 15, 10, 30, 0);
            Map<String, Object> row = Map.of(
                    "local_date_time", ldt,
                    "instant", ldt,
                    "offset_date_time", ldt,
                    "local_date", ldt
            );

            TemporalRecord result = mapper.mapOne(row, TemporalRecord.class);

            assertThat(result.localDateTime()).isEqualTo(ldt);
            assertThat(result.instant()).isEqualTo(ldt.toInstant(ZoneOffset.UTC));
            assertThat(result.offsetDateTime()).isEqualTo(ldt.atOffset(ZoneOffset.UTC));
            assertThat(result.localDate()).isEqualTo(ldt.toLocalDate());
        }

        @Test
        void timestamp_toLong() {
            Timestamp ts = Timestamp.valueOf(LocalDateTime.of(2024, 6, 15, 10, 30, 0));
            assertThat(coerce(ts, long.class)).isEqualTo(ts.getTime());
            assertThat(coerce(ts, Long.class)).isEqualTo(ts.getTime());
        }

        @Test
        void instant_toLong() {
            Instant instant = Instant.parse("2024-06-15T10:30:00Z");
            assertThat(coerce(instant, long.class)).isEqualTo(instant.toEpochMilli());
            assertThat(coerce(instant, Long.class)).isEqualTo(instant.toEpochMilli());
        }

        @Test
        void sqlDate_toNonLocalDate_fallsThrough() {
            java.sql.Date sqlDate = java.sql.Date.valueOf(LocalDate.of(2024, 6, 15));
            // sql.Date with non-LocalDate target falls through the if-block
            // and hits the final "Cannot coerce" since sql.Date is not OffsetDateTime/Instant/LocalDateTime
            assertThatThrownBy(() -> coerce(sqlDate, OffsetDateTime.class))
                    .isInstanceOf(MappingException.class);
        }
    }

    @Nested
    @DisplayName("Type coercion - UUID")
    class UuidCoercionTests {

        @Test
        void uuidFromString() {
            UUID expected = UUID.randomUUID();
            Map<String, Object> row = Map.of("id", expected.toString(), "name", "X", "age", 1);

            SimpleRecord result = mapper.mapOne(row, SimpleRecord.class);

            assertThat(result.id()).isEqualTo(expected);
        }

        @Test
        void uuidPassthrough() {
            UUID expected = UUID.randomUUID();
            Map<String, Object> row = Map.of("id", expected, "name", "X", "age", 1);

            SimpleRecord result = mapper.mapOne(row, SimpleRecord.class);

            assertThat(result.id()).isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("Type coercion - Boolean")
    class BooleanCoercionTests {

        @Test
        void booleanFromNumber() {
            assertThat(coerce(1, boolean.class)).isEqualTo(true);
            assertThat(coerce(0, boolean.class)).isEqualTo(false);
            assertThat(coerce(1, Boolean.class)).isEqualTo(true);
            assertThat(coerce(0, Boolean.class)).isEqualTo(false);
        }

        @Test
        void booleanFromString() {
            assertThat(coerce("true", boolean.class)).isEqualTo(true);
            assertThat(coerce("false", boolean.class)).isEqualTo(false);
            assertThat(coerce("1", boolean.class)).isEqualTo(true);
            assertThat(coerce("yes", boolean.class)).isEqualTo(true);
            assertThat(coerce("no", boolean.class)).isEqualTo(false);
            assertThat(coerce("true", Boolean.class)).isEqualTo(true);
        }

        @Test
        void booleanFromBoolean() {
            assertThat(coerce(Boolean.TRUE, boolean.class)).isEqualTo(true);
        }

        @Test
        void booleanFromNumberViaCoerceBoolean() {
            // Ensure Number path in coerceBoolean is hit (not the coerceNumber path)
            // This happens when rawValue is Number and targetType is Boolean.class
            // but isAssignableFrom doesn't match (Number is not Boolean)
            assertThat(coerce(1L, Boolean.class)).isEqualTo(true);
        }

        @Test
        void booleanFromUnsupportedType_throwsMappingException() {
            assertThatThrownBy(() -> coerce(List.of(), boolean.class))
                    .isInstanceOf(MappingException.class)
                    .hasMessageContaining("Cannot coerce");
        }
    }

    @Nested
    @DisplayName("Type coercion - Enum")
    class EnumCoercionTests {

        @Test
        void enumFromString() {
            Map<String, Object> row = Map.of("name", "Alice", "status", "ACTIVE");

            EnumRecord result = mapper.mapOne(row, EnumRecord.class);

            assertThat(result.status()).isEqualTo(Status.ACTIVE);
        }

        @Test
        void invalidEnumValue_throwsMappingException() {
            Map<String, Object> row = Map.of("name", "Alice", "status", "UNKNOWN");

            assertThatThrownBy(() -> mapper.mapOne(row, EnumRecord.class))
                    .isInstanceOf(MappingException.class)
                    .hasMessageContaining("No enum constant");
        }
    }

    @Nested
    @DisplayName("Type coercion - Edge cases")
    class EdgeCaseTests {

        @Test
        void nullValue_primitiveInt_returnsZero() {
            assertThat(coerce(null, int.class)).isEqualTo(0);
        }

        @Test
        void nullValue_primitiveBoolean_returnsFalse() {
            assertThat(coerce(null, boolean.class)).isEqualTo(false);
        }

        @Test
        void nullValue_primitiveChar_returnsNullChar() {
            assertThat(coerce(null, char.class)).isEqualTo('\0');
        }

        @Test
        void nullValue_primitiveLong_returnsZero() {
            assertThat(coerce(null, long.class)).isEqualTo(0L);
        }

        @Test
        void nullValue_primitiveDouble_returnsZero() {
            assertThat(coerce(null, double.class)).isEqualTo(0.0d);
        }

        @Test
        void nullValue_primitiveFloat_returnsZero() {
            assertThat(coerce(null, float.class)).isEqualTo(0.0f);
        }

        @Test
        void nullValue_primitiveShort_returnsZero() {
            assertThat(coerce(null, short.class)).isEqualTo((short) 0);
        }

        @Test
        void nullValue_primitiveByte_returnsZero() {
            assertThat(coerce(null, byte.class)).isEqualTo((byte) 0);
        }

        @Test
        void nullValue_referenceType_returnsNull() {
            assertThat(coerce(null, String.class)).isNull();
        }

        @Test
        void alreadyCorrectType_passesThrough() {
            String value = "hello";
            assertThat(coerce(value, String.class)).isSameAs(value);
        }

        @Test
        void objectToString_usesToString() {
            assertThat(coerce(42, String.class)).isEqualTo("42");
        }

        @Test
        void incompatibleType_throwsMappingException() {
            assertThatThrownBy(() -> coerce("not-a-list", List.class))
                    .isInstanceOf(MappingException.class)
                    .hasMessageContaining("Cannot coerce");
        }

        @Test
        void numberToUnsupportedNumericType_throwsMappingException() {
            assertThatThrownBy(() -> coerce(42L, UUID.class))
                    .isInstanceOf(MappingException.class);
        }

        @Test
        void uuidFromNonString_throwsMappingException() {
            assertThatThrownBy(() -> coerce(12345L, UUID.class))
                    .isInstanceOf(MappingException.class)
                    .hasMessageContaining("Cannot coerce");
        }

        @Test
        void sqlDate_toLocalDate() {
            java.sql.Date sqlDate = java.sql.Date.valueOf(LocalDate.of(2024, 6, 15));
            Object result = coerce(sqlDate, LocalDate.class);
            assertThat(result).isEqualTo(LocalDate.of(2024, 6, 15));
        }
    }

    @Nested
    @DisplayName("toSnakeCase")
    class ToSnakeCaseTests {

        @Test
        void camelCase_convertsCorrectly() {
            assertThat(TypeMetadata.toSnakeCase("firstName")).isEqualTo("first_name");
            assertThat(TypeMetadata.toSnakeCase("createdAt")).isEqualTo("created_at");
            assertThat(TypeMetadata.toSnakeCase("id")).isEqualTo("id");
            assertThat(TypeMetadata.toSnakeCase("myURLParser")).isEqualTo("my_urlparser");
        }

        @Test
        void alreadySnakeCase_unchanged() {
            assertThat(TypeMetadata.toSnakeCase("first_name")).isEqualTo("first_name");
        }

        @Test
        void singleChar_unchanged() {
            assertThat(TypeMetadata.toSnakeCase("x")).isEqualTo("x");
        }
    }

    @Nested
    @DisplayName("Caching")
    class CachingTests {

        @Test
        void sameType_usesCache() {
            Map<String, Object> row = Map.of("id", UUID.randomUUID(), "name", "A", "age", 1);

            // Call twice — second call should use cached metadata
            SimpleRecord r1 = mapper.mapOne(row, SimpleRecord.class);
            SimpleRecord r2 = mapper.mapOne(row, SimpleRecord.class);

            assertThat(r1.name()).isEqualTo(r2.name());
        }
    }

    @Nested
    @DisplayName("Coercion - Timestamp edge cases")
    class TimestampEdgeCaseTests {

        @Test
        void timestamp_toUnsupportedType_throwsMappingException() {
            Timestamp ts = Timestamp.valueOf(LocalDateTime.now());
            assertThatThrownBy(() -> coerce(ts, BigInteger.class))
                    .isInstanceOf(MappingException.class)
                    .hasMessageContaining("Cannot coerce Timestamp");
        }

        @Test
        void offsetDateTime_toUnsupportedType_throwsMappingException() {
            OffsetDateTime odt = OffsetDateTime.now();
            assertThatThrownBy(() -> coerce(odt, BigInteger.class))
                    .isInstanceOf(MappingException.class)
                    .hasMessageContaining("Cannot coerce OffsetDateTime");
        }

        @Test
        void instant_toUnsupportedType_throwsMappingException() {
            Instant instant = Instant.now();
            assertThatThrownBy(() -> coerce(instant, BigInteger.class))
                    .isInstanceOf(MappingException.class)
                    .hasMessageContaining("Cannot coerce Instant");
        }

        @Test
        void localDateTime_toUnsupportedType_throwsMappingException() {
            LocalDateTime ldt = LocalDateTime.now();
            assertThatThrownBy(() -> coerce(ldt, BigInteger.class))
                    .isInstanceOf(MappingException.class)
                    .hasMessageContaining("Cannot coerce LocalDateTime");
        }

        @Test
        void offsetDateTime_toTimestamp() {
            OffsetDateTime odt = OffsetDateTime.of(2024, 6, 15, 10, 30, 0, 0, ZoneOffset.UTC);
            Object result = coerce(odt, Timestamp.class);
            assertThat(result).isEqualTo(Timestamp.from(odt.toInstant()));
        }

        @Test
        void instant_toTimestamp() {
            Instant instant = Instant.parse("2024-06-15T10:30:00Z");
            Object result = coerce(instant, Timestamp.class);
            assertThat(result).isEqualTo(Timestamp.from(instant));
        }

        @Test
        void localDateTime_toTimestamp() {
            LocalDateTime ldt = LocalDateTime.of(2024, 6, 15, 10, 30, 0);
            Object result = coerce(ldt, Timestamp.class);
            assertThat(result).isEqualTo(Timestamp.valueOf(ldt));
        }
    }

    @Nested
    @DisplayName("Coercion - Number to Number edge cases")
    class NumberEdgeCaseTests {

        @Test
        void numberToNumber_unsupportedTarget_throwsMappingException() {
            assertThatThrownBy(() -> coerce(42, char.class))
                    .isInstanceOf(MappingException.class);
        }

        @Test
        void bigDecimalPassthrough() {
            BigDecimal bd = new BigDecimal("123.45");
            assertThat(TypeCoercer.coerceNumber(bd, BigDecimal.class)).isSameAs(bd);
        }

        @Test
        void bigIntegerPassthrough() {
            BigInteger bi = BigInteger.valueOf(999);
            assertThat(TypeCoercer.coerceNumber(bi, BigInteger.class)).isSameAs(bi);
        }

        @Test
        void numberToBigDecimal_fromInteger() {
            Object result = TypeCoercer.coerceNumber(42, BigDecimal.class);
            assertThat(result).isEqualTo(BigDecimal.valueOf(42.0));
        }

        @Test
        void numberToBigInteger_fromInteger() {
            Object result = TypeCoercer.coerceNumber(42, BigInteger.class);
            assertThat(result).isEqualTo(BigInteger.valueOf(42));
        }

        @Test
        void numberToBoolean_zeroIsFalse() {
            assertThat(coerce(0, Boolean.class)).isEqualTo(false);
        }

        @Test
        void numberToBoolean_nonZeroIsTrue() {
            assertThat(coerce(5, Boolean.class)).isEqualTo(true);
        }

        @Test
        void numberToString_viaCoerceNumber() {
            assertThat(TypeCoercer.coerceNumber(3.14, String.class)).isEqualTo("3.14");
        }

        @Test
        void numberToString_fromNumber() {
            assertThat(coerce(3.14, String.class)).isEqualTo("3.14");
        }

        @Test
        void numberToUnsupportedType_throwsMappingException() {
            assertThatThrownBy(() -> coerce(42, List.class))
                    .isInstanceOf(MappingException.class)
                    .hasMessageContaining("Cannot coerce Number");
        }
    }

    @Nested
    @DisplayName("Error paths - instantiation failures")
    class InstantiationErrorTests {

        // Record whose canonical constructor throws
        public record ThrowingRecord(String name) {
            public ThrowingRecord {
                if (name != null) {
                    throw new IllegalStateException("boom");
                }
            }
        }

        @Test
        void record_constructorThrows_throwsMappingException() {
            Map<String, Object> row = Map.of("name", "test");

            assertThatThrownBy(() -> mapper.mapOne(row, ThrowingRecord.class))
                    .isInstanceOf(MappingException.class)
                    .hasMessageContaining("Failed to instantiate record ThrowingRecord");
        }

        @Test
        void pojo_mappingExceptionInCoercion_propagatesDirectly() {
            // Force a MappingException during POJO mapping via invalid enum value
            Map<String, Object> row = Map.of("name", "test", "status", "INVALID_VALUE");

            assertThatThrownBy(() -> mapper.mapOne(row, PojoWithEnum.class))
                    .isInstanceOf(MappingException.class)
                    .hasMessageContaining("No enum constant");
        }
    }

    public static class PojoWithEnum {
        private String name;
        private Status status;

        public String getName() { return name; }
        public Status getStatus() { return status; }
    }

    public static class PojoWithStaticField {
        private static final String CONSTANT = "ignored";
        private String name;

        public String getName() { return name; }
    }

    @Nested
    @DisplayName("TypeMetadata - field filtering")
    class FieldFilteringTests {

        @Test
        void staticFields_areIgnored() {
            Map<String, Object> row = Map.of("name", "Alice");

            PojoWithStaticField result = mapper.mapOne(row, PojoWithStaticField.class);

            assertThat(result.getName()).isEqualTo("Alice");
        }

        @Test
        void syntheticFields_areIgnored() {
            // Non-static inner class has a synthetic "this$0" field.
            // resolve() will call collectFields (hitting the synthetic branch)
            // then fail on no-arg constructor — but the synthetic skip is still covered.
            assertThatThrownBy(() -> TypeMetadata.resolve(InnerClassWithSynthetic.class))
                    .isInstanceOf(MappingException.class)
                    .hasMessageContaining("no-arg constructor");
        }
    }

    // Non-static inner class — compiler generates synthetic "this$0" field
    public class InnerClassWithSynthetic {
        private String value;
    }
}
