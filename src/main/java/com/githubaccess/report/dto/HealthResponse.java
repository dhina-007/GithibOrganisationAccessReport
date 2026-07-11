package com.githubaccess.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Application health status")
public record HealthResponse(
        @Schema(description = "Health status", example = "UP")
        String status,
        @Schema(description = "Application name", example = "github-access-report")
        String application
) {
}
