# Quality Gates

This project uses GitHub Actions and Maven checks to keep changes reviewable before they reach `main` or `develop`.

## Current Pipeline

The main Maven workflow runs on pushes and pull requests targeting `main` or `develop`.

- Build command: `mvn -B clean verify`
- Java runtime: Temurin JDK 21
- Test report artifact: `target/surefire-reports/`
- Coverage report artifact: `target/site/jacoco/`
- Build artifact: `target/*.jar`

The workflow now also runs Dependency Review on pull requests. New dependencies with high or critical known vulnerabilities should be fixed before merge.

## Sonar Static Analysis

The workflow includes SonarQube/SonarCloud support without hardcoding secrets. On same-repository pull requests and pushes, the Sonar job is expected to run and fail clearly if required configuration is missing. Pull requests from forks do not run this job because repository secrets are not exposed to forked workflows.

Configure this repository secret:

- Secret: `SONAR_TOKEN`

The workflow defaults to the SonarCloud project `jdemic-e2_jdemic` in organization `jdemic-e2`. Override these only if the Sonar project changes:

- Variable: `SONAR_PROJECT_KEY`
- Variable for SonarCloud: `SONAR_ORGANIZATION`
- Optional variable for self-hosted SonarQube: `SONAR_HOST_URL`

If `SONAR_HOST_URL` is not set, the workflow uses `https://sonarcloud.io`. Sonar waits for the quality gate result, so a failed quality gate fails CI.

## Coverage

JaCoCo runs during `mvn verify`, generates HTML/XML/CSV reports under `target/site/jacoco/`, and enforces a temporary 15% bundle instruction coverage baseline.

The baseline is intentionally modest because the current test suite is still growing. Its job is to keep coverage from silently regressing while the team adds focused tests for game logic, networking, and security code. Raise the threshold in small steps after meaningful tests land.

Coverage checks exclude non-business-code entry points and UI-only classes:

- `Launcher*`
- `jdemic/Main*`
- `jdemic/Scenes/**/*`
- `jdemic/ui/**/*`
- legacy executable test harnesses under `jdemic/DedicatedServer/*Test*`
- `jdemic/DedicatedServer/TestClient*`

Sonar consumes `target/site/jacoco/jacoco.xml` from the `jacoco-reports` artifact after the Maven build job succeeds. The Sonar job fails if that XML report is missing.

## Branch Protection Recommendations

Apply these rules to `main` and `develop` in GitHub branch protection settings:

- Require a pull request before merging.
- Require at least one approving review.
- Dismiss stale approvals when new commits are pushed.
- Require conversation resolution before merge.
- Require status checks to pass before merge.
- Require branches to be up to date before merge.
- Require the `build` workflow job.
- Require `Dependency review` for pull requests.
- Require `Sonar static analysis` after the `SONAR_TOKEN` secret is configured.
- Block force pushes and branch deletion.
- Restrict direct pushes to maintainers if the team needs tighter release control.

## Local Commands

Run the same Maven gate locally before opening a PR:

```bash
mvn -B clean verify
```

Use the generated report at `target/site/jacoco/index.html` to inspect coverage locally.
