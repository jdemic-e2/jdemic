# Trello to GitHub Quality Gates Mapping

This document maps the current DevOps and Security board items to GitHub controls. It focuses on pipeline and review gates only; gameplay, backend behavior, frontend scenes, resources, E2E tests, integration tests, and coverage thresholds are tracked in separate PRs.

| Trello area | Board item | GitHub quality gate | Status |
| --- | --- | --- | --- |
| DevOps Team | GitHub Repository Setup | Repository workflows, pull requests, branch protection settings | Existing repository setup, expanded by this PR |
| DevOps Team | Additional Branches | Protect `main` and `develop`; require PRs for integration branches | Documented in `docs/quality-gates.md` |
| DevOps Team | Branch Protection rules | Required checks, reviews, stale approval dismissal, no force pushes | Documented in `docs/quality-gates.md` |
| DevOps Team | CI Build Pipeline | `mvn -B clean verify` on pushes and pull requests | Existing workflow, preserved by this PR |
| DevOps Team | Unit Test Integration | Surefire reports from Maven tests | Existing test lifecycle, reports uploaded by this PR |
| DevOps Team | API & Integration Testing Setup | Failsafe/integration test setup | Tracked by PR #35; not duplicated here |
| DevOps Team | Conflict Resolution Protocol | Up-to-date branch requirement and conversation resolution before merge | Recommended branch protection setting |
| Security Team | Sanitizarea Datelor | Unit and security tests, Dependency Review, Sonar static analysis | Dependency Review and Sonar support added here |
| Security Team | Prevenirea Flood-ului / Rate Limiting | Static analysis and focused tests for networking/security code | Sonar support added here; behavior tests remain separate |
| Security Team | Monitorizarea Sesiunii / Heartbeat | Static analysis and Maven test gate | Existing Maven gate, Sonar support added here |
| Security Team | Information Masking | Static analysis and focused security tests | Sonar support added here; masking behavior remains app-code work |
| Cross-team | Dependabot dependency updates | Dependabot Maven and GitHub Actions updates | Tracked by PR #35; not duplicated here |
| Cross-team | Code coverage threshold | JaCoCo report and coverage gate | Tracked by PR #39; not duplicated here |
| Cross-team | E2E testing setup | TestFX/E2E test infrastructure | Tracked by PR #38; not duplicated here |

## Merge Readiness Definition

A PR is ready for merge when:

- Its scope matches the PR title and target branch.
- Maven verification passes.
- Dependency Review has no unresolved high or critical new vulnerabilities.
- Sonar analysis passes when repository configuration is enabled.
- Required reviews and conversations are resolved.
- Any separate quality-gate PRs that the change depends on are merged or explicitly called out.
