package ru.itmo.soa.flats.model;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * Дата в XML передаётся в формате date-time (ISO 8601, UTC), например {@code 2026-09-25T12:00:00Z}.
 */
public class DateAdapter extends XmlAdapter<String, Date> {

    public static String format(Date date) {
        return DateTimeFormatter.ISO_INSTANT.format(date.toInstant().truncatedTo(ChronoUnit.MILLIS));
    }

    @Override
    public Date unmarshal(String value) {
        return value == null ? null : Date.from(Instant.parse(value.trim()));
    }

    @Override
    public String marshal(Date value) {
        return value == null ? null : format(value);
    }
}
