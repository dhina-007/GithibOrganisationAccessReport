# GitHub Organization Access Report

Spring Boot service that authenticates with GitHub, scans an organization's repositories, and returns a JSON access report mapping **users → repositories** (with permission details).

## Requirements

- Java 17+
- Maven 3.9+
- A GitHub Personal Access Token (PAT) with permission to list organization repositories and collaborators

## How authentication is configured

The app authenticates to the GitHub REST API using a **Personal Access Token** supplied via environment variable (never hard-code secrets):

```bash
export GITHUB_TOKEN=ghp_your_token_here
```

Recommended token scopes (classic PAT):

- `read:org` — list organization membership/resources
- `repo` — list private repositories and collaborators (org admin / repo admin rights are typically required to list collaborators)

Fine-grained PAT: grant the organization access with **Repository permissions → Collaborators: Read** and **Metadata: Read**, plus access to the repositories you want included.

The token is sent as `Authorization: Bearer <token>` on outbound GitHub calls. It is never returned in API responses and is masked in application logs.

## How to run

```bash
export GITHUB_TOKEN=ghp_your_token_here
./mvnw spring-boot:run
```

Or with Maven installed:

```bash
mvn spring-boot:run
```

The API starts on [http://localhost:8080](http://localhost:8080).


### Swagger UI

Interactive docs: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

OpenAPI JSON: [http://localhost:8080/api-docs](http://localhost:8080/api-docs)

### Health check

```bash
curl -s "http://localhost:8080/github/health"
# {"status":"UP","application":"github-access-report"}

curl -s "http://localhost:8080/actuator/health"
# {"status":"UP"}
```

## How to call the API

### JSON access report

```bash
curl -s "http://localhost:8080/github/organization/{org}/access-report" | jq
```

Example:

```bash
curl -s "http://localhost:8080/github/organization/octokit/access-report" | jq
```

### PDF access report

Downloads a structured PDF (summary + per-user repository tables with permission details):

```bash
curl -L -o access-report.pdf \
  "http://localhost:8080/github/organization/{org}/access-report/pdf"
```

Example:

```bash
curl -L -o access-report-dhina-project.pdf \
  "http://localhost:8080/github/organization/dhina-project/access-report/pdf"
```

The PDF includes:
- Cover header with organization and generation timestamp
- Summary cards (users, repositories, access mappings)
- Permission level legend
- Per-user sections with a table of repository, permission, and access details
- Page footer with organization name and page numbers

### Sample response

```json
{
  "organization": "acme",
  "generatedAt": "2026-07-11T06:00:00Z",
  "totalUsers": 1,
  "totalRepositories": 1,
  "users": [
    {
      "login": "alice",
      "repositories": [
        {
          "name": "api",
          "fullName": "acme/api",
          "permission": "admin",
          "roleName": "admin",
          "accessDescription": "Admin access — can manage settings, collaborators, and all repository content",
          "permissions": {
            "admin": true,
            "maintain": true,
            "write": true,
            "triage": true,
            "read": true
          }
        }
      ]
    }
  ]
}
```

## Configuration

| Property | Env / default | Description |
|----------|---------------|-------------|
| `github.token` | `GITHUB_TOKEN` | GitHub PAT |
| `github.base-url` | `https://api.github.com` | API base URL |
| `github.max-concurrent-requests` | `15` | Parallel collaborator fetches |
| `github.per-page` | `100` | GitHub pagination page size |
| `github.retry.max-attempts` | `3` | Retries on technical failures |

Profile: `dev` is active by default (`application-dev.yml`).

## Design decisions

1. **No database** — the report is generated live from GitHub. The assignment asks for visibility into current access, not historical storage.
2. **Aggregation model** — GitHub is queried as repo → collaborators, then inverted to user → repositories for the response shape required by the assignment.
3. **Scale (100+ repos / 1000+ users)** — repository collaborator calls run in parallel on a bounded thread pool (`max-concurrent-requests`) to avoid sequential N+1 latency while limiting rate-limit pressure. Pagination uses `per_page=100` and follows `Link: rel="next"`.
4. **Resilience** — technical failures (timeouts, 5xx, 429) retry up to 3 times with incremental backoff. 401/403/404 map to clear business errors.
5. **Layered structure** — controller → service → GitHub client, with DTOs, mapper, aggregator, and a global exception handler.

## Assumptions

- The configured token can list the target org's repositories and each repository's collaborators (`affiliation=all`).
- Users returned by the collaborators API (including outside collaborators and users with access via teams, when GitHub includes them) are the source of truth for “who has access”.
- Organization login must match GitHub's login rules (validated on the path parameter).

## Tests

```bash
./mvnw test
```

## Project structure

```
com.githubaccess.report
├── config          # WebClient, properties, OpenAPI, AOP
├── controller      # REST endpoints
├── constant        # API constants
├── dto             # Request/response models
├── client          # GitHub REST client
├── enums           # Permission levels
├── exception       # Domain/technical errors + handler
├── mapper          # GitHub payload → report DTOs
└── service         # Business logic + aggregation
```

## Docker deployment

### Files

| File | Purpose |
|------|---------|
| `Dockerfile` | Multi-stage build (JDK 17 build → JRE 17 runtime) |
| `docker-compose.yml` | One-command local/prod-style run |
| `.env.example` | Template for secrets and ports |
| `.dockerignore` | Keeps build context small and excludes secrets |

### Prerequisites

- Docker 24+ and Docker Compose v2
- A valid `GITHUB_TOKEN`

### Quick start (Compose)

```bash
# 1. Create env file from template
cp .env.example .env

# 2. Edit .env and set your real token
# GITHUB_TOKEN=ghp_xxxxxxxx

# 3. Build and start
docker compose up -d --build

# 4. Check health
curl -s http://localhost:8080/actuator/health
curl -s http://localhost:8080/github/health

# 5. Call APIs
curl -s "http://localhost:8080/github/organization/dhina-project/access-report" | jq
curl -L -o access-report.pdf \
  "http://localhost:8080/github/organization/dhina-project/access-report/pdf"

# 6. Stop
docker compose down
```

Swagger (when running in Docker): [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

### Build and run with Docker only

```bash
docker build -t github-access-report:1.0.0 .

docker run -d \
  --name github-access-report \
  -p 8080:8080 \
  -e GITHUB_TOKEN=ghp_your_token_here \
  -e SPRING_PROFILES_ACTIVE=prod \
  github-access-report:1.0.0
```

### Environment variables

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `GITHUB_TOKEN` | Yes | — | GitHub PAT used for API calls |
| `SPRING_PROFILES_ACTIVE` | No | `prod` (in image) | Spring profile |
| `SERVER_PORT` | No | `8080` | Host port mapping / app port |
| `GITHUB_MAX_CONCURRENT_REQUESTS` | No | `15` | Parallel GitHub collaborator fetches |
| `JAVA_OPTS` | No | container-aware heap | JVM options |

### Deployment notes

1. **Never bake the token into the image.** Pass `GITHUB_TOKEN` at runtime via `-e`, Compose `.env`, or your platform’s secret store (Kubernetes Secret, AWS Secrets Manager, etc.).
2. **Do not commit `.env`.** It is gitignored; only `.env.example` is tracked.
3. **Health checks:** container and Compose use `/actuator/health`. Use this for load balancer / Kubernetes readiness probes.
4. **Resources:** the image sets `-XX:MaxRAMPercentage=75.0`. For large orgs (100+ repos), give the container at least **512MB–1GB RAM**.
5. **Outbound network:** the container must reach `https://api.github.com` (and any GitHub Enterprise host if you change `github.base-url`).
6. **Production profile:** Docker defaults to `SPRING_PROFILES_ACTIVE=prod` (`application-prod.yml`).
7. **Reverse proxy:** put Nginx/Traefik/ALB in front if you need TLS; the app listens on HTTP `8080` inside the container.
8. **Rolling updates:** build a new tag, pull/restart Compose or update your orchestrator deployment; no database migration is required.
9. **Logs:** `docker compose logs -f github-access-report` — tokens are masked in request/response logs.
10. **Security:** run as non-root (`appuser`) in the image; rotate PATs regularly; prefer fine-grained tokens scoped to one org.

### Example Kubernetes snippet (optional)

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: github-access-report
spec:
  replicas: 1
  selector:
    matchLabels:
      app: github-access-report
  template:
    metadata:
      labels:
        app: github-access-report
    spec:
      containers:
        - name: app
          image: github-access-report:1.0.0
          ports:
            - containerPort: 8080
          env:
            - name: SPRING_PROFILES_ACTIVE
              value: prod
            - name: GITHUB_TOKEN
              valueFrom:
                secretKeyRef:
                  name: github-access-secrets
                  key: token
          readinessProbe:
            httpGet:
              path: /actuator/health
              port: 8080
            initialDelaySeconds: 30
            periodSeconds: 10
          livenessProbe:
            httpGet:
              path: /actuator/health
              port: 8080
            initialDelaySeconds: 40
            periodSeconds: 20
          resources:
            requests:
              memory: "512Mi"
              cpu: "250m"
            limits:
              memory: "1Gi"
              cpu: "1000m"
```
