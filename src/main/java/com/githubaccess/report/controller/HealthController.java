package com.githubaccess.report.controller;

import com.githubaccess.report.dto.HealthResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/github")
@Tag(name = "Health", description = "Application health check")
public class HealthController {

    private final String applicationName;

    public HealthController(@Value("${spring.application.name:github-access-report}") String applicationName) {
        this.applicationName = applicationName;
    }

    @GetMapping(value = "/health", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Health check", description = "Returns UP when the application is running")
    @ApiResponse(responseCode = "200", description = "Application is healthy",
            content = @Content(schema = @Schema(implementation = HealthResponse.class)))
    public HealthResponse health() {
        return new HealthResponse("UP", applicationName);
    }
}
