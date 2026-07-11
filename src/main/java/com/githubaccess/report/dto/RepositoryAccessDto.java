package com.githubaccess.report.dto;

import com.githubaccess.report.enums.PermissionLevel;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Repository access details for a user")
public record RepositoryAccessDto(
        @Schema(description = "Repository name", example = "api")
        String name,
        @Schema(description = "Full repository name", example = "acme/api")
        String fullName,
        @Schema(description = "Highest permission level", example = "admin")
        PermissionLevel permission,
        @Schema(description = "GitHub role name when available", example = "admin")
        String roleName,
        @Schema(description = "Human-readable description of the user's access",
                example = "Admin access — can manage settings, collaborators, and all repository content")
        String accessDescription,
        @Schema(description = "Detailed permission flags for this repository")
        AccessPermissionsDto permissions
) {
}
