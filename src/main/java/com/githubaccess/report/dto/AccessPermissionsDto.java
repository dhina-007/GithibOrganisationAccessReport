package com.githubaccess.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Detailed repository permission flags for a user")
public record AccessPermissionsDto(
        @Schema(description = "Can administer the repository", example = "true")
        boolean admin,
        @Schema(description = "Can manage repository settings without full admin", example = "false")
        boolean maintain,
        @Schema(description = "Can push / write to the repository", example = "true")
        boolean write,
        @Schema(description = "Can triage issues and pull requests", example = "false")
        boolean triage,
        @Schema(description = "Can read / pull from the repository", example = "true")
        boolean read
) {
}
