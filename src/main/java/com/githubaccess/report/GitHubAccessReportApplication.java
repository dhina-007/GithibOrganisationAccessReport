package com.githubaccess.report;

import com.githubaccess.report.config.GitHubProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(GitHubProperties.class)
public class GitHubAccessReportApplication {

    public static void main(String[] args) {
        SpringApplication.run(GitHubAccessReportApplication.class, args);
    }
}
