package com.githubaccess.report.service;

import com.githubaccess.report.dto.AccessReportResponse;
import com.githubaccess.report.dto.GitHubCollaboratorDto;
import com.githubaccess.report.dto.GitHubPermissionsDto;
import com.githubaccess.report.dto.GitHubRepositoryDto;
import com.githubaccess.report.dto.RepositoryCollaborators;
import com.githubaccess.report.dto.UserAccessDto;
import com.githubaccess.report.enums.PermissionLevel;
import com.githubaccess.report.mapper.AccessReportMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AccessReportAggregatorTest {

    private AccessReportAggregator aggregator;

    @BeforeEach
    void setUp() {
        aggregator = new AccessReportAggregator(new AccessReportMapper());
    }

    @Test
    void aggregate_givenEmptyRepositories_shouldReturnEmptyUsers() {
        AccessReportResponse report = aggregator.aggregate("acme", List.of());

        assertThat(report.organization()).isEqualTo("acme");
        assertThat(report.totalUsers()).isZero();
        assertThat(report.totalRepositories()).isZero();
        assertThat(report.users()).isEmpty();
    }

    @Test
    void aggregate_givenSingleUserAcrossRepos_shouldGroupByUser() {
        GitHubRepositoryDto api = new GitHubRepositoryDto(1L, "api", "acme/api", true);
        GitHubRepositoryDto web = new GitHubRepositoryDto(2L, "web", "acme/web", false);
        GitHubCollaboratorDto alice = collaborator("alice", true, false, true, false, true, "admin");

        AccessReportResponse report = aggregator.aggregate("acme", List.of(
                new RepositoryCollaborators(api, List.of(alice)),
                new RepositoryCollaborators(web, List.of(alice))
        ));

        assertThat(report.totalUsers()).isEqualTo(1);
        assertThat(report.totalRepositories()).isEqualTo(2);
        UserAccessDto user = report.users().get(0);
        assertThat(user.login()).isEqualTo("alice");
        assertThat(user.repositories()).hasSize(2);
        assertThat(user.repositories().get(0).fullName()).isEqualTo("acme/api");
        assertThat(user.repositories().get(0).permission()).isEqualTo(PermissionLevel.ADMIN);
        assertThat(user.repositories().get(0).accessDescription()).contains("Admin access");
        assertThat(user.repositories().get(0).permissions().admin()).isTrue();
        assertThat(user.repositories().get(0).permissions().write()).isTrue();
        assertThat(user.repositories().get(0).permissions().read()).isTrue();
        assertThat(user.repositories().get(1).fullName()).isEqualTo("acme/web");
    }

    @Test
    void aggregate_givenPermissionFlagsOnly_shouldMapWritePermission() {
        GitHubRepositoryDto api = new GitHubRepositoryDto(1L, "api", "acme/api", true);
        GitHubCollaboratorDto bob = collaborator("bob", false, false, true, false, true, null);

        AccessReportResponse report = aggregator.aggregate("acme", List.of(
                new RepositoryCollaborators(api, List.of(bob))
        ));

        assertThat(report.users()).hasSize(1);
        assertThat(report.users().get(0).repositories().get(0).permission())
                .isEqualTo(PermissionLevel.WRITE);
    }

    @Test
    void aggregate_givenBlankLogin_shouldSkipCollaborator() {
        GitHubRepositoryDto api = new GitHubRepositoryDto(1L, "api", "acme/api", true);
        GitHubCollaboratorDto blank = collaborator("  ", false, false, false, false, true, "read");

        AccessReportResponse report = aggregator.aggregate("acme", List.of(
                new RepositoryCollaborators(api, List.of(blank))
        ));

        assertThat(report.totalUsers()).isZero();
        assertThat(report.users()).isEmpty();
    }

    private GitHubCollaboratorDto collaborator(String login,
                                               boolean admin,
                                               boolean maintain,
                                               boolean push,
                                               boolean triage,
                                               boolean pull,
                                               String roleName) {
        return new GitHubCollaboratorDto(
                1L,
                login,
                new GitHubPermissionsDto(admin, maintain, push, triage, pull),
                roleName
        );
    }
}
