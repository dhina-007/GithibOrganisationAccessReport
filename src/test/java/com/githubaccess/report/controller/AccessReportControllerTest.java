package com.githubaccess.report.controller;

import com.githubaccess.report.dto.AccessPermissionsDto;
import com.githubaccess.report.dto.AccessReportResponse;
import com.githubaccess.report.dto.RepositoryAccessDto;
import com.githubaccess.report.dto.UserAccessDto;
import com.githubaccess.report.enums.PermissionLevel;
import com.githubaccess.report.exception.GlobalExceptionHandler;
import com.githubaccess.report.exception.ResourceNotFoundException;
import com.githubaccess.report.service.AccessReportPdfService;
import com.githubaccess.report.service.AccessReportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AccessReportController.class)
@Import(GlobalExceptionHandler.class)
class AccessReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AccessReportService accessReportService;

    @MockBean
    private AccessReportPdfService accessReportPdfService;

    @Test
    void getAccessReport_givenValidOrg_shouldReturn200() throws Exception {
        AccessReportResponse response = sampleReport();
        given(accessReportService.generateAccessReport("acme")).willReturn(response);

        mockMvc.perform(get("/github/organization/acme/access-report")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organization").value("acme"))
                .andExpect(jsonPath("$.totalUsers").value(1))
                .andExpect(jsonPath("$.users[0].login").value("alice"))
                .andExpect(jsonPath("$.users[0].repositories[0].permission").value("admin"))
                .andExpect(jsonPath("$.users[0].repositories[0].accessDescription").value(
                        "Admin access — can manage settings, collaborators, and all repository content"))
                .andExpect(jsonPath("$.users[0].repositories[0].permissions.admin").value(true))
                .andExpect(jsonPath("$.users[0].repositories[0].permissions.write").value(true));
    }

    @Test
    void getAccessReportPdf_givenValidOrg_shouldReturnPdf() throws Exception {
        byte[] pdfBytes = "%PDF-1.4 sample".getBytes();
        given(accessReportPdfService.generateAccessReportPdf("acme")).willReturn(pdfBytes);

        mockMvc.perform(get("/github/organization/acme/access-report/pdf")
                        .accept(MediaType.APPLICATION_PDF))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"access-report-acme.pdf\""))
                .andExpect(header().string("Content-Type", MediaType.APPLICATION_PDF_VALUE));
    }

    @Test
    void getAccessReport_givenNonExistentOrg_shouldReturn404() throws Exception {
        given(accessReportService.generateAccessReport("missing"))
                .willThrow(new ResourceNotFoundException("Organization", "missing"));

        mockMvc.perform(get("/github/organization/missing/access-report"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void getAccessReport_givenInvalidOrgName_shouldReturn400() throws Exception {
        mockMvc.perform(get("/github/organization/-invalid/access-report"))
                .andExpect(status().isBadRequest());
    }

    private AccessReportResponse sampleReport() {
        return new AccessReportResponse(
                "acme",
                Instant.parse("2026-07-11T06:00:00Z"),
                1,
                1,
                List.of(new UserAccessDto(
                        "alice",
                        List.of(new RepositoryAccessDto(
                                "api",
                                "acme/api",
                                PermissionLevel.ADMIN,
                                "admin",
                                "Admin access — can manage settings, collaborators, and all repository content",
                                new AccessPermissionsDto(true, true, true, true, true)
                        ))
                ))
        );
    }
}
