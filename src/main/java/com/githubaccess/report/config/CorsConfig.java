package com.githubaccess.report.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final CorsProperties corsProperties;

    public CorsConfig(CorsProperties corsProperties) {
        this.corsProperties = corsProperties;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        List<String> origins = expandOrigins(corsProperties.getAllowedOrigins());

        var mapping = registry.addMapping("/**")
                .allowedMethods(corsProperties.getAllowedMethods().toArray(String[]::new))
                .allowedHeaders(corsProperties.getAllowedHeaders().toArray(String[]::new))
                .allowCredentials(corsProperties.isAllowCredentials())
                .maxAge(corsProperties.getMaxAgeSeconds());

        if (origins.size() == 1 && "*".equals(origins.get(0))) {
            mapping.allowedOriginPatterns("*");
        } else {
            mapping.allowedOrigins(origins.toArray(String[]::new));
        }
    }

    private static List<String> expandOrigins(List<String> configuredOrigins) {
        return configuredOrigins.stream()
                .flatMap(value -> Arrays.stream(value.split(",")))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();
    }
}
