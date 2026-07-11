package com.githubaccess.report.client;

import com.githubaccess.report.config.GitHubProperties;
import com.githubaccess.report.constant.GitHubApiConstants;
import com.githubaccess.report.dto.GitHubCollaboratorDto;
import com.githubaccess.report.dto.GitHubRepositoryDto;
import com.githubaccess.report.exception.DomainException;
import com.githubaccess.report.exception.GitHubForbiddenException;
import com.githubaccess.report.exception.GitHubUnauthorizedException;
import com.githubaccess.report.exception.ResourceNotFoundException;
import com.githubaccess.report.exception.TechnicalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class DefaultGitHubClient implements GitHubClient {

    private static final Logger log = LoggerFactory.getLogger(DefaultGitHubClient.class);

    private static final Pattern NEXT_LINK_PATTERN =
            Pattern.compile("<([^>]+)>;\\s*rel=\"next\"", Pattern.CASE_INSENSITIVE);

    private static final ParameterizedTypeReference<List<GitHubRepositoryDto>> REPO_LIST_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private static final ParameterizedTypeReference<List<GitHubCollaboratorDto>> COLLABORATOR_LIST_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final WebClient gitHubWebClient;
    private final GitHubProperties gitHubProperties;

    public DefaultGitHubClient(WebClient gitHubWebClient, GitHubProperties gitHubProperties) {
        this.gitHubWebClient = gitHubWebClient;
        this.gitHubProperties = gitHubProperties;
    }

    @Override
    public List<GitHubRepositoryDto> listOrganizationRepositories(String org) {
        log.debug("QUERY listing repositories for org={}", org);
        List<GitHubRepositoryDto> repositories = fetchAllPages(
                () -> URI.create(gitHubProperties.getBaseUrl()
                        + "/orgs/" + org + "/repos"
                        + "?" + GitHubApiConstants.QUERY_PER_PAGE + "=" + gitHubProperties.getPerPage()
                        + "&" + GitHubApiConstants.QUERY_PAGE + "=1"),
                REPO_LIST_TYPE,
                "Organization",
                org
        );
        log.debug("QUERY response org={} repositoryCount={}", org, repositories.size());
        return repositories;
    }

    @Override
    public List<GitHubCollaboratorDto> listRepositoryCollaborators(String owner, String repo) {
        log.debug("QUERY listing collaborators for repo={}/{}", owner, repo);
        List<GitHubCollaboratorDto> collaborators = fetchAllPages(
                () -> URI.create(gitHubProperties.getBaseUrl()
                        + "/repos/" + owner + "/" + repo + "/collaborators"
                        + "?" + GitHubApiConstants.QUERY_PER_PAGE + "=" + gitHubProperties.getPerPage()
                        + "&" + GitHubApiConstants.QUERY_PAGE + "=1"
                        + "&" + GitHubApiConstants.QUERY_AFFILIATION + "="
                        + GitHubApiConstants.AFFILIATION_ALL),
                COLLABORATOR_LIST_TYPE,
                "Repository",
                owner + "/" + repo
        );
        log.debug("QUERY response repo={}/{} collaboratorCount={}", owner, repo, collaborators.size());
        return collaborators;
    }

    private <T> List<T> fetchAllPages(Supplier<URI> firstPageUri,
                                      ParameterizedTypeReference<List<T>> type,
                                      String resourceLabel,
                                      String resourceId) {
        List<T> allItems = new ArrayList<>();
        URI currentUri = firstPageUri.get();

        while (currentUri != null) {
            URI pageUri = currentUri;
            PageResult<T> page = executeWithRetry(
                    () -> gitHubWebClient.get()
                            .uri(pageUri)
                            .exchangeToMono(response -> mapPageResponse(response, type, resourceLabel, resourceId))
                            .block(),
                    resourceLabel,
                    resourceId
            );
            allItems.addAll(page.items());
            currentUri = page.nextUrl().orElse(null);
        }

        return allItems;
    }

    private <T> Mono<PageResult<T>> mapPageResponse(ClientResponse response,
                                                    ParameterizedTypeReference<List<T>> type,
                                                    String resourceLabel,
                                                    String resourceId) {
        HttpStatusCode status = response.statusCode();
        if (status.is2xxSuccessful()) {
            Optional<URI> nextLink = extractNextLink(response.headers().asHttpHeaders());
            logRateLimit(response.headers().asHttpHeaders());
            return response.bodyToMono(type)
                    .defaultIfEmpty(List.of())
                    .map(items -> new PageResult<>(items, nextLink));
        }
        return response.bodyToMono(String.class)
                .defaultIfEmpty("")
                .flatMap(body -> Mono.error(mapError(status, body, resourceLabel, resourceId)));
    }

    private RuntimeException mapError(HttpStatusCode status, String body, String resourceLabel, String resourceId) {
        int code = status.value();
        if (code == 401) {
            return new GitHubUnauthorizedException(
                    "GitHub authentication failed. Check that GITHUB_TOKEN is set and valid.");
        }
        if (code == 403) {
            return new GitHubForbiddenException(
                    "Access denied by GitHub for " + resourceLabel + " '" + resourceId
                            + "'. Ensure the token has sufficient permissions.");
        }
        if (code == 404) {
            return new ResourceNotFoundException(resourceLabel, resourceId);
        }
        if (code == 429) {
            return new TechnicalException("GitHub rate limit exceeded while fetching " + resourceLabel
                    + " '" + resourceId + "'");
        }
        return new TechnicalException("GitHub API returned HTTP " + code + " for " + resourceLabel
                + " '" + resourceId + "'" + (body.isBlank() ? "" : ": " + truncate(body)));
    }

    private <T> T executeWithRetry(Callable<T> call, String resourceLabel, String resourceId) {
        int maxAttempts = gitHubProperties.getRetry().getMaxAttempts();
        long delayMs = gitHubProperties.getRetry().getInitialDelayMs();
        double multiplier = gitHubProperties.getRetry().getMultiplier();
        TechnicalException lastError = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return call.call();
            } catch (DomainException ex) {
                throw ex;
            } catch (WebClientResponseException ex) {
                if (ex.getStatusCode().is4xxClientError() && ex.getStatusCode().value() != 429) {
                    throw mapError(ex.getStatusCode(), ex.getResponseBodyAsString(), resourceLabel, resourceId);
                }
                lastError = new TechnicalException(
                        "GitHub request failed for " + resourceLabel + " '" + resourceId + "'", ex);
            } catch (WebClientRequestException ex) {
                lastError = new TechnicalException(
                        "GitHub connection failed for " + resourceLabel + " '" + resourceId + "'", ex);
            } catch (TechnicalException ex) {
                lastError = ex;
            } catch (Exception ex) {
                Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                if (cause instanceof DomainException domainException) {
                    throw domainException;
                }
                if (cause instanceof TechnicalException technicalException) {
                    lastError = technicalException;
                } else {
                    lastError = new TechnicalException(
                            "Unexpected failure calling GitHub for " + resourceLabel + " '" + resourceId + "'",
                            cause);
                }
            }

            if (attempt < maxAttempts) {
                log.warn("Retrying GitHub call for {} '{}' attempt={}/{} delayMs={}",
                        resourceLabel, resourceId, attempt, maxAttempts, delayMs);
                sleep(delayMs);
                delayMs = Math.round(delayMs * multiplier);
            }
        }

        throw lastError != null
                ? lastError
                : new TechnicalException("GitHub call failed for " + resourceLabel + " '" + resourceId + "'");
    }

    private void sleep(long delayMs) {
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new TechnicalException("Interrupted while waiting to retry GitHub call", ie);
        }
    }

    private Optional<URI> extractNextLink(HttpHeaders headers) {
        List<String> linkHeaders = headers.get(GitHubApiConstants.HEADER_LINK);
        if (linkHeaders == null || linkHeaders.isEmpty()) {
            return Optional.empty();
        }
        String linkHeader = String.join(",", linkHeaders);
        Matcher matcher = NEXT_LINK_PATTERN.matcher(linkHeader);
        if (matcher.find()) {
            return Optional.of(URI.create(matcher.group(1)));
        }
        return Optional.empty();
    }

    private void logRateLimit(HttpHeaders headers) {
        String remaining = headers.getFirst(GitHubApiConstants.HEADER_RATE_LIMIT_REMAINING);
        if (remaining != null) {
            log.debug("GitHub rate limit remaining={}", remaining);
        }
    }

    private String truncate(String body) {
        return body.length() > 200 ? body.substring(0, 200) + "..." : body;
    }

    private record PageResult<T>(List<T> items, Optional<URI> nextUrl) {
    }
}
