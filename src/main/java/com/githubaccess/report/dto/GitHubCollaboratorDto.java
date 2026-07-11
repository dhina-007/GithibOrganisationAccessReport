package com.githubaccess.report.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubCollaboratorDto(
        Long id,
        String login,
        @JsonProperty("permissions") GitHubPermissionsDto permissions,
        @JsonProperty("role_name") String roleName
) {
}
