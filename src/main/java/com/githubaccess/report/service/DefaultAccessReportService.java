package com.githubaccess.report.service;

import com.githubaccess.report.client.GitHubClient;
import com.githubaccess.report.config.GitHubProperties;
import com.githubaccess.report.dto.AccessReportResponse;
import com.githubaccess.report.dto.GitHubCollaboratorDto;
import com.githubaccess.report.dto.GitHubRepositoryDto;
import com.githubaccess.report.dto.RepositoryCollaborators;
import com.githubaccess.report.exception.BusinessRuleViolationException;
import com.githubaccess.report.exception.TechnicalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

@Service
public class DefaultAccessReportService implements AccessReportService {

    private static final Logger log = LoggerFactory.getLogger(DefaultAccessReportService.class);

    private final GitHubClient gitHubClient;
    private final AccessReportAggregator accessReportAggregator;
    private final GitHubProperties gitHubProperties;
    private final Executor gitHubTaskExecutor;

    public DefaultAccessReportService(GitHubClient gitHubClient,
                                      AccessReportAggregator accessReportAggregator,
                                      GitHubProperties gitHubProperties,
                                      @Qualifier("gitHubTaskExecutor") Executor gitHubTaskExecutor) {
        this.gitHubClient = gitHubClient;
        this.accessReportAggregator = accessReportAggregator;
        this.gitHubProperties = gitHubProperties;
        this.gitHubTaskExecutor = gitHubTaskExecutor;
    }

    @Override
    public AccessReportResponse generateAccessReport(String organization) {
        validateTokenConfigured();
        validateOrganization(organization);

        String org = organization.trim();
        log.info("Generating access report for organization={}", org);

        List<GitHubRepositoryDto> repositories = gitHubClient.listOrganizationRepositories(org);
        List<RepositoryCollaborators> repositoryCollaborators = fetchCollaboratorsInParallel(repositories);

        AccessReportResponse report = accessReportAggregator.aggregate(org, repositoryCollaborators);
        log.info("Access report generated for organization={} users={} repositories={}",
                org, report.totalUsers(), report.totalRepositories());
        return report;
    }

    private List<RepositoryCollaborators> fetchCollaboratorsInParallel(List<GitHubRepositoryDto> repositories) {
        if (repositories.isEmpty()) {
            return List.of();
        }

        List<CompletableFuture<RepositoryCollaborators>> futures = repositories.stream()
                .map(repository -> CompletableFuture.supplyAsync(
                        () -> fetchCollaboratorsForRepository(repository),
                        gitHubTaskExecutor))
                .toList();

        List<RepositoryCollaborators> results = new ArrayList<>(repositories.size());
        for (CompletableFuture<RepositoryCollaborators> future : futures) {
            try {
                results.add(future.join());
            } catch (CompletionException ex) {
                Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                if (cause instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                }
                throw new TechnicalException("Failed while fetching repository collaborators", cause);
            }
        }
        return results;
    }

    private RepositoryCollaborators fetchCollaboratorsForRepository(GitHubRepositoryDto repository) {
        String[] parts = splitFullName(repository);
        List<GitHubCollaboratorDto> collaborators =
                gitHubClient.listRepositoryCollaborators(parts[0], parts[1]);
        return new RepositoryCollaborators(repository, collaborators);
    }

    private String[] splitFullName(GitHubRepositoryDto repository) {
        if (repository.fullName() != null && repository.fullName().contains("/")) {
            String[] parts = repository.fullName().split("/", 2);
            return new String[]{parts[0], parts[1]};
        }
        throw new TechnicalException("Repository full name is invalid: " + repository.name());
    }

    private void validateTokenConfigured() {
        String token = gitHubProperties.getToken();
        if (token == null || token.isBlank()) {
            throw new BusinessRuleViolationException(
                    "GITHUB_TOKEN_MISSING",
                    "GITHUB_TOKEN is not configured. Set the environment variable before calling this API.");
        }
    }

    private void validateOrganization(String organization) {
        if (organization == null || organization.isBlank()) {
            throw new BusinessRuleViolationException(
                    "INVALID_ORGANIZATION",
                    "Organization name must not be blank");
        }
    }
}
