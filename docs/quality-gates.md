# Quality Gates

This project uses one canonical GitHub Actions workflow and Maven checks to keep changes reviewable before they reach integration and release branches.

## Current Pipeline

The canonical Maven workflow is `.github/workflows/maven.yml` (`Java CI with Maven`).

- Pull requests targeting any branch run the QA checks, including integration branches such as `backend-frontend/merger`.
- Pushes to `main` or `develop` run QA checks and are the only events that can deploy the Docker image to GHCR.
- Pushes to other branches do not run this workflow unless opened as a pull request.

- Build command: `mvn -B clean verify`
- Java runtime: Temurin JDK 21
- Test report artifact: `target/surefire-reports/`
- Coverage report artifact: `target/site/jacoco/`
- Build artifact: `target/*.jar`

The workflow jobs are:

- `Unit tests`
- `Integration tests`
- `Build and coverage`
- `Dependency review` on pull requests only
- `Docker image and smoke test` on pull requests and deployable pushes to `main` or `develop`
- `Sonar static analysis` when configured and not running from a forked pull request
- `Deploy Docker image to GHCR` on pushes to `main` or `develop` only

Dependency Review runs on pull requests. New dependencies with high or critical known vulnerabilities should be fixed before merge.

## Sonar Static Analysis

The workflow includes SonarQube/SonarCloud support without hardcoding secrets. Pull requests from forks do not run this job because repository secrets are not exposed to forked workflows.

On same-repository pull requests and pushes, the `Sonar static analysis` job runs after `Build and coverage`. If `SONAR_TOKEN` is missing, the job exits successfully with a notice and skips analysis. Once the secret is configured, Sonar imports the JaCoCo XML report and waits for the Sonar quality gate result.

Configure this repository secret to enable analysis:

- Secret: `SONAR_TOKEN`

Generate the token from a SonarCloud user that can run analysis on the `jdemic-e2_jdemic` project. If Sonar analysis fails with a 401/403/404 after the token is present, regenerate the token from a user that belongs to the `jdemic-e2` SonarCloud organization and has permission to execute analysis for the project.

The workflow defaults to the SonarCloud project `jdemic-e2_jdemic` in organization `jdemic-e2`. Override these only if the Sonar project changes:

- Variable: `SONAR_PROJECT_KEY`
- Variable for SonarCloud: `SONAR_ORGANIZATION`
- Optional variable for self-hosted SonarQube: `SONAR_HOST_URL`

If `SONAR_HOST_URL` is not set, the workflow uses `https://sonarcloud.io`. Once Sonar is configured, a failed Sonar quality gate fails CI.

## Coverage

JaCoCo runs during `mvn verify`, generates HTML/XML/CSV reports under `target/site/jacoco/`, and enforces a temporary 15% bundle instruction coverage baseline.

The baseline is intentionally modest because the current test suite is still growing. Its job is to keep coverage from silently regressing while the team adds focused tests for game logic, networking, and security code. Raise the threshold in small steps after meaningful tests land.

Coverage checks exclude non-business-code entry points and UI-only classes:

- `Launcher*`
- `jdemic/Main*`
- `jdemic/Scenes/**/*`
- `jdemic/ui/**/*`

Sonar consumes `target/site/jacoco/jacoco.xml` from the `jacoco-reports` artifact after the Maven build job succeeds. The Sonar job fails if that XML report is missing.

## Branch Protection Recommendations

Apply these rules to `main` and `develop` in GitHub branch protection settings:

- Require a pull request before merging.
- Require at least one approving review.
- Dismiss stale approvals when new commits are pushed.
- Require conversation resolution before merge.
- Require status checks to pass before merge.
- Require branches to be up to date before merge.
- Require `Unit tests`.
- Require `Integration tests`.
- Require `Build and coverage`.
- Require `Dependency review` for pull requests.
- Require `Docker image and smoke test` for pull requests that touch Docker or server runtime behavior.
- Require `Sonar static analysis` after the `SONAR_TOKEN` secret is configured and the team wants Sonar to block merges.
- Block force pushes and branch deletion.
- Restrict direct pushes to maintainers if the team needs tighter release control.

## Local Commands

Run the same Maven gate locally before opening a PR:

```bash
mvn -B clean verify
```

Use the generated report at `target/site/jacoco/index.html` to inspect coverage locally.
