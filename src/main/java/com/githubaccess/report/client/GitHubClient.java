package com.githubaccess.report.client;

import com.githubaccess.report.dto.GitHubCollaboratorDto;
import com.githubaccess.report.dto.GitHubRepositoryDto;

import java.util.List;

public interface GitHubClient {

    List<GitHubRepositoryDto> listOrganizationRepositories(String org);

    List<GitHubCollaboratorDto> listRepositoryCollaborators(String owner, String repo);
}
