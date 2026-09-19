package com.bookease.common.validation;

import java.util.Locale;

public final class NameNormalizer {

    private NameNormalizer() {
    }

    public static String display(String raw) {
        return raw == null ? null : raw.trim().replaceAll("\\s+", " ");
    }

    public static String key(String raw) {
        String display = display(raw);
        return display == null ? null : display.toLowerCase(Locale.ROOT);
    }
}
