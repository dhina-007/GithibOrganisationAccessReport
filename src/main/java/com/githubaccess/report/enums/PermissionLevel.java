package com.githubaccess.report.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

public enum PermissionLevel {

    ADMIN("admin"),
    MAINTAIN("maintain"),
    WRITE("write"),
    TRIAGE("triage"),
    READ("read"),
    UNKNOWN("unknown");

    private final String value;

    PermissionLevel(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static PermissionLevel fromValue(String raw) {
        if (raw == null || raw.isBlank()) {
            return UNKNOWN;
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        for (PermissionLevel level : values()) {
            if (level.value.equals(normalized)) {
                return level;
            }
        }
        return UNKNOWN;
    }
}
