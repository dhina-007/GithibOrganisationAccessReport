package com.githubaccess.report.service;

import com.githubaccess.report.client.GitHubClient;
import com.githubaccess.report.config.GitHubProperties;
import com.githubaccess.report.dto.AccessReportResponse;
import com.githubaccess.report.dto.GitHubCollaboratorDto;
import com.githubaccess.report.dto.GitHubPermissionsDto;
import com.githubaccess.report.dto.GitHubRepositoryDto;
import com.githubaccess.report.exception.BusinessRuleViolationException;
import com.githubaccess.report.exception.GitHubForbiddenException;
import com.githubaccess.report.exception.ResourceNotFoundException;
import com.githubaccess.report.mapper.AccessReportMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class DefaultAccessReportServiceTest {

    @Mock
    private GitHubClient gitHubClient;

    private GitHubProperties gitHubProperties;
    private DefaultAccessReportService accessReportService;

    @BeforeEach
    void setUp() {
        gitHubProperties = new GitHubProperties();
        gitHubProperties.setToken("test-token");
        AccessReportAggregator aggregator = new AccessReportAggregator(new AccessReportMapper());
        Executor directExecutor = Runnable::run;
        accessReportService = new DefaultAccessReportService(
                gitHubClient, aggregator, gitHubProperties, directExecutor);
    }

    @Test
    void generateAccessReport_givenNullOrganization_shouldThrowBusinessRuleViolation() {
        assertThatThrownBy(() -> accessReportService.generateAccessReport(null))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("blank");
        verifyNoInteractions(gitHubClient);
    }

    @Test
    void generateAccessReport_givenBlankOrganization_shouldThrowBusinessRuleViolation() {
        assertThatThrownBy(() -> accessReportService.generateAccessReport("   "))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("blank");
    }

    @Test
    void generateAccessReport_givenMissingToken_shouldThrowBusinessRuleViolation() {
        gitHubProperties.setToken("");

        assertThatThrownBy(() -> accessReportService.generateAccessReport("acme"))
                .isInstanceOf(BusinessRuleViolationException.class)
                .extracting("errorCode")
                .isEqualTo("GITHUB_TOKEN_MISSING");
        verifyNoInteractions(gitHubClient);
    }

    @Test
    void generateAccessReport_givenNonExistentOrg_shouldThrowResourceNotFound() {
        given(gitHubClient.listOrganizationRepositories("missing"))
                .willThrow(new ResourceNotFoundException("Organization", "missing"));

        assertThatThrownBy(() -> accessReportService.generateAccessReport("missing"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("missing");
    }

    @Test
    void generateAccessReport_givenForbidden_shouldThrowGitHubForbidden() {
        given(gitHubClient.listOrganizationRepositories("secret"))
                .willThrow(new GitHubForbiddenException("Access denied"));

        assertThatThrownBy(() -> accessReportService.generateAccessReport("secret"))
                .isInstanceOf(GitHubForbiddenException.class);
    }

    @Test
    void generateAccessReport_givenValidOrg_shouldReturnAggregatedReport() {
        GitHubRepositoryDto repo = new GitHubRepositoryDto(1L, "api", "acme/api", true);
        GitHubCollaboratorDto alice = new GitHubCollaboratorDto(
                10L,
                "alice",
                new GitHubPermissionsDto(true, false, true, false, true),
                "admin"
        );
        given(gitHubClient.listOrganizationRepositories("acme")).willReturn(List.of(repo));
        given(gitHubClient.listRepositoryCollaborators("acme", "api")).willReturn(List.of(alice));

        AccessReportResponse report = accessReportService.generateAccessReport("acme");

        assertThat(report.organization()).isEqualTo("acme");
        assertThat(report.totalRepositories()).isEqualTo(1);
        assertThat(report.totalUsers()).isEqualTo(1);
        assertThat(report.users().get(0).login()).isEqualTo("alice");
        assertThat(report.users().get(0).repositories()).hasSize(1);
    }

    @Test
    void generateAccessReport_givenNoRepositories_shouldReturnEmptyUsers() {
        given(gitHubClient.listOrganizationRepositories("empty")).willReturn(List.of());

        AccessReportResponse report = accessReportService.generateAccessReport("empty");

        assertThat(report.totalRepositories()).isZero();
        assertThat(report.users()).isEmpty();
    }
}
