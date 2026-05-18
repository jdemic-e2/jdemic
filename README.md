# jDemic

jDemic is a Java 21 multiplayer board-game project inspired by *Pandemic*. The project combines a JavaFX client UI, game logic, dedicated server networking, security-focused networking features, automated tests, CI/CD quality gates, and Docker support for running the dedicated server.

The repository is organized as a team project, with separate areas for frontend/UI, backend/game logic, security, QA/DevOps, architecture diagrams, and supporting documentation.

## Features and Modules

- **JavaFX frontend/UI**: menu, lobby, gameplay, settings, tutorial, and reusable UI components.
- **Game logic**: map graph, players, cards, disease state, actions, turn handling, win/lose conditions, and game state models.
- **Dedicated server and networking**: packet-based communication, client handling, game client integration, server runtime configuration, and server status endpoints.
- **Security-related networking**: packet sanitization, secure connection handling, rate limiting, heartbeat/session monitoring, and state masking tests.
- **QA and testing**: JUnit 5 unit tests, integration-test setup, frontend/TestFX tests, E2E test files, JaCoCo coverage, and CI artifacts.
- **DevOps support**: GitHub Actions workflows, Dependency Review, optional Sonar analysis, Docker smoke testing, GHCR image deployment, and branch-protection documentation.
- **Docker dedicated server**: Dockerfile, Compose setup, environment-based runtime configuration, and health checks.

## Repository Structure

```text
.
├── src/                         # Main Java source code and resources
│   ├── Launcher.java
│   ├── jdemic/
│   │   ├── DedicatedServer/      # Dedicated server, networking, security, transport, sessions
│   │   ├── GameLogic/            # Core game model, actions, map, cards, players, state
│   │   ├── Scenes/               # JavaFX scenes: main menu, lobby, play, settings, tutorial
│   │   ├── ui/                   # Shared JavaFX UI utilities and gameplay UI components
│   │   └── Main.java             # JavaFX application entry point
│   └── resources/                # Images, audio, fonts, cards, icons, settings
├── test/                         # Automated tests
│   ├── integration/              # Integration tests
│   └── jdemic/                   # Unit, frontend, networking, security, and E2E tests
├── docs/                         # Project documentation and quality-gate docs
├── backend/                      # Backend documentation and diagrams
├── front-end/                    # Frontend documentation, backlog, C4/UML diagrams
├── security/                     # Security backlog and architecture diagrams
├── devops/                       # DevOps diagrams and workflow images
├── .github/                      # GitHub Actions, PR template, Dependabot config
├── Dockerfile.dedicated-server   # Dedicated server Docker image
├── docker-compose.yml            # Local dedicated server Compose setup
├── pom.xml                       # Maven build configuration
└── README.md
```

> Note: this project uses a custom Maven layout: `src` is the main source directory and `test` is the test source directory.

## Requirements

- **Java 21**
- **Maven 3.9+** recommended
- **Docker** and **Docker Compose** for dedicated-server container usage
- A desktop environment for running the JavaFX client locally
- On Linux CI/headless environments, JavaFX/TestFX tests may require `xvfb`

Check Java and Maven versions:

```bash
java -version
mvn -version
```

## Build

Compile and verify the project with Maven:

```bash
mvn -B clean verify
```

For a faster test-only run:

```bash
mvn -B clean test
```

Build the JAR without running tests:

```bash
mvn -B -DskipTests package
```

## Run the JavaFX Application

Run the main JavaFX client with Maven:

```bash
mvn javafx:run
```

The JavaFX Maven plugin is configured to launch `jdemic.Main`.

## Run Tests

Run the default automated test suite:

```bash
mvn -B clean test
```

Run the full Maven verification gate, including coverage checks configured in the build:

```bash
mvn -B clean verify
```

Run a focused test class when debugging a specific failure:

```bash
mvn -B -Dtest=ClassNameTest test
```

E2E tests live under `test/jdemic/e2e`. They are isolated from the default Surefire test run in the Maven configuration, and some E2E/UI tests may be disabled when they document known gameplay or JavaFX/TestFX issues. Check the test annotations and comments before relying on them as release blockers.

## Run the Dedicated Server Locally

The dedicated server entry point is:

```text
jdemic.DedicatedServer.network.core.JdemicNetworkServer
```

Build the application and copy runtime dependencies:

```bash
mvn -B -DskipTests package dependency:copy-dependencies
```

Run the server on Linux/macOS:

```bash
java -cp "target/jdemic-engine-1.0-SNAPSHOT.jar:target/dependency/*" jdemic.DedicatedServer.network.core.JdemicNetworkServer
```

On Windows, use `;` instead of `:` in the classpath:

```powershell
java -cp "target/jdemic-engine-1.0-SNAPSHOT.jar;target/dependency/*" jdemic.DedicatedServer.network.core.JdemicNetworkServer
```

### Dedicated Server Environment Variables

| Variable | Default | Purpose |
| --- | --- | --- |
| `JDEMIC_SERVER_PORT` | `9000` | TCP port for game client connections |
| `JDEMIC_STATUS_ENABLED` | `true` | Enables the HTTP status/health server |
| `JDEMIC_STATUS_HOST` | `localhost` locally, `0.0.0.0` in Docker | Status server bind host |
| `JDEMIC_STATUS_PORT` | `8080` | HTTP status/health port |
| `JDEMIC_OPEN_BROWSER` | `false` | Opens the status page only when explicitly enabled |

## Docker Dedicated Server

Validate the Compose configuration:

```bash
docker compose -f docker-compose.yml config
```

Build the dedicated server image:

```bash
docker build -f Dockerfile.dedicated-server -t jdemic-dedicated-server .
```

Run the dedicated server with Docker Compose:

```bash
docker compose up --build dedicated-server
```

The Compose setup exposes:

- Game server: `localhost:9000`
- Status/health server: `localhost:8080`

Check the health endpoint:

```bash
curl http://localhost:8080/health
```

Stop the local container:

```bash
docker compose down
```

For more details, see [`docs/docker-dedicated-server.md`](docs/docker-dedicated-server.md).

## QA, CI, and Coverage

The project uses GitHub Actions and Maven quality gates for pull requests and integration branches. The CI setup includes some or all of the following checks, depending on the active workflow version:

- Java 21 Maven build and test execution
- Unit test reports uploaded as CI artifacts
- Integration test support
- JaCoCo coverage reports and coverage gate
- Dependency Review on pull requests
- Optional SonarQube/SonarCloud static analysis when repository secrets/variables are configured
- Docker image build and dedicated-server smoke test
- GHCR image deployment on eligible branch pushes

Useful local commands before opening a PR:

```bash
mvn -B clean test
mvn -B clean verify
docker compose -f docker-compose.yml config
```

When Docker files or server runtime behavior change, also run:

```bash
docker build -f Dockerfile.dedicated-server -t jdemic-dedicated-server .
docker compose up --build dedicated-server
```

For quality-gate details and recommended branch-protection settings, see [`docs/quality-gates.md`](docs/quality-gates.md).

## Security and Networking

The networking/security area includes packet validation, secure connection handling, rate limiting, heartbeat/session monitoring, state masking, and server-side handling of client/game packets.

Security-related expectations for contributors:

- Do not commit secrets, tokens, credentials, or local machine paths.
- Keep debug/status endpoints safe when bound to public interfaces such as `0.0.0.0`.
- Do not expose private player/card data in public status or debug responses.
- Add or update focused tests when changing packet processing, sanitization, secure connections, session handling, or state masking.
- Run the relevant security/networking tests before opening a PR.

## Documentation

Detailed documentation and diagrams are split across the repository:

- [`docs/quality-gates.md`](docs/quality-gates.md) — CI, coverage, quality gates, and branch-protection recommendations
- [`docs/docker-dedicated-server.md`](docs/docker-dedicated-server.md) — dedicated server Docker runtime
- [`docs/trello-quality-gates.md`](docs/trello-quality-gates.md) — Trello-to-GitHub quality gate mapping
- [`backend/`](backend/) — backend documentation, backlog, UML/C4 diagrams
- [`front-end/`](front-end/) — frontend backlog, C4 diagrams, UML diagrams
- [`security/`](security/) — security backlog and project diagrams
- [`devops/`](devops/) — DevOps process and architecture images

## Contributing and PR Checklist

Before opening or merging a pull request:

- Target the intended branch and keep the scope focused.
- Run the relevant local checks:

  ```bash
  mvn -B clean test
  mvn -B clean verify
  ```

- If Docker/server behavior changed, validate Docker Compose and the dedicated server image.
- Add or update tests for changed behavior.
- Check coverage impact when modifying core logic, networking, or security code.
- Update documentation for workflow, runtime, security, or operational changes.
- Confirm no secrets, generated local files, IDE files, or machine-specific paths are committed.
- Make sure CI passes and required review conversations are resolved.

The repository includes a pull request template under `.github/` with CI, security, and quality checklist items.
