package com.githubaccess.report.config;

import com.githubaccess.report.constant.AppConstants;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "github")
public class GitHubProperties {

    @NotBlank
    private String baseUrl = "https://api.github.com";

    private String token = "";

    @Positive
    private int connectTimeoutMs = AppConstants.DEFAULT_CONNECT_TIMEOUT_MS;

    @Positive
    private int readTimeoutMs = AppConstants.DEFAULT_READ_TIMEOUT_MS;

    @Min(1)
    private int maxConcurrentRequests = AppConstants.DEFAULT_MAX_CONCURRENT_REQUESTS;

    @Min(1)
    private int perPage = AppConstants.DEFAULT_PER_PAGE;

    @Valid
    private Retry retry = new Retry();

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(int connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public int getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(int readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }

    public int getMaxConcurrentRequests() {
        return maxConcurrentRequests;
    }

    public void setMaxConcurrentRequests(int maxConcurrentRequests) {
        this.maxConcurrentRequests = maxConcurrentRequests;
    }

    public int getPerPage() {
        return perPage;
    }

    public void setPerPage(int perPage) {
        this.perPage = perPage;
    }

    public Retry getRetry() {
        return retry;
    }

    public void setRetry(Retry retry) {
        this.retry = retry;
    }

    public static class Retry {

        @Min(1)
        private int maxAttempts = AppConstants.DEFAULT_RETRY_MAX_ATTEMPTS;

        @Positive
        private long initialDelayMs = AppConstants.DEFAULT_RETRY_INITIAL_DELAY_MS;

        @Positive
        private double multiplier = AppConstants.DEFAULT_RETRY_MULTIPLIER;

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public long getInitialDelayMs() {
            return initialDelayMs;
        }

        public void setInitialDelayMs(long initialDelayMs) {
            this.initialDelayMs = initialDelayMs;
        }

        public double getMultiplier() {
            return multiplier;
        }

        public void setMultiplier(double multiplier) {
            this.multiplier = multiplier;
        }
    }
}
