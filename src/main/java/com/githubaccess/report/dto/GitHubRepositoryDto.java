package com.githubaccess.report.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubRepositoryDto(
        Long id,
        String name,
        @JsonProperty("full_name") String fullName,
        @JsonProperty("private") boolean isPrivate
) {
}
