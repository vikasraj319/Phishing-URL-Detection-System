# PhishGuard AI

Explainable phishing detection for URLs and messages. React + TypeScript frontend, Java 25 (LTS) / Spring Boot 4.0 backend, PostgreSQL.

**Status:** the detection engine, helpers and provider-reply parsing were compiled with `--release 25` (warnings as
errors) and their self-checks pass on a real JDK 25 (Temurin 25.0.5, LTS). They also pass on JDK 26 and compile for 21.
The frontend is typechecked, built and tested. The Spring Boot 4 layer (controllers, JPA, Flyway wiring, JUnit tests)
was written but never compiled or run, because Maven Central is unreachable in the build sandbox. Run `mvn test` first
and fix anything it reports.

**Java versions:** the build targets Java 25 (`java.version` in `backend/pom.xml`). Spring Boot 4.0.x supports Java 17
through 25. To use Java 26, set `java.version` to 26 (Boot 4.0.4+ documents it) and add
`-Dnet.bytebuddy.experimental=true` to the surefire `argLine` if Mockito/Byte Buddy rejects the newer class files.

## What it does
- Scans a URL (never opened or fetched) or a pasted message with deterministic, explainable rules.
- Every finding has a rule id, severity, evidence, explanation and score contribution.
- Score 0-100 from a documented noisy-OR combination; classes SAFE-LOOKING / SUSPICIOUS / HIGH RISK
  (thresholds 30 and 60, configurable, **uncalibrated**). Safe-looking means "no strong indicators", not "safe".
- Optional Google Safe Browsing and VirusTotal lookups. A failed, timed-out or rate-limited lookup is reported as
  UNAVAILABLE and never treated as clean. A FLAGGED verdict adds a heavy finding.
- History, dashboard statistics, charts and top indicators come from PostgreSQL only.

## Windows setup
Prerequisites: JDK 25 (e.g. Temurin; set `JAVA_HOME` and check `java -version` and `mvn -v` both report 25), Maven 3.9+, Node.js 20+, PostgreSQL 16 (or Docker Desktop).

1. **Database** (pick one)
   - Docker: `$env:DB_PASSWORD="choose-a-password"; docker compose up -d db`
   - Or in psql: `CREATE USER phishguard WITH PASSWORD '...'; CREATE DATABASE phishguard OWNER phishguard;`
2. **Backend** (PowerShell, from `backend`). Spring does not read `.env`; set variables in the shell or your IDE:
   ```powershell
   $env:DB_PASSWORD = "choose-a-password"
   # optional: $env:SAFE_BROWSING_API_KEY = "..."; $env:VIRUSTOTAL_API_KEY = "..."
   mvn test
   mvn spring-boot:run
   ```
   Flyway creates the schema on start. API docs: http://localhost:8080/swagger-ui.html
3. **Frontend** (from `frontend`):
   ```powershell
   copy .env.example .env
   npm install
   npm run dev      # http://localhost:5173
   ```

## API
`POST /api/v1/scan/url` `{url}` - `POST /api/v1/scan/message` `{message, allowExternalLookup?}` -
`GET /api/v1/scans` (q,type,classification,from,to,sort,dir,page,size) - `GET|DELETE /api/v1/scans/{id}` -
`GET /api/v1/dashboard/{stats|risk-distribution|scan-activity|top-indicators}?range=7d|30d` -
`GET /api/v1/threat-intelligence/status` - `GET /api/v1/health`. Errors: `{status, code, message, fieldErrors?}`.

## Definitions
- **Detection Rate** on the dashboard = (suspicious + high risk) / total scans in the range. It is the share of scans
  that raised a flag. It is **not** an accuracy, precision or recall figure. Trends compare with the previous period
  of equal length and are shown only when that period has scans.
- Windows are UTC calendar days including today.

## Privacy and security
- URLs are normalised and analysed locally; the server never connects to a submitted URL. Provider calls go only to
  the fixed Safe Browsing and VirusTotal hosts, and only if a key is set. With a key set, scanned URLs are sent to that provider.
- Message text is analysed locally. Links from a message go to providers only when the request sets
  `allowExternalLookup: true` (the bundled UI does not).
- Stored: a redacted summary (links, emails, long numbers masked; URLs lose credentials and query), a SHA-256 of the
  input, the findings (evidence may quote short matched phrases, max 120 chars), links found, provider statuses.
  The full message is never stored. `RETENTION_DAYS` (default 30, 0 = keep) purges old scans nightly; users can delete any scan.
- Input limits: URL 2048 chars, message 10,000 chars, request body 64 KB; per-IP rate limits (default 30 scans/min).
- Responses never include stack traces; submitted input is not logged.
- **No authentication in this build.** History is shared by everyone who can reach the API. Run it on localhost or a
  private network only; add auth and per-user ownership before any public deployment.

## Known limitations
- Rule weights, thresholds and the brand list are starting points. No labeled dataset has been used, so **no accuracy
  claims are made**. Calibrate with labeled data (precision, recall, F1, false positives/negatives) before relying on it.
- Registered-domain logic is approximate (no full Public Suffix List). No DNS, certificate or domain-age signals.
- `AiClassificationService` is an extension point only; no ML model ships and none is blended into scores.
- No integration tests against a real PostgreSQL (e.g. Testcontainers) yet; `scan-activity` uses a Postgres-specific query.
- Spring Boot 4 / Spring Framework 7 / Hibernate 7 / Jackson 3 are new major versions that I could not compile against.
  springdoc 3.0.3 targets Boot 4.0.5; Boot 4.1.x is not verified with it.
- Mockito self-attaches an agent for inline mocks; `pom.xml` passes `-XX:+EnableDynamicAgentLoading` to keep test output clean.
- Rate limiting is in-memory per instance.
