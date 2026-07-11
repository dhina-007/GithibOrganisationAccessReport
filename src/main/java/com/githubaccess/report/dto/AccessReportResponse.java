package com.githubaccess.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "Aggregated organization access report mapping users to repositories")
public record AccessReportResponse(
        @Schema(description = "GitHub organization login", example = "dhina-project")
        String organization,
        @Schema(description = "When the report was generated")
        Instant generatedAt,
        @Schema(description = "Number of distinct users with repository access")
        int totalUsers,
        @Schema(description = "Number of repositories scanned")
        int totalRepositories,
        @Schema(description = "Users and their repository access")
        List<UserAccessDto> users
) {
}
