package com.githubaccess.report.constant;

public final class GitHubApiConstants {

    private GitHubApiConstants() {
    }

    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String HEADER_ACCEPT = "Accept";
    public static final String HEADER_API_VERSION = "X-GitHub-Api-Version";
    public static final String HEADER_LINK = "Link";
    public static final String HEADER_RATE_LIMIT_REMAINING = "X-RateLimit-Remaining";
    public static final String HEADER_RETRY_AFTER = "Retry-After";

    public static final String ACCEPT_JSON = "application/vnd.github+json";
    public static final String API_VERSION = "2022-11-28";
    public static final String BEARER_PREFIX = "Bearer ";

    public static final String PATH_ORG_REPOS = "/orgs/{org}/repos";
    public static final String PATH_REPO_COLLABORATORS = "/repos/{owner}/{repo}/collaborators";

    public static final String QUERY_PER_PAGE = "per_page";
    public static final String QUERY_PAGE = "page";
    public static final String QUERY_AFFILIATION = "affiliation";
    public static final String AFFILIATION_ALL = "all";

    public static final String LINK_REL_NEXT = "next";
}
