package com.parca.yedekparca.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Set;

/** Validates current day objects and the legacy day: "HH:mm-HH:mm" JSON format. */
public final class WorkingHoursValidator {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Set<String> DAYS = Set.of("monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday",
            "pazartesi", "salı", "sali", "çarşamba", "carsamba", "perşembe", "persembe", "cuma", "cumartesi", "pazar");
    private WorkingHoursValidator() { }

    public static void validate(String value) {
        if (value == null || value.isBlank()) return;
        try {
            JsonNode root = JSON.readTree(value);
            if (root == null || !root.isObject()) throw invalid();
            var fields = root.fields();
            while (fields.hasNext()) {
                var entry = fields.next();
                if (!DAYS.contains(entry.getKey().toLowerCase(Locale.ROOT))) throw invalid();
                JsonNode day = entry.getValue();
                if (day.isTextual()) { validateRange(day.asText()); continue; }
                if (!day.isObject() || !day.path("closed").isBoolean()) throw invalid();
                if (day.path("closed").asBoolean()) continue;
                if (!day.path("open").isTextual() || !day.path("close").isTextual()) throw invalid();
                validateTimes(day.path("open").asText(), day.path("close").asText());
            }
        } catch (DateTimeParseException | java.io.IOException ex) {
            throw invalid();
        }
    }

    private static void validateRange(String value) {
        String[] times = value.split("-", -1);
        if (times.length != 2) throw invalid();
        validateTimes(times[0].trim(), times[1].trim());
    }

    private static void validateTimes(String open, String close) {
        try {
            LocalTime start = LocalTime.parse(open), end = LocalTime.parse(close);
            if (!start.isBefore(end)) throw invalid();
        } catch (DateTimeParseException ex) { throw invalid(); }
    }

    private static IllegalArgumentException invalid() {
        return new IllegalArgumentException("Çalışma saatleri geçersiz: haftanın günlerini, HH:mm saatlerini ve açılışın kapanıştan önce olduğunu kontrol edin.");
    }
}
