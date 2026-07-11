package com.githubaccess.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "User and the repositories they can access")
public record UserAccessDto(
        @Schema(description = "GitHub login", example = "alice")
        String login,
        @Schema(description = "Repositories this user can access")
        List<RepositoryAccessDto> repositories
) {
}
