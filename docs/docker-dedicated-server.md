# Dedicated Server Docker Runtime

The dedicated server can be configured entirely through environment variables, which lets the same code run locally and in containers.

## Environment Variables

| Variable | Local default | Docker recommendation | Description |
| --- | --- | --- | --- |
| `JDEMIC_SERVER_PORT` | `9000` | `9000` | TCP port for game client connections. |
| `JDEMIC_STATUS_ENABLED` | `true` | `true` | Enables the lightweight HTTP status server. |
| `JDEMIC_STATUS_HOST` | `localhost` | `0.0.0.0` | Bind address for the status server. Containers should bind all interfaces. |
| `JDEMIC_STATUS_PORT` | `8080` | `8080` | HTTP port for status and health checks. |
| `JDEMIC_OPEN_BROWSER` | `false` | `false` | Opens the local health page only when explicitly enabled and not headless. |

## Build

```bash
docker build -f Dockerfile.dedicated-server -t jdemic-dedicated-server .
```

The runtime image uses Eclipse Temurin Java 21, runs the Java process as the
non-root `jdemic` user, and includes a container `HEALTHCHECK` against
`/health`. The healthcheck is skipped only when `JDEMIC_STATUS_ENABLED=false`.

## Run

```bash
docker run --rm \
  -p 9000:9000 \
  -p 8080:8080 \
  -e JDEMIC_SERVER_PORT=9000 \
  -e JDEMIC_STATUS_ENABLED=true \
  -e JDEMIC_STATUS_HOST=0.0.0.0 \
  -e JDEMIC_STATUS_PORT=8080 \
  -e JDEMIC_OPEN_BROWSER=false \
  jdemic-dedicated-server
```

## Docker Compose

Use Compose for local dedicated-server development:

```bash
docker compose up --build dedicated-server
```

The compose file builds `Dockerfile.dedicated-server`, exposes the game server on `localhost:9000`, and exposes the status server on `localhost:8080`.

Stop and remove the local container with:

```bash
docker compose down
```

## Health Check

```bash
curl http://localhost:8080/health
```

Expected response:

```json
{"status":"ok","service":"jdemic-dedicated-server","gameServerPort":9000}
```

The process handles `SIGTERM` through a JVM shutdown hook, closes the game server socket, stops the status server, and closes active client sockets.

## CI and GHCR Deployment

The Maven QA workflow builds the dedicated server image and runs the Docker smoke test before any image is pushed to GitHub Container Registry. The smoke test starts the container, polls `/health`, verifies that the TCP game port accepts a connection, and then stops the container cleanly.

GHCR deployment runs only on pushes to `main` or `develop` and uses the built-in `GITHUB_TOKEN`. Images are tagged with:

- the branch/channel name, such as `main` or `develop`
- the commit SHA, such as `sha-<short-sha>`
- `latest` only for pushes to `main`
