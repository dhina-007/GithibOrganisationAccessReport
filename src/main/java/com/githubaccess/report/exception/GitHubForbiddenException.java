package com.githubaccess.report.exception;

import org.springframework.http.HttpStatus;

public class GitHubForbiddenException extends DomainException {

    public GitHubForbiddenException(String message) {
        super("GITHUB_FORBIDDEN", message, HttpStatus.FORBIDDEN);
    }
}
