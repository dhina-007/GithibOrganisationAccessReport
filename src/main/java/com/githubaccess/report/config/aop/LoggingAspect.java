package com.githubaccess.report.config.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(LoggingAspect.class);

    @Around("execution(* com.githubaccess.report.service..*(..))")
    public Object logServiceCalls(ProceedingJoinPoint pjp) throws Throwable {
        String method = pjp.getSignature().toShortString();
        long start = System.currentTimeMillis();
        log.debug("→ Entering: {}", method);
        try {
            Object result = pjp.proceed();
            log.debug("← Exiting: {} [{}ms]", method, System.currentTimeMillis() - start);
            return result;
        } catch (Exception ex) {
            log.error("✗ Exception in {} [{}ms]: {}", method, System.currentTimeMillis() - start, ex.getMessage());
            throw ex;
        }
    }
}
