package com.bookease.common;

import org.slf4j.MDC;

public final class CorrelationId {

    public static final String MDC_KEY = "correlationId";
    public static final String HEADER = "X-Correlation-Id";

    private CorrelationId() {
    }

    public static String current() {
        return MDC.get(MDC_KEY);
    }
}
