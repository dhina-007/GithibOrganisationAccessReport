package com.githubaccess.report.mapper;

import com.githubaccess.report.dto.GitHubCollaboratorDto;
import com.githubaccess.report.dto.GitHubPermissionsDto;
import com.githubaccess.report.enums.PermissionLevel;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AccessReportMapperTest {

    private final AccessReportMapper mapper = new AccessReportMapper();

    @Test
    void resolvePermission_givenAdminRole_shouldReturnAdmin() {
        GitHubCollaboratorDto collaborator = new GitHubCollaboratorDto(
                1L, "alice",
                new GitHubPermissionsDto(true, false, true, false, true),
                "admin"
        );

        assertThat(mapper.resolvePermission(collaborator)).isEqualTo(PermissionLevel.ADMIN);
    }

    @Test
    void resolveFromPermissions_givenNull_shouldReturnUnknown() {
        assertThat(mapper.resolveFromPermissions(null)).isEqualTo(PermissionLevel.UNKNOWN);
    }

    @Test
    void resolveFromPermissions_givenTriageOnly_shouldReturnTriage() {
        GitHubPermissionsDto permissions = new GitHubPermissionsDto(false, false, false, true, true);

        assertThat(mapper.resolveFromPermissions(permissions)).isEqualTo(PermissionLevel.TRIAGE);
    }

    @Test
    void describeAccess_givenRead_shouldExplainReadAccess() {
        assertThat(mapper.describeAccess(PermissionLevel.READ))
                .contains("Read access");
    }

    @Test
    void toAccessPermissions_givenGitHubFlags_shouldMapWriteAndRead() {
        GitHubPermissionsDto permissions = new GitHubPermissionsDto(false, false, true, false, true);

        assertThat(mapper.toAccessPermissions(permissions, PermissionLevel.WRITE).write()).isTrue();
        assertThat(mapper.toAccessPermissions(permissions, PermissionLevel.WRITE).read()).isTrue();
        assertThat(mapper.toAccessPermissions(permissions, PermissionLevel.WRITE).admin()).isFalse();
    }
}
