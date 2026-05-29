# Dedicated Server Docker Runtime

The Docker image runs the master orchestrator. The orchestrator listens for `HOST` requests from the client and starts one dedicated game server process per hosted lobby.

## Environment Variables

| Variable | Local default | Docker recommendation | Description |
| --- | --- | --- | --- |
| `JDEMIC_ORCHESTRATOR_PORT` | `8080` | `8080` | TCP control port for `HOST` requests and HTTP `/health`. |
| `JDEMIC_SERVER_PORT_MIN` | `9001` | `9001` | First TCP port the orchestrator can assign to a game server. |
| `JDEMIC_SERVER_PORT_MAX` | `9010` | Match the exposed range | Last TCP port the orchestrator can assign to a game server. |

## Build

```bash
docker build -f Dockerfile.dedicated-server -t jdemic-dedicated-server .
```

The runtime image uses Eclipse Temurin Java 21, runs the Java process as the
non-root `jdemic` user, and includes a container `HEALTHCHECK` against the
orchestrator `/health` endpoint.

## Run

```bash
docker run --rm \
  -p 8080:8080 \
  -p 9001-9010:9001-9010 \
  -e JDEMIC_ORCHESTRATOR_PORT=8080 \
  -e JDEMIC_SERVER_PORT_MIN=9001 \
  -e JDEMIC_SERVER_PORT_MAX=9010 \
  jdemic-dedicated-server
```

## Docker Compose

Use Compose for local dedicated-server development:

```bash
docker compose up --build dedicated-server
```

The compose file builds `Dockerfile.dedicated-server`, exposes the orchestrator on `localhost:8080`, and exposes game server ports `localhost:9001` through `localhost:9010`.

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
{"status":"ok","service":"jdemic-master-orchestrator","activeServers":0,"serverPortMin":9001,"serverPortMax":9010}
```

To request a new game server from the orchestrator, the client opens a TCP connection to port `8080` and sends `HOST`. The response is `SUCCESS:<port>` when a game server was started.

The orchestrator handles `SIGTERM` through a JVM shutdown hook and destroys spawned game server processes.

## CI and GHCR Deployment

The Maven QA workflow builds the dedicated server image and runs the Docker smoke test before any image is pushed to GitHub Container Registry. The smoke test starts the container, polls `/health`, sends a `HOST` request, verifies that the returned TCP game port accepts a connection, and then stops the container cleanly.

GHCR deployment runs only on pushes to `main` or `develop` and uses the built-in `GITHUB_TOKEN`. Images are tagged with:

- the branch/channel name, such as `main` or `develop`
- the commit SHA, such as `sha-<short-sha>`
- `latest` only for pushes to `main`
