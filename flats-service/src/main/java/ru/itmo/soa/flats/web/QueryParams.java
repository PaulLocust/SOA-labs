package ru.itmo.soa.flats.web;

import ru.itmo.soa.flats.exception.ApiException;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Разбор параметров запроса (query string).
 * Все ошибки синтаксиса (параметр не передан, неверный тип или формат) приводят к ответу 400.
 */
public final class QueryParams {

    private static final Pattern INTEGER = Pattern.compile("[+-]?\\d+");
    private static final Pattern DECIMAL = Pattern.compile("[+-]?(\\d+(\\.\\d*)?|\\.\\d+)([eE][+-]?\\d+)?");

    private final Map<String, String[]> params;

    public QueryParams(Map<String, String[]> params) {
        this.params = params;
    }

    public boolean has(String name) {
        String[] values = params.get(name);
        return values != null && values.length > 0;
    }

    public boolean hasAny(String... names) {
        return Arrays.stream(names).anyMatch(this::has);
    }

    /** Все значения многозначного параметра (например, {@code sort}). */
    public List<String> all(String name) {
        String[] values = params.get(name);
        return values == null ? List.of() : List.of(values);
    }

    /** Значение однозначного параметра или {@code null}, если параметр не передан. */
    public String string(String name) {
        String[] values = params.get(name);
        if (values == null || values.length == 0) {
            return null;
        }
        if (values.length > 1) {
            throw ApiException.badRequest("Parameter '" + name + "' must be specified only once");
        }
        return values[0];
    }

    public String requiredString(String name) {
        return require(name, string(name));
    }

    public Long longValue(String name) {
        String raw = string(name);
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        if (INTEGER.matcher(value).matches()) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException ignored) {
                // выход за границы long - тоже ошибка формата
            }
        }
        throw ApiException.badRequest("Parameter '" + name + "' must be an integer");
    }

    public Long requiredLong(String name) {
        return require(name, longValue(name));
    }

    public Integer intValue(String name) {
        Long value = longValue(name);
        if (value == null) {
            return null;
        }
        if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
            throw ApiException.badRequest("Parameter '" + name + "' must be a 32-bit integer");
        }
        return value.intValue();
    }

    public Integer requiredInt(String name) {
        return require(name, intValue(name));
    }

    public Double doubleValue(String name) {
        String raw = string(name);
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        if (DECIMAL.matcher(value).matches()) {
            double parsed = Double.parseDouble(value);
            if (Double.isFinite(parsed)) {
                return parsed;
            }
        }
        throw ApiException.badRequest("Parameter '" + name + "' must be a number");
    }

    public Double requiredDouble(String name) {
        return require(name, doubleValue(name));
    }

    public Float floatValue(String name) {
        String raw = string(name);
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        if (DECIMAL.matcher(value).matches()) {
            float parsed = Float.parseFloat(value);
            if (Float.isFinite(parsed)) {
                return parsed;
            }
        }
        throw ApiException.badRequest("Parameter '" + name + "' must be a number");
    }

    public Float requiredFloat(String name) {
        return require(name, floatValue(name));
    }

    public Boolean booleanValue(String name) {
        String raw = string(name);
        if (raw == null) {
            return null;
        }
        return parseBoolean(name, raw);
    }

    public Boolean requiredBoolean(String name) {
        return require(name, booleanValue(name));
    }

    public <E extends Enum<E>> E enumValue(String name, Class<E> type) {
        String raw = string(name);
        if (raw == null) {
            return null;
        }
        return parseEnum(name, raw, type);
    }

    public <E extends Enum<E>> E requiredEnum(String name, Class<E> type) {
        return require(name, enumValue(name, type));
    }

    /**
     * Дата-время в формате ISO 8601. Если смещение не указано, время считается в UTC.
     */
    public Date dateTimeValue(String name) {
        String raw = string(name);
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        try {
            return Date.from(OffsetDateTime.parse(value).toInstant());
        } catch (DateTimeParseException e) {
            try {
                return Date.from(LocalDateTime.parse(value).toInstant(ZoneOffset.UTC));
            } catch (DateTimeParseException ignored) {
                throw ApiException.badRequest("Parameter '" + name
                        + "' must be a date-time in ISO 8601 format, e.g. 2026-09-25T12:00:00Z");
            }
        }
    }

    public static boolean parseBoolean(String name, String raw) {
        String value = raw.trim();
        if ("true".equalsIgnoreCase(value)) {
            return true;
        }
        if ("false".equalsIgnoreCase(value)) {
            return false;
        }
        throw ApiException.badRequest("Parameter '" + name + "' must be a boolean (true or false)");
    }

    public static <E extends Enum<E>> E parseEnum(String name, String raw, Class<E> type) {
        String value = raw.trim();
        for (E constant : type.getEnumConstants()) {
            if (constant.name().equals(value)) {
                return constant;
            }
        }
        String allowed = Arrays.stream(type.getEnumConstants()).map(Enum::name).collect(Collectors.joining(", "));
        throw ApiException.badRequest("Parameter '" + name + "' must be one of: " + allowed);
    }

    public static long parsePathId(String name, String raw) {
        String value = raw.trim();
        if (INTEGER.matcher(value).matches()) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException ignored) {
                // выход за границы long
            }
        }
        throw ApiException.badRequest("Parameter '" + name + "' must be an integer");
    }

    private static <T> T require(String name, T value) {
        if (value == null) {
            throw ApiException.badRequest("Required parameter '" + name + "' is missing");
        }
        return value;
    }
}
