package com.githubaccess.report.service;

import com.githubaccess.report.dto.AccessPermissionsDto;
import com.githubaccess.report.dto.AccessReportResponse;
import com.githubaccess.report.dto.RepositoryAccessDto;
import com.githubaccess.report.dto.UserAccessDto;
import com.githubaccess.report.enums.PermissionLevel;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AccessReportPdfGeneratorTest {

    private final AccessReportPdfGenerator generator = new AccessReportPdfGenerator();

    @Test
    void generate_givenReportWithUsers_shouldProducePdfBytes() {
        AccessReportResponse report = new AccessReportResponse(
                "dhina-project",
                Instant.parse("2026-07-11T06:00:00Z"),
                2,
                1,
                List.of(
                        new UserAccessDto("AdarshAB-43", List.of(
                                new RepositoryAccessDto(
                                        "Test",
                                        "dhina-project/Test",
                                        PermissionLevel.READ,
                                        "read",
                                        "Read access — can view and clone the repository",
                                        new AccessPermissionsDto(false, false, false, false, true)
                                )
                        )),
                        new UserAccessDto("dhina-007", List.of(
                                new RepositoryAccessDto(
                                        "Test",
                                        "dhina-project/Test",
                                        PermissionLevel.ADMIN,
                                        "admin",
                                        "Admin access — can manage settings, collaborators, and all repository content",
                                        new AccessPermissionsDto(true, true, true, true, true)
                                )
                        ))
                )
        );

        byte[] pdf = generator.generate(report);

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
    }

    @Test
    void generate_givenEmptyUsers_shouldStillProducePdf() {
        AccessReportResponse report = new AccessReportResponse(
                "empty-org",
                Instant.parse("2026-07-11T06:00:00Z"),
                0,
                0,
                List.of()
        );

        byte[] pdf = generator.generate(report);

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
    }
}
