package com.githubaccess.report.constant;

public final class AppConstants {

    private AppConstants() {
    }

    public static final int DEFAULT_MAX_CONCURRENT_REQUESTS = 15;
    public static final int DEFAULT_PER_PAGE = 100;
    public static final int DEFAULT_RETRY_MAX_ATTEMPTS = 3;
    public static final long DEFAULT_RETRY_INITIAL_DELAY_MS = 500L;
    public static final double DEFAULT_RETRY_MULTIPLIER = 2.0;
    public static final int DEFAULT_CONNECT_TIMEOUT_MS = 5000;
    public static final int DEFAULT_READ_TIMEOUT_MS = 30000;
}
