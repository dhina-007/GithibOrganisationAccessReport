package com.githubaccess.report.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubPermissionsDto(
        boolean admin,
        boolean maintain,
        boolean push,
        boolean triage,
        boolean pull
) {
}
