package com.githubaccess.report.mapper;

import com.githubaccess.report.dto.AccessPermissionsDto;
import com.githubaccess.report.dto.GitHubCollaboratorDto;
import com.githubaccess.report.dto.GitHubPermissionsDto;
import com.githubaccess.report.dto.GitHubRepositoryDto;
import com.githubaccess.report.dto.RepositoryAccessDto;
import com.githubaccess.report.enums.PermissionLevel;
import org.springframework.stereotype.Component;

@Component
public class AccessReportMapper {

    public RepositoryAccessDto toRepositoryAccess(GitHubRepositoryDto repository,
                                                  GitHubCollaboratorDto collaborator) {
        PermissionLevel permission = resolvePermission(collaborator);
        String roleName = collaborator.roleName() != null
                ? collaborator.roleName()
                : permission.getValue();
        AccessPermissionsDto permissions = toAccessPermissions(collaborator.permissions(), permission);
        return new RepositoryAccessDto(
                repository.name(),
                repository.fullName(),
                permission,
                roleName,
                describeAccess(permission),
                permissions
        );
    }

    public PermissionLevel resolvePermission(GitHubCollaboratorDto collaborator) {
        if (collaborator.roleName() != null && !collaborator.roleName().isBlank()) {
            PermissionLevel fromRole = PermissionLevel.fromValue(collaborator.roleName());
            if (fromRole != PermissionLevel.UNKNOWN) {
                return fromRole;
            }
        }
        return resolveFromPermissions(collaborator.permissions());
    }

    public PermissionLevel resolveFromPermissions(GitHubPermissionsDto permissions) {
        if (permissions == null) {
            return PermissionLevel.UNKNOWN;
        }
        if (permissions.admin()) {
            return PermissionLevel.ADMIN;
        }
        if (permissions.maintain()) {
            return PermissionLevel.MAINTAIN;
        }
        if (permissions.push()) {
            return PermissionLevel.WRITE;
        }
        if (permissions.triage()) {
            return PermissionLevel.TRIAGE;
        }
        if (permissions.pull()) {
            return PermissionLevel.READ;
        }
        return PermissionLevel.UNKNOWN;
    }

    public AccessPermissionsDto toAccessPermissions(GitHubPermissionsDto permissions,
                                                    PermissionLevel fallbackLevel) {
        if (permissions != null) {
            return new AccessPermissionsDto(
                    permissions.admin(),
                    permissions.maintain(),
                    permissions.push(),
                    permissions.triage(),
                    permissions.pull()
            );
        }
        return permissionsFromLevel(fallbackLevel);
    }

    public String describeAccess(PermissionLevel permission) {
        return switch (permission) {
            case ADMIN -> "Admin access — can manage settings, collaborators, and all repository content";
            case MAINTAIN -> "Maintain access — can manage the repository without destructive or security actions";
            case WRITE -> "Write access — can push to the repository and manage issues/PRs";
            case TRIAGE -> "Triage access — can manage issues and pull requests without write access to code";
            case READ -> "Read access — can view and clone the repository";
            case UNKNOWN -> "Access level could not be determined";
        };
    }

    private AccessPermissionsDto permissionsFromLevel(PermissionLevel level) {
        return switch (level) {
            case ADMIN -> new AccessPermissionsDto(true, true, true, true, true);
            case MAINTAIN -> new AccessPermissionsDto(false, true, true, true, true);
            case WRITE -> new AccessPermissionsDto(false, false, true, true, true);
            case TRIAGE -> new AccessPermissionsDto(false, false, false, true, true);
            case READ -> new AccessPermissionsDto(false, false, false, false, true);
            case UNKNOWN -> new AccessPermissionsDto(false, false, false, false, false);
        };
    }
}
