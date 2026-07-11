package com.githubaccess.report.controller;

import com.githubaccess.report.dto.AccessReportResponse;
import com.githubaccess.report.dto.ErrorResponse;
import com.githubaccess.report.service.AccessReportPdfService;
import com.githubaccess.report.service.AccessReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/github/organization")
@Validated
@Tag(name = "Access Report", description = "GitHub organization repository access reports")
public class AccessReportController {

    private static final String ORG_PATTERN =
            "^[A-Za-z0-9](?:[A-Za-z0-9]|-(?=[A-Za-z0-9])){0,38}$";

    private final AccessReportService accessReportService;
    private final AccessReportPdfService accessReportPdfService;

    public AccessReportController(AccessReportService accessReportService,
                                  AccessReportPdfService accessReportPdfService) {
        this.accessReportService = accessReportService;
        this.accessReportPdfService = accessReportPdfService;
    }

    @GetMapping(value = "/{org}/access-report", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Generate organization access report (JSON)",
            description = """
                    Returns an aggregated JSON report mapping each user to the repositories \
                    they can access within the given GitHub organization, including permission details.

                    Requires `GITHUB_TOKEN` to be configured on the server.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Access report generated successfully",
                    content = @Content(schema = @Schema(implementation = AccessReportResponse.class))),
            @ApiResponse(responseCode = "401", description = "GitHub authentication failed",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient GitHub permissions",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Organization not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "Missing token or invalid organization",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "503", description = "GitHub temporarily unavailable",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public AccessReportResponse getAccessReport(
            @Parameter(description = "GitHub organization login", example = "dhina-project")
            @PathVariable("org")
            @NotBlank(message = "Organization name must not be blank")
            @Pattern(regexp = ORG_PATTERN, message = "Organization name must be a valid GitHub login")
            String org) {
        return accessReportService.generateAccessReport(org);
    }

    @GetMapping(value = "/{org}/access-report/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(
            summary = "Download organization access report (PDF)",
            description = """
                    Generates a structured PDF report of which users have access to which repositories \
                    in the organization, including permission levels and access details.

                    Requires `GITHUB_TOKEN` to be configured on the server.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "PDF report generated successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_PDF_VALUE)),
            @ApiResponse(responseCode = "401", description = "GitHub authentication failed",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient GitHub permissions",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Organization not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "Missing token or invalid organization",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "503", description = "GitHub temporarily unavailable",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<byte[]> getAccessReportPdf(
            @Parameter(description = "GitHub organization login", example = "dhina-project")
            @PathVariable("org")
            @NotBlank(message = "Organization name must not be blank")
            @Pattern(regexp = ORG_PATTERN, message = "Organization name must be a valid GitHub login")
            String org) {
        byte[] pdf = accessReportPdfService.generateAccessReportPdf(org);
        String filename = "access-report-" + org + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.length)
                .body(pdf);
    }
}
