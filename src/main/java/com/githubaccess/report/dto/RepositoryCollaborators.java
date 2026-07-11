package com.githubaccess.report.dto;

import java.util.List;

public record RepositoryCollaborators(
        GitHubRepositoryDto repository,
        List<GitHubCollaboratorDto> collaborators
) {
}
