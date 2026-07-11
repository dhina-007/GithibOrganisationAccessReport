package com.githubaccess.report.exception;

import org.springframework.http.HttpStatus;

public class GitHubUnauthorizedException extends DomainException {

    public GitHubUnauthorizedException(String message) {
        super("GITHUB_UNAUTHORIZED", message, HttpStatus.UNAUTHORIZED);
    }
}
