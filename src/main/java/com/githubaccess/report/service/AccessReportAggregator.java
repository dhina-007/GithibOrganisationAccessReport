package com.githubaccess.report.service;

import com.githubaccess.report.dto.AccessReportResponse;
import com.githubaccess.report.dto.GitHubCollaboratorDto;
import com.githubaccess.report.dto.GitHubRepositoryDto;
import com.githubaccess.report.dto.RepositoryAccessDto;
import com.githubaccess.report.dto.RepositoryCollaborators;
import com.githubaccess.report.dto.UserAccessDto;
import com.githubaccess.report.mapper.AccessReportMapper;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class AccessReportAggregator {

    private final AccessReportMapper accessReportMapper;

    public AccessReportAggregator(AccessReportMapper accessReportMapper) {
        this.accessReportMapper = accessReportMapper;
    }

    public AccessReportResponse aggregate(String organization,
                                          List<RepositoryCollaborators> repositoryCollaborators) {
        Map<String, List<RepositoryAccessDto>> accessByUser = new LinkedHashMap<>();

        for (RepositoryCollaborators entry : repositoryCollaborators) {
            GitHubRepositoryDto repository = entry.repository();
            for (GitHubCollaboratorDto collaborator : entry.collaborators()) {
                if (collaborator.login() == null || collaborator.login().isBlank()) {
                    continue;
                }
                RepositoryAccessDto access = accessReportMapper.toRepositoryAccess(repository, collaborator);
                accessByUser
                        .computeIfAbsent(collaborator.login(), key -> new ArrayList<>())
                        .add(access);
            }
        }

        List<UserAccessDto> users = accessByUser.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER))
                .map(entry -> {
                    List<RepositoryAccessDto> repos = entry.getValue().stream()
                            .sorted(Comparator.comparing(RepositoryAccessDto::fullName,
                                    String.CASE_INSENSITIVE_ORDER))
                            .toList();
                    return new UserAccessDto(entry.getKey(), repos);
                })
                .toList();

        return new AccessReportResponse(
                organization,
                Instant.now(),
                users.size(),
                repositoryCollaborators.size(),
                users
        );
    }
}
