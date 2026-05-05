# Quality Gates

This project uses GitHub Actions and Maven checks to keep changes reviewable before they reach `main` or `develop`.

## Current Pipeline

The main Maven workflow runs on pushes and pull requests targeting `main` or `develop`.

- Build command: `mvn -B clean verify`
- Java runtime: Temurin JDK 21
- Test report artifact: `target/surefire-reports/`
- Build artifact: `target/*.jar`

The workflow now also runs Dependency Review on pull requests. New dependencies with high or critical known vulnerabilities should be fixed before merge.

## Sonar Static Analysis

The workflow includes SonarQube/SonarCloud support without hardcoding secrets. The Sonar job skips cleanly when configuration is missing.

Configure these repository settings to enable it:

- Secret: `SONAR_TOKEN`
- Variable: `SONAR_PROJECT_KEY`
- Variable for SonarCloud: `SONAR_ORGANIZATION`
- Optional variable for self-hosted SonarQube: `SONAR_HOST_URL`

If `SONAR_HOST_URL` is not set, the workflow defaults to `https://sonarcloud.io`.

## Coverage

JaCoCo coverage thresholds are intentionally not configured here because coverage gating is tracked in PR #39. Once that work is merged, add `target/site/jacoco/**` and `target/site/jacoco/jacoco.xml` as workflow artifacts and document the active threshold here.

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
- Require `Sonar static analysis` after Sonar secrets and variables are configured.
- Block force pushes and branch deletion.
- Restrict direct pushes to maintainers if the team needs tighter release control.

## Local Commands

Run the same Maven gate locally before opening a PR:

```bash
mvn -B clean verify
```

After JaCoCo is merged, use the generated report at `target/site/jacoco/index.html` to inspect coverage locally.
