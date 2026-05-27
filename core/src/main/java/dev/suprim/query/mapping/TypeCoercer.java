package dev.suprim.query.mapping;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Converts raw DB values to target Java types.
 *
 * <p>Handles common coercions: Number widening/narrowing, temporal conversions,
 * UUID from String, Boolean from various representations, Enum from name, and
 * BigDecimal/BigInteger from String or Number.
 *
 * <p>Returns primitive defaults (0, false, '\0') when raw value is null and
 * target is a primitive type. Returns null for reference types.
 */
final class TypeCoercer {

    private TypeCoercer() {
        // utility class
    }

    /**
     * Coerces a raw DB value to the target Java type.
     *
     * @param rawValue   value from the result map (may be null)
     * @param targetType desired Java type
     * @return coerced value, or null/primitive-default if rawValue is null
     * @throws MappingException if coercion is not possible
     */
    @SuppressWarnings("unchecked")
    static Object coerce(Object rawValue, Class<?> targetType) {
        if (rawValue == null) {
            return defaultForPrimitive(targetType);
        }

        // Already correct type
        if (targetType.isAssignableFrom(rawValue.getClass())) {
            return rawValue;
        }

        // String target — anything can toString()
        if (targetType == String.class) {
            return rawValue.toString();
        }

        // UUID — only from String
        if (targetType == UUID.class) {
            if (rawValue instanceof String s) {
                return UUID.fromString(s);
            }
            throw new MappingException(
                    "Cannot coerce " + rawValue.getClass().getSimpleName() + " to UUID"
            );
        }

        // Numeric coercions
        if (rawValue instanceof Number number) {
            return coerceNumber(number, targetType);
        }

        // Temporal coercions
        if (rawValue instanceof Timestamp timestamp) {
            return coerceTimestamp(timestamp, targetType);
        }
        if (rawValue instanceof java.sql.Date sqlDate) {
            if (targetType == LocalDate.class) {
                return sqlDate.toLocalDate();
            }
        }
        if (rawValue instanceof OffsetDateTime odt) {
            return coerceOffsetDateTime(odt, targetType);
        }
        if (rawValue instanceof Instant instant) {
            return coerceInstant(instant, targetType);
        }
        if (rawValue instanceof LocalDateTime ldt) {
            return coerceLocalDateTime(ldt, targetType);
        }

        // Boolean
        if (targetType == boolean.class || targetType == Boolean.class) {
            return coerceBoolean(rawValue);
        }

        // Enum
        if (targetType.isEnum()) {
            return coerceEnum(rawValue, targetType);
        }

        // BigDecimal / BigInteger from String
        if (targetType == BigDecimal.class) {
            return new BigDecimal(rawValue.toString());
        }
        if (targetType == BigInteger.class) {
            return new BigInteger(rawValue.toString());
        }

        throw new MappingException(
                "Cannot coerce " + rawValue.getClass().getSimpleName()
                        + " to " + targetType.getSimpleName()
        );
    }

    // --- Number ---

    private static Object coerceNumber(Number number, Class<?> targetType) {
        if (targetType == int.class || targetType == Integer.class) {
            return number.intValue();
        }
        if (targetType == long.class || targetType == Long.class) {
            return number.longValue();
        }
        if (targetType == double.class || targetType == Double.class) {
            return number.doubleValue();
        }
        if (targetType == float.class || targetType == Float.class) {
            return number.floatValue();
        }
        if (targetType == short.class || targetType == Short.class) {
            return number.shortValue();
        }
        if (targetType == byte.class || targetType == Byte.class) {
            return number.byteValue();
        }
        if (targetType == BigDecimal.class) {
            if (number instanceof BigDecimal) {
                return number;
            }
            return BigDecimal.valueOf(number.doubleValue());
        }
        if (targetType == BigInteger.class) {
            if (number instanceof BigInteger) {
                return number;
            }
            return BigInteger.valueOf(number.longValue());
        }
        if (targetType == boolean.class || targetType == Boolean.class) {
            return number.intValue() != 0;
        }
        if (targetType == String.class) {
            return number.toString();
        }
        throw new MappingException(
                "Cannot coerce Number to " + targetType.getSimpleName()
        );
    }

    // --- Temporal ---

    private static Object coerceTimestamp(Timestamp timestamp, Class<?> targetType) {
        if (targetType == LocalDateTime.class) {
            return timestamp.toLocalDateTime();
        }
        if (targetType == Instant.class) {
            return timestamp.toInstant();
        }
        if (targetType == OffsetDateTime.class) {
            return timestamp.toInstant().atOffset(ZoneOffset.UTC);
        }
        if (targetType == LocalDate.class) {
            return timestamp.toLocalDateTime().toLocalDate();
        }
        if (targetType == long.class || targetType == Long.class) {
            return timestamp.getTime();
        }
        throw new MappingException(
                "Cannot coerce Timestamp to " + targetType.getSimpleName()
        );
    }

    private static Object coerceOffsetDateTime(OffsetDateTime odt, Class<?> targetType) {
        if (targetType == LocalDateTime.class) {
            return odt.toLocalDateTime();
        }
        if (targetType == Instant.class) {
            return odt.toInstant();
        }
        if (targetType == LocalDate.class) {
            return odt.toLocalDate();
        }
        if (targetType == Timestamp.class) {
            return Timestamp.from(odt.toInstant());
        }
        throw new MappingException(
                "Cannot coerce OffsetDateTime to " + targetType.getSimpleName()
        );
    }

    private static Object coerceInstant(Instant instant, Class<?> targetType) {
        if (targetType == LocalDateTime.class) {
            return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
        }
        if (targetType == OffsetDateTime.class) {
            return instant.atOffset(ZoneOffset.UTC);
        }
        if (targetType == Timestamp.class) {
            return Timestamp.from(instant);
        }
        if (targetType == long.class || targetType == Long.class) {
            return instant.toEpochMilli();
        }
        throw new MappingException(
                "Cannot coerce Instant to " + targetType.getSimpleName()
        );
    }

    private static Object coerceLocalDateTime(LocalDateTime ldt, Class<?> targetType) {
        if (targetType == Instant.class) {
            return ldt.toInstant(ZoneOffset.UTC);
        }
        if (targetType == OffsetDateTime.class) {
            return ldt.atOffset(ZoneOffset.UTC);
        }
        if (targetType == Timestamp.class) {
            return Timestamp.valueOf(ldt);
        }
        if (targetType == LocalDate.class) {
            return ldt.toLocalDate();
        }
        throw new MappingException(
                "Cannot coerce LocalDateTime to " + targetType.getSimpleName()
        );
    }

    // --- Boolean ---

    private static Object coerceBoolean(Object rawValue) {
        if (rawValue instanceof Boolean) {
            return rawValue;
        }
        if (rawValue instanceof Number number) {
            return number.intValue() != 0;
        }
        if (rawValue instanceof String s) {
            return "true".equalsIgnoreCase(s) || "1".equals(s) || "yes".equalsIgnoreCase(s);
        }
        throw new MappingException(
                "Cannot coerce " + rawValue.getClass().getSimpleName() + " to boolean"
        );
    }

    // --- Enum ---

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object coerceEnum(Object rawValue, Class<?> targetType) {
        String name = rawValue.toString();
        try {
            return Enum.valueOf((Class<Enum>) targetType, name);
        } catch (IllegalArgumentException e) {
            throw new MappingException(
                    "No enum constant " + targetType.getSimpleName() + "." + name, e
            );
        }
    }

    // --- Primitive defaults ---

    private static Object defaultForPrimitive(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (type == boolean.class) return false;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == double.class) return 0.0d;
        if (type == float.class) return 0.0f;
        if (type == short.class) return (short) 0;
        if (type == byte.class) return (byte) 0;
        // char.class is the only remaining primitive
        return '\0';
    }
}
