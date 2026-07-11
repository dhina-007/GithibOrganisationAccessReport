package com.githubaccess.report.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI githubAccessReportOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("GitHub Organization Access Report API")
                        .description("""
                                Generates an aggregated report of which users have access to which \
                                repositories within a GitHub organization.

                                Configure authentication with a GitHub Personal Access Token via the \
                                `GITHUB_TOKEN` environment variable before starting the application.
                                """)
                        .version("1.0.0")
                        .contact(new Contact().name("GitHub Access Report")))
                .servers(List.of(new Server().url("http://localhost:8080").description("Local")));
    }
}
