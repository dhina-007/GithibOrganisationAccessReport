package com.githubaccess.report.config.aop;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;

@Aspect
@Component
public class RequestResponseLoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(RequestResponseLoggingAspect.class);
    private static final int MAX_BODY_LENGTH = 2000;

    private final ObjectMapper objectMapper;

    public RequestResponseLoggingAspect(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Around("execution(* com.githubaccess.report.controller..*(..))")
    public Object logRequestAndResponse(ProceedingJoinPoint pjp) throws Throwable {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return pjp.proceed();
        }

        HttpServletRequest request = attributes.getRequest();
        String method = request.getMethod();
        String url = buildUrl(request);

        log.info("→ REQUEST {} {} args={}", method, url, summarizeArgs(pjp.getArgs()));

        long start = System.currentTimeMillis();
        try {
            Object result = pjp.proceed();
            String responseBody = serializeResponse(result);
            log.info("← RESPONSE {} {} [{}ms] body={}",
                    method, url, System.currentTimeMillis() - start, responseBody);
            return result;
        } catch (Throwable ex) {
            log.error("✗ RESPONSE {} {} error after {}ms: {}",
                    method, url, System.currentTimeMillis() - start, ex.getMessage());
            throw ex;
        }
    }

    private String buildUrl(HttpServletRequest request) {
        String queryString = request.getQueryString();
        if (queryString == null || queryString.isBlank()) {
            return request.getRequestURI();
        }
        return request.getRequestURI() + "?" + queryString;
    }

    private String summarizeArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }
        return maskSensitive(String.valueOf(Arrays.toString(args)));
    }

    private String serializeResponse(Object result) {
        if (result == null) {
            return "null";
        }
        try {
            String json = objectMapper.writeValueAsString(result);
            return truncate(maskSensitive(json));
        } catch (Exception ex) {
            return truncate(String.valueOf(result));
        }
    }

    private String maskSensitive(String value) {
        if (value == null) {
            return null;
        }
        return value
                .replaceAll("(?i)(Bearer\\s+)[A-Za-z0-9._-]+", "$1***")
                .replaceAll("(?i)(ghp_|github_pat_)[A-Za-z0-9_]+", "$1***");
    }

    private String truncate(String value) {
        if (value == null || value.length() <= MAX_BODY_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_BODY_LENGTH) + "...(truncated)";
    }
}
