package com.githubaccess.report.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends DomainException {

    public ResourceNotFoundException(String resource, Object id) {
        super("RESOURCE_NOT_FOUND",
                String.format("%s '%s' was not found", resource, id),
                HttpStatus.NOT_FOUND);
    }
}
