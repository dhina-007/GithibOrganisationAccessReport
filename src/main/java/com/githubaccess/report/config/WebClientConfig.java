package com.githubaccess.report.config;

import com.githubaccess.report.constant.GitHubApiConstants;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

@Configuration
public class WebClientConfig {

    private final GitHubProperties gitHubProperties;

    public WebClientConfig(GitHubProperties gitHubProperties) {
        this.gitHubProperties = gitHubProperties;
    }

    @Bean
    public WebClient gitHubWebClient(WebClient.Builder builder) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, gitHubProperties.getConnectTimeoutMs())
                .responseTimeout(Duration.ofMillis(gitHubProperties.getReadTimeoutMs()))
                .doOnConnected(connection -> connection
                        .addHandlerLast(new ReadTimeoutHandler(gitHubProperties.getReadTimeoutMs(), TimeUnit.MILLISECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(gitHubProperties.getReadTimeoutMs(), TimeUnit.MILLISECONDS)));

        return builder
                .baseUrl(gitHubProperties.getBaseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader(GitHubApiConstants.HEADER_ACCEPT, GitHubApiConstants.ACCEPT_JSON)
                .defaultHeader(GitHubApiConstants.HEADER_API_VERSION, GitHubApiConstants.API_VERSION)
                .filter(authorizationFilter())
                .build();
    }

    @Bean(name = "gitHubTaskExecutor")
    public Executor gitHubTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(gitHubProperties.getMaxConcurrentRequests());
        executor.setMaxPoolSize(gitHubProperties.getMaxConcurrentRequests());
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("github-api-");
        executor.initialize();
        return executor;
    }

    private ExchangeFilterFunction authorizationFilter() {
        return ExchangeFilterFunction.ofRequestProcessor(request -> {
            String token = gitHubProperties.getToken();
            if (token != null && !token.isBlank()) {
                return Mono.just(ClientRequest.from(request)
                        .header(GitHubApiConstants.HEADER_AUTHORIZATION,
                                GitHubApiConstants.BEARER_PREFIX + token)
                        .build());
            }
            return Mono.just(request);
        });
    }
}
