# Laboratory Course – Cloud & DevOps (Semester I)

## Weekly Plan (Course + Lab)

| Week | Course (Theory)                                                         | Laboratory (Practice)                                                       | Notes               |
| ---- | ----------------------------------------------------------------------- | --------------------------------------------------------------------------- | ------------------- |
| 1    | Introduction to Cloud & DevOps. Cloud-native paradigm.                  | L1: Hello World with containers (Docker/Podman).                            | –                   |
| 2    | Introduction to Java (syntax, OOP, classes, methods, threads) and HTTP. | L2: Java introduction – build a simple HTTP server from scratch.            | –                   |
| 3    | Spring Boot & Databases.                                                | L3: Example of Spring Boot app connected to a database with ORM.            | –                   |
| 4    | Secret Management.                                                      | L4: Containerize Java app. Use a self-hosted secret manager.                | Spring integration. |
| 5    | Observability – the three pillars.                                      | L5: Spring observability (metrics/logs/traces).                             | –                   |
| 6    | Code Quality & Security.                                                | L6: Self-host SonarQube and Snyk.                                           | –                   |
| 7    | Service Integration. Contracts & Testing.                               | L7: Starter microservice in C++.                                            | –                   |
| 8    | Service Integration.                                                    | L8: Java ↔ C++ integration via HTTP, service discovery with Docker Compose. | –                   |
| 9    | CI/CD. Jenkins.                                                         | L9: Self-host Jenkins, configure GitHub integration.                        | –                   |
| 10   | GitHub Actions.                                                         | L10: Complete workflow with GitHub Actions.                                 | –                   |
| 11   | Cloud storage & file services.                                          | L11: Integrate LocalStack S3 for file attachments.                          | –                   |
| 12   | Infrastructure as Code (IaC).                                           | L12: Define resources with OpenTofu/Terraform.                              | –                   |
| 13   | Migration to real AWS.                                                  | L13: Deploy API + S3 to AWS.                                                | –                   |
| 14   | Recap & Project Presentations.                                          | Final demo & evaluation.                                                    | –                   |

---

## Laboratory Descriptions

### L1: Hello World with Containers

**Goal**: Get familiar with containers.
**Activities**: Install Docker/Podman, run a container.

```bash
# Pull and run Nginx
docker run -d -p 9000:80 nginx

# Test in browser:
# http://localhost:9000
```

**Homework**: None (intro lab).

---

### L2: Java & HTTP

**Goal**: Recap OOP and understand HTTP.
**Activities**: Build a simple HTTP server with threads.

```java
import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class SimpleHttpServer {
    public static void main(String[] args) throws Exception {
        ServerSocket server = new ServerSocket(8080);
        ExecutorService pool = Executors.newFixedThreadPool(4);
        System.out.println("Server running on http://localhost:8080");

        while (true) {
            Socket client = server.accept();
            pool.execute(() -> handle(client));
        }
    }

    static void handle(Socket client) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
             PrintWriter out = new PrintWriter(client.getOutputStream())) {
            
            String line = in.readLine(); // first line = request
            System.out.println("Request: " + line);

            out.println("HTTP/1.1 200 OK");
            out.println("Content-Type: text/plain");
            out.println();
            out.println("Hello from Java HTTP Server!");
            out.flush();
        } catch (IOException e) { e.printStackTrace(); }
    }
}
```

Now, make it REST compliant:

```java
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

public class Rest {
    private static volatile boolean running = true; // flag to control loop
    private static ServerSocket server;
    private static ExecutorService pool;

    public static void main(String[] args) throws IOException {
        int port = 8080;
        server = new ServerSocket(port);
        pool = Executors.newFixedThreadPool(4);
        log("Server running on http://localhost:" + port);

        // --- Graceful Shutdown Hook ---
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log("Shutdown requested. Stopping server...");
            running = false;
            try {
                server.close(); // stop accepting new clients
            } catch (IOException ignored) {}
            pool.shutdown(); // stop thread pool gracefully
            try {
                if (!pool.awaitTermination(3, TimeUnit.SECONDS)) {
                    log("Forcing shutdown...");
                    pool.shutdownNow();
                }
            } catch (InterruptedException e) {
                pool.shutdownNow();
            }
            log("Server stopped.");
        }));

        // --- Main Loop ---
        while (running) {
            try {
                Socket client = server.accept();
                pool.execute(() -> handle(client));
            } catch (SocketException e) {
                if (running) log("Socket exception: " + e.getMessage());
                // Break loop only if shutting down
                break;
            }
        }
    }

    static void handle(Socket client) {
        try (client;
             BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
             PrintWriter out = new PrintWriter(client.getOutputStream())) {

            // --- Parse the Request ---
            String requestLine = in.readLine();
            if (requestLine == null || requestLine.isEmpty()) return;

            String[] parts = requestLine.split(" ");
            if (parts.length < 3) {
                sendResponse(out, 400, "Bad Request", "Malformed HTTP request");
                log("400 Bad Request");
                return;
            }

            String method = parts[0];
            String path = parts[1];
            log("Incoming Request -> " + method + " " + path);

            // --- Basic Routing ---
            switch (method) {
                case "GET" -> handleGet(path, out);
                case "POST" -> handlePost(path, in, out);
                default -> {
                    sendResponse(out, 405, "Method Not Allowed", "Supported: GET, POST");
                    log("405 Method Not Allowed for " + method);
                }
            }

        } catch (IOException e) {
            log("Error: " + e.getMessage());
        }
    }

    static void handleGet(String path, PrintWriter out) {
        if ("/".equals(path)) {
            sendResponse(out, 200, "OK", "Welcome to the RESTful Java Server!");
        } else if ("/ping".equals(path)) {
            sendResponse(out, 200, "OK", "pong");
        } else {
            sendResponse(out, 404, "Not Found", "The resource " + path + " was not found.");
        }
    }

    static void handlePost(String path, BufferedReader in, PrintWriter out) throws IOException {
        // Read headers
        String line;
        int contentLength = 0;
        while (!(line = in.readLine()).isEmpty()) {
            if (line.startsWith("Content-Length:")) {
                contentLength = Integer.parseInt(line.split(":")[1].trim());
            }
        }

        char[] body = new char[contentLength];
        if (contentLength > 0) in.read(body);

        sendResponse(out, 200, "OK", "Received POST data: " + new String(body));
    }

    static void sendResponse(PrintWriter out, int statusCode, String statusText, String body) {
        out.println("HTTP/1.1 " + statusCode + " " + statusText);
        out.println("Content-Type: text/plain; charset=utf-8");
        out.println("Content-Length: " + body.length());
        out.println();
        out.print(body);
        out.flush();

        log("Response -> " + statusCode + " " + statusText);
    }

    static void log(String message) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        System.out.println("[" + timestamp + "] " + message);
    }
}
```

Run it, then open [http://localhost:8080](http://localhost:8080).
**Homework**: None.

---

### L3: Spring Boot & Databases

**Goal**: Connect a Spring Boot app to a DB.
**Activities**: CRUD with JPA/Hibernate.

Run Postgres 14 on Podman:
```bash
podman run -d \
  --name pg-local \
  -p 5432:5432 \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=javalab \
  postgres:14
```

Create project using [https://start.spring.io/](https://start.spring.io/)

```
src/main/resources/application.yaml

spring:
  profiles: local
  datasource:
    driver-class-name: org.postgresql.Driver
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:postgres}
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:javalab}
  jpa:
    hibernate:
      ddl-auto: update

src/main/resources/application-local.yaml
spring:
  jpa:
    show-sql: true
    properties:
      hibernate:
        format_sql: true
```

```java
// Example: simple "Note" entity
@Entity
public class Note {
    @Id @GeneratedValue
    private Long id;
    private String text;
    // getters/setters
}

@Repository
interface NoteRepo extends JpaRepository<Note, Long> {}

@RestController
class NoteController {
    @Autowired NoteRepo repo;

    @PostMapping("/notes")
    public Note add(@RequestBody Note n) { return repo.save(n); }

    @GetMapping("/notes")
    public List<Note> list() { return repo.findAll(); }
}
```

**Homework 1 (10%)**:

* Create a Spring Boot app with the following endpoints:
  - POST /notes
  - GET /notes
  - GET /notes/:id
  - PATCH /notes/:id
  - DELETE /notes/:id
* Data must persist between runs.
* Dokerize app
* Document the API -> OpenAPI Swagger.
* Add docker-compose.yaml for both DB and SpringBoot app to run at the same time.

---

# Lab 4 — Vault (self-hosted)

**Goal:** run a local Vault server, store the DB password in Vault, and make the Spring Boot app read it at startup.

### 1) Run Vault (dev mode) with Docker (quick lab-friendly setup)

> **Dev note:** `dev` mode is *not* secure for production, but perfect for labs.

```hcl

# config/vault.sh

disable_mlock = true

storage "file" {
  path = "/vault/file"
}

listener "tcp" {
  address     = "0.0.0.0:8200"
  tls_disable = 1
}

ui = true
default_lease_ttl = "168h"
max_lease_ttl     = "720h"

```

```bash
#!/bin/bash

# run_vault.sh

podman run -d --cap-add=IPC_LOCK \
  -v "$(pwd)/config:/vault/config.d" \
  -v vault-data:/vault/file \
  -p 8200:8200 \
  hashicorp/vault:1.20.0 \
  server -config=/vault/config.d/vault.hcl
```

Run commands step by step:
- `podman ps` -> get the CONTAINER_ID for the vault service
- `podman exec -it <CONTAINER_ID> sh` -> enter the container CLI to get access to `vault` commands
- `export VAULT_ADDR='http://127.0.0.1:8200'` -> set vault addr to http (tls is disabled)
- `vault operator init -key-shares=5 -key-threshold=3 -format=json > vault-init.json` -> generate vault credentials (unseal keys and root tooken)
- `cat vault-init.json` -> read the values generated and save them in a "secure" way locally
- `vault operator unseal <unseal_keys_b64[1]>`
- `vault operator unseal <unseal_keys_b64[2]>`
- `vault operator unseal <unseal_keys_b64[3]>`
- `vault login <root_token>`
- `export VAULT_TOKEN='<root_token>'`
- `vault secrets enable -path=secret -version=2 kv` -> enable key-value pairs
- `vault kv put secret/myapp username=alice password='S3cr3t'` - test save secret
- `vault kv list secret/` -> list apps in secret/
- `vault kv get secret/myapp` -> get the secrets of myapp

Now, check:
* Vault UI: [http://localhost:8200](http://localhost:8200)

### 2) Outside of container CLI

##### 1) Write a secret into Vault

```bash
# set a simple secret at path secret/data/myapp/db
export VAULT_ADDR=http://127.0.0.1:8200
export VAULT_TOKEN='<root_token>'

# put the DB password
curl --header "X-Vault-Token: ${VAULT_TOKEN}" \
     --request POST \
     --data '{"data": {"username":"postgres","password":"postgres"}}' \
     ${VAULT_ADDR}/v1/secret/data/java-lab/db
```

##### 2) Fetch secret manually (curl) to see it

```bash
curl --header "X-Vault-Token: ${VAULT_TOKEN}" ${VAULT_ADDR}/v1/secret/data/java-lab/db
# Inspect JSON -> data.data.password
```

### 4) Integrate with Spring Boot (simple, no extra libraries)

__Recommended__: Spring Vault / Spring Cloud Vault

- enter container CLI as the above
- `vault auth enable approle` -> enable approle
- Add `app-policy.hcl` in config/ and restart container `podman restart <CONTAINER_ID>`

```hcl
# config/app-policy.hcl

path "secret/data/java-lab/*" {
  capabilities = ["read"]
}
```

- Unseal vault
- `vault policy write app-policy app-policy.hcl`
- Create an approle for the SpringBoot app
```bash
vault write auth/approle/role/java-lab \
  secret_id_ttl=60m \
  token_ttl=30m \
  token_max_ttl=120m \
  policies="app-policy"
```

* `vault read auth/approle/role/java-lab/role-id` → not secret, can be used and displayed anywhere.
* `vault write -f auth/approle/role/java-lab/secret-id` → secret, should **not** be added to any file or log.
* Recommended: `vault write -wrap-ttl=60s -f auth/approle/role/java-lab/secret-id` → the result is stored as a secret. At startup, if it is still valid, the application requests **unwrap** and receives the real SecretID. This avoids issues if it appears in logs, etc.

```yml

# application.yml

spring:
  cloud:
    vault:
      uri: http://127.0.0.1:8200
      authentication: APPROLE
      app-role:
        role-id: <ROLE_ID>
        secret-id: <SECRET_ID>
      kv:
        enabled: true
        backend: secret
        application-name: java-lab

...

  datasource:
    username: ${db.username}
    password: ${db.password}
```

---

# Lab 5 — Observability (metrics, logs, traces) — **detailed step-by-step**

**Goal:** instrument your Spring Boot app, collect metrics with Prometheus, view dashboards in Grafana, and see traces in Jaeger.

We’ll do: Spring Boot → Prometheus (metrics) → Grafana (dashboards) and Spring Boot → OpenTelemetry Java Agent → Jaeger (traces). We use Docker Compose for Prometheus/Grafana/Jaeger.

---

## A. Start monitoring stack (docker-compose)

Create `monitoring-docker-compose.yml`:

```yaml
version: "3.8"
services:
  prometheus:
    image: prom/prometheus:latest
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml:ro
    ports: ["9090:9090"]

  grafana:
    image: grafana/grafana:latest
    ports: ["3000:3000"]
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin

  jaeger:
    image: jaegertracing/all-in-one:latest
    ports: ["16686:16686","4317:4317"]  # 16686=UI, 4317=OTLP receiver (used by OpenTelemetry)
```

Create a minimal `prometheus.yml` (scrape Spring Boot `/actuator/prometheus`):

```yaml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'spring-actuator'
    static_configs:
      - targets: ['host.docker.internal:8080']   # Windows/Mac: host.docker.internal
      # On Linux, if your app runs on host use: 'localhost:8080'
```

> **Note:** `host.docker.internal` works in Docker Desktop on Mac/Windows. On Linux you may set `targets: ['172.17.0.1:8080']` or run the app in a container within the same compose network.

Run the stack:

```bash
docker-compose -f monitoring-docker-compose.yml up -d
```

* Prometheus UI: [http://localhost:9090](http://localhost:9090)
* Grafana UI: [http://localhost:3000](http://localhost:3000) (admin/admin)
* Jaeger UI: [http://localhost:16686](http://localhost:16686)

---

## B. Instrument the Spring Boot app for metrics

**1) Add Gradle dependencies (build.gradle)**

```groovy
dependencies {
    ...
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'io.micrometer:micrometer-registry-prometheus'
}
```

**2) application.properties**

```properties
management.endpoints.web.exposure.include=health,info,prometheus
management.endpoint.prometheus.enabled=true
management.server.port=8080
```

**3) Add a custom metric in a controller (example)**

```java
@RestController
public class NoteController {
    private final Counter createCounter;
    private final Timer requestTimer;

    public NoteController(MeterRegistry registry) {
        this.createCounter = registry.counter("notes.created.count");
        this.requestTimer = registry.timer("notes.request.timer");
    }

    @PostMapping("/notes")
    public Note add(@RequestBody Note note) {
        createCounter.increment();
        return requestTimer.record(() -> repo.save(note));
    }
}
```

* Now `/actuator/prometheus` will expose metrics including `notes_created_count` and the standard `http_server_requests_seconds` (Micrometer).

---

## C. Scrape metrics with Prometheus

* Confirm Prometheus scrapes the endpoint: Go to Prometheus → `Status > Targets`.
* If target is down, adjust `prometheus.yml` target. For simplicity, run the Spring Boot app on host at port `8080` (so Prometheus in Docker can reach it with `host.docker.internal:8080`).

---

## D. Create Grafana dashboard (basic)

1. Log in Grafana ([http://localhost:3000](http://localhost:3000), admin/admin), go to **Configuration > Data Sources > Add > Prometheus**, URL: `http://prometheus:9090` **(when Grafana runs in Docker, service name `prometheus` resolves inside the same compose network)** — if Grafana is on host, use `http://localhost:9090`.
2. Create a new dashboard:

   * Panel 1 (Requests per second): PromQL:

     ```
     sum(rate(http_server_requests_seconds_count[1m])) by (uri)
     ```
   * Panel 2 (Latency - p95):

     ```
     histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[5m])) by (le, uri))
     ```
3. Add other panels: error rate:

   ```
   sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) by (uri)
   ```

---

## E. Traces with OpenTelemetry -> Jaeger

1) Add to `gradle.properties`:
```
otel.enabled=true
otel.agent.jar=opentelemetry-javaagent.jar
otel.config.file=otel-config.yaml
otel.service.name=notes-service
otel.exporter.otlp.endpoint=http://localhost:4317
```

2) Add to `build.gradle`:
```groovy
bootRun {
    // Read OTEL enabled flag from project property
    def otelEnabled = project.hasProperty('otel.enabled') ? project.property('otel.enabled').toBoolean() : false

    if (otelEnabled) {
        println "✅ OpenTelemetry enabled - attaching agent"
        def otelAgentJar = project.findProperty('otel.agent.jar')
        def otelConfigFile = project.findProperty('otel.config.file')
        def otelServiceName = System.getenv('OTEL_SERVICE_NAME') ?: project.findProperty('otel.service.name')
        def otelOtlpEndpoint = System.getenv('OTEL_EXPORTER_OTLP_ENDPOINT') ?: project.findProperty('otel.exporter.otlp.endpoint')

        jvmArgs = [
            "-javaagent:${otelAgentJar}",
            "-Dotel.config.file=${otelConfigFile}",
            "-Dotel.service.name=${otelServiceName}",
            "-Dotel.exporter.otlp.endpoint=${otelOtlpEndpoint}"
        ]
    } else {
        println "⚪ OpenTelemetry disabled - running normally"
    }
}
```

3) Add `otel-config.yaml`:
```yaml
otel:
  exporter:
    otlp:
      endpoint: ${OTEL_EXPORTER_OTLP_ENDPOINT:http://localhost:4317}
  resource:
    attributes:
      service.name: ${OTEL_SERVICE_NAME:notes-service}
  instrumentation:
    jdbc: true
    spring-webmvc: true
    http-client: true
```

Explanation:

- When otel.enabled=true → the Java agent is attached automatically.
- Uses gradle.properties as defaults.
- Environment variables override Gradle properties.
- When otel.enabled=false → runs normally.

* The agent automatically instruments Spring MVC, http clients, jdbc etc.
* Open Jaeger UI ([http://localhost:16686](http://localhost:16686)) and search for `notes-service` traces.

---

## F. Generate load to see metrics/traces

Use a simple load generator (hey, ab, or a looped `curl`):

```bash
# simple loop to generate traffic
for i in {1..200}; do curl -s -o /dev/null http://localhost:8080/notes; done
```

Then in Grafana you should start seeing metrics; in Jaeger you should see traces.

---

## G. Homework (10%)

* Add at least **two graphs** in Grafana: traffic (RPS) and latency (p95).
* Create a short doc: how to reproduce the trace for one request and where to find it in Jaeger.

---

# Lab 6 — SonarQube & Snyk (self-hosted) — step-by-step

### A. SonarQube (local with PostgreSQL)

`docker-compose-sonar.yml`:

```yaml
version: '3.8'
services:
  db:
    image: postgres:13
    environment:
      - POSTGRES_USER=sonar
      - POSTGRES_PASSWORD=sonar
      - POSTGRES_DB=sonar
    volumes:
      - sonar-db:/var/lib/postgresql/data

  sonarqube:
    image: sonarqube:community
    depends_on: [db]
    ports:
      - "9000:9000"
    environment:
      - SONAR_JDBC_URL=jdbc:postgresql://db:5432/sonar
      - SONAR_JDBC_USERNAME=sonar
      - SONAR_JDBC_PASSWORD=sonar
    ulimits:
      nofile:
        soft: 65536
        hard: 65536

volumes:
  sonar-db:
```

Run:

```bash
docker-compose -f docker-compose-sonar.yml up -d
```

* Sonar UI: [http://localhost:9000](http://localhost:9000) (default admin/admin — change it)
* Create a new project token in Sonar UI to be used by scanner.

### B. Analyze a Maven project with Sonar

In your `pom.xml`, add the Sonar Maven plugin or run the scanner directly:

```bash
# from project root
mvn -DskipTests clean package sonar:sonar \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.login=<SONAR_TOKEN>
```

* Read the Sonar UI for issues, code smells, vulnerabilities, and fix them iteratively.

### C. Snyk (local scanning)

Snyk requires creating a (free) account to get an auth token. Steps:

1. Sign up at snyk.io (free for students).
2. Install Snyk CLI:

   * Node/npm method:

     ```bash
     npm install -g snyk
     snyk auth   # follow link in terminal to authenticate
     ```
3. Run a local test:

   ```bash
   # test dependencies of a Java project (Maven)
   snyk test --org=<your-org-if-needed>
   ```
4. Optionally run `snyk monitor` to send results to Snyk web UI.

**Alternative (no sign-up)**: use OWASP Dependency-Check or `mvn dependency:analyze` for basic local insights.

---

# Lab 7 — C++ microservice (CMake) + integration with Java (HTTP)

**Goal:** create a C++ HTTP microservice with CMake, containerize it, and call it from Java.

### 1) Minimal C++ microservice using `cpp-httplib`

`cpp-httplib` is header-only and easy to use.

`cpp-service/main.cpp`:

```cpp
#include "httplib.h"
using namespace httplib;

int main() {
    Server svr;
    svr.Get("/hello", [](const Request&, Response& res) {
        res.set_content("Hello from C++ service!", "text/plain");
    });
    svr.listen("0.0.0.0", 8081);
}
```

### 2) CMakeLists.txt

`cpp-service/CMakeLists.txt`:

```cmake
cmake_minimum_required(VERSION 3.10)
project(cpp_service)

set(CMAKE_CXX_STANDARD 17)

add_executable(cpp_service main.cpp)

# If using additional libs, link here.
```

### 3) Dockerfile for C++ service

`cpp-service/Dockerfile`:

```dockerfile
FROM ubuntu:22.04
RUN apt-get update && apt-get install -y build-essential cmake git
WORKDIR /app
COPY . /app
RUN cmake -S . -B build && cmake --build build --config Release
EXPOSE 8081
CMD ["./build/cpp_service"]
```

### 4) Build & run (locally)

```bash
cd cpp-service
docker build -t cpp-service:lab1 .
docker run -p 8081:8081 cpp-service:lab1
# test
curl http://localhost:8081/hello
```

### 5) Java code to call C++ service

Use Spring Boot `RestTemplate` or `HttpClient`:

```java
RestTemplate rt = new RestTemplate();
String resp = rt.getForObject("http://cpp-service:8081/hello", String.class);
System.out.println("from cpp: " + resp);
```

* If running both in Docker Compose, use service name `cpp-service` from Java service.

---

# Lab 8 — Docker Compose integration (step-by-step)

**Goal:** bring Java, C++ service, DB, Vault and monitoring together via Docker Compose.

### 1) Minimal `docker-compose.yml`

```yaml
version: '3.8'
services:
  postgres:
    image: postgres:13
    environment:
      POSTGRES_USER: lab
      POSTGRES_PASSWORD: lab
      POSTGRES_DB: labdb
    ports: ["5432:5432"]

  vault:
    image: vault:latest
    environment:
      VAULT_DEV_ROOT_TOKEN_ID: devroot
    ports: ["8200:8200"]
    command: "server -dev -dev-listen-address=0.0.0.0:8200"

  cpp-service:
    build: ./cpp-service
    ports: ["8081:8081"]

  java-api:
    build: ./java-api
    ports: ["8080:8080"]
    depends_on:
      - postgres
      - cpp-service
      - vault
    environment:
      VAULT_ADDR: http://vault:8200
      VAULT_TOKEN: devroot
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/labdb
```

### 2) Bring it up

```bash
docker-compose up --build
```

### 3) Verify

* `curl http://localhost:8080/notes` (Java API) — Java will call `http://cpp-service:8081/hello` internally.
* `docker-compose logs -f java-api` to follow logs.

### 4) Service discovery & DNS

* In Compose, service names act as DNS names. From `java-api` container you can reach `cpp-service:8081`.

---

# Labs 9 & 10 — Jenkins & GitHub Actions (self-hosted runner) — step-by-step with local service access

You want CI workflows that *actually talk to your local self-hosted services* — two reliable options:

* **A)** Run a GitHub Actions self-hosted runner on the same machine (recommended). Workflows run locally and can access `localhost` and local Docker Compose services directly.
* **B)** Use cloud runners + an exposed tunnel (ngrok) to forward a local service to a public URL (less secure; handy to demo).

---

## Option A — GitHub Actions self-hosted runner

1. On your machine, create folder and download the runner from GitHub (Settings → Actions → Runners → New self-hosted runner). GitHub provides the download link and a registration token.

2. Unpack and configure (example Linux commands — replace token/URL with values from GitHub):

```bash
mkdir actions-runner && cd actions-runner
# download the runner tarball (from GitHub page) then extract...
tar xzf actions-runner-linux-x64-*.tar.gz
./config.sh --url https://github.com/your-org/your-repo --token YOUR_TOKEN
./run.sh
```

3. Create a workflow that uses the self-hosted runner:

`.github/workflows/ci.yml`

```yaml
name: CI local
on: [push]
jobs:
  build:
    runs-on: self-hosted   # this will run on the machine where you started the runner
    steps:
      - uses: actions/checkout@v3
      - name: Build with Maven
        run: ./mvnw -B package
      - name: Run integration tests
        run: ./mvnw -DskipUnitTests=false verify
      - name: Call local service
        run: curl --fail http://localhost:8080/health
```

* Because the runner is on the same host, `localhost:8080` refers to local services (or `host.docker.internal` if you run inside containers). This lets workflows interact with your local DB, Vault, MinIO, etc.

---

## Option B — Use ngrok to expose a local service

1. Install ngrok, run:

```bash
ngrok http 8080
```

2. ngrok gives a public URL (e.g. `https://abc123.ngrok.io`). In GitHub Actions (cloud runner) you can then `curl https://abc123.ngrok.io/health`.
   **Security caution:** ngrok exposes your local service publicly — don’t use with confidential data.

---

## Jenkins (for Lab 9) — quick steps

1. Run Jenkins via Docker:

```bash
docker run --name jenkins -d -p 8085:8080 -p 50000:50000 \
  -v jenkins_home:/var/jenkins_home jenkins/jenkins:lts
```

2. Open Jenkins [http://localhost:8085](http://localhost:8085), follow setup, install recommended plugins (Git, Pipeline).
3. Create a pipeline job with `Jenkinsfile` in repository:

`Jenkinsfile`

```groovy
pipeline {
  agent any
  stages {
    stage('Checkout') { steps { checkout scm } }
    stage('Build') { steps { sh './mvnw -DskipTests clean package' } }
    stage('Integration') { steps { sh 'curl -f http://host.docker.internal:8080/health' } }
  }
}
```

* Jenkins running locally can reach local services (host.docker.internal or direct container network depending on setup).

---

# Lab 11 — MinIO (self-hosted) step-by-step

**Goal:** run MinIO, create a bucket, and integrate Spring Boot using AWS SDK.

### 1) Start MinIO (Docker Compose)

`docker-compose-minio.yml`:

```yaml
version: '3.8'
services:
  minio:
    image: minio/minio:latest
    environment:
      MINIO_ROOT_USER: minioadmin
      MINIO_ROOT_PASSWORD: minioadmin
    command: server /data --console-address ":9001"
    ports:
      - "9000:9000"  # S3 API
      - "9001:9001"  # web console
    volumes:
      - minio-data:/data

volumes:
  minio-data:
```

Run:

```bash
docker-compose -f docker-compose-minio.yml up -d
```

* MinIO Console: [http://localhost:9001](http://localhost:9001) (user: `minioadmin` / `minioadmin`)

### 2) Create a bucket with `mc` (minio client) or via console

* Install `mc` or use console to create bucket `attachments`.

`mc` example:

```bash
mc alias set local http://localhost:9000 minioadmin minioadmin
mc mb local/attachments
```

### 3) Configure Spring Boot to use MinIO (AWS S3 compatible)

Use AWS SDK v2 (or Spring Cloud AWS). Example code (AWS v2):

```java
S3Client s3 = S3Client.builder()
    .endpointOverride(URI.create("http://localhost:9000"))
    .credentialsProvider(StaticCredentialsProvider.create(
        AwsBasicCredentials.create("minioadmin","minioadmin")))
    .region(Region.of("us-east-1"))
    .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
    .build();

PutObjectRequest putReq = PutObjectRequest.builder()
    .bucket("attachments").key("file.txt").build();

s3.putObject(putReq, RequestBody.fromString("hello minio"));
```

* `pathStyleAccessEnabled(true)` is important for MinIO.

### 4) Test via curl/postman

Use the Java API to upload, then `mc` or MinIO console to verify the object exists.

---

# Lab 12 — Infrastructure as Code (EC2 + DB) — Terraform (OpenTofu compatible)

**Goal:** use Terraform/OpenTofu to create an EC2 instance (where the app will run) and a managed PostgreSQL database (RDS) in AWS.

> **IMPORTANT:** Running these resources may incur costs. Use AWS Free Tier where possible. Destroy resources after the lab (`terraform destroy`).

### 1) Basic Terraform structure

`main.tf` (skeleton, replace placeholders and set variables):

```hcl
provider "aws" {
  region = var.aws_region
}

data "aws_ami" "amazon_linux" {
  most_recent = true
  owners      = ["amazon"]
  filter {
    name   = "name"
    values = ["amzn2-ami-hvm-*-x86_64-gp2"]
  }
}

resource "aws_security_group" "web_sg" {
  name = "lab-web-sg"
  ingress {
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = [var.my_ip_cidr]
  }
  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }
  egress { from_port = 0; to_port = 0; protocol = "-1"; cidr_blocks = ["0.0.0.0/0"] }
}

resource "aws_instance" "web" {
  ami           = data.aws_ami.amazon_linux.id
  instance_type = var.instance_type
  security_groups = [aws_security_group.web_sg.name]
  user_data = <<-EOF
              #!/bin/bash
              # Install docker and run your docker-compose
              yum update -y
              amazon-linux-extras install docker -y
              systemctl enable --now docker
              curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
              chmod +x /usr/local/bin/docker-compose
              # assume we have a repo with docker-compose.yml:
              # git clone <repo> /app
              # cd /app && docker-compose up -d
              EOF
  tags = { Name = "lab-web-instance" }
}

resource "aws_db_instance" "pg" {
  allocated_storage    = 20
  engine               = "postgres"
  engine_version       = "13.7"
  instance_class       = "db.t3.micro"
  name                 = var.db_name
  username             = var.db_user
  password             = var.db_password
  skip_final_snapshot  = true
  vpc_security_group_ids = [aws_security_group.db_sg.id]
}
```

`variables.tf` (example):

```hcl
variable "aws_region" { default = "us-east-1" }
variable "my_ip_cidr" { default = "YOUR_IP/32" }  # restrict SSH to your IP
variable "instance_type" { default = "t3.micro" }
variable "db_name" { default = "labdb" }
variable "db_user" { default = "labuser" }
variable "db_password" { default = "ChangeMe123!" }
```

### 2) Steps

1. `terraform init`
2. `terraform plan -out plan.tf`
3. `terraform apply plan.tf` (or `terraform apply`)

### 3) After create

* Note the EC2 public IP (output it in Terraform `outputs.tf`).
* SSH into the EC2: `ssh -i yourkey.pem ec2-user@<public-ip>`
* On the EC2, clone your repo and run Docker Compose (user_data can automate it).

### 4) Security & clean-up

* Use a security group that restricts SSH to your IP only.
* Destroy resources after the lab: `terraform destroy`.

---

# Lab 13 — Deploying API + S3 to AWS (step-by-step)

**Goal:** use resources provisioned in Lab 12 to deploy the app reachable from internet and use S3.

### 1) Create an S3 bucket (Terraform snippet)

```hcl
resource "aws_s3_bucket" "app_bucket" {
  bucket = "lab-app-bucket-${random_id.suffix.hex}"
  acl    = "private"
}
```

Add `random_id` to generate unique name:

```hcl
resource "random_id" "suffix" {
  byte_length = 4
}
```

### 2) Give EC2 an IAM role (recommended) to access S3

Create IAM role and instance profile, attach policy for S3 access. Then attach instance profile to `aws_instance.web` so your app can use instance profile credentials (no long-lived keys).

(Example in Terraform is a few resources: `aws_iam_role`, `aws_iam_policy`, `aws_iam_role_policy_attachment`, `aws_iam_instance_profile`.)

### 3) Build and push Docker image (optional) — or run docker-compose on EC2

* If you use ECR: create an ECR repo, `docker login --username AWS --password-stdin ...`, `docker push`.
* Simpler for labs: on EC2 `git clone` your app and run `docker-compose up -d` (user data does this).

### 4) Configure Spring Boot to use S3

* If you used IAM role, the AWS SDK on EC2 will pick up instance role automatically.
* If not, set `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY` **as environment variables** (not in Git) — better: store in Vault.

Example application.properties for AWS S3 (if using SDK default credentials nothing required):

```
app.s3.bucket=lab-app-bucket-xyz
```

Java code sample (AWS SDK v2) — same as MinIO example but without `endpointOverride`.

### 5) Security groups

Allow inbound port 80 (or 8080) in EC2 security group. Use a small Nginx reverse proxy container if you need to serve via port 80.

### 6) Test from internet

* Visit `http://<ec2-public-ip>:80/` (or mapped port) to access API.
* Upload a file to S3 via the app; confirm object appears in S3 console.

### 7) Document and destroy

Document steps (what Terraform created, where to find logs, how to connect). Destroy resources when done.

---

# Final notes, tips & safety reminders

* **Costs:** AWS RDS + EC2 may cost money. Use the free tier where possible and always `terraform destroy` or delete resources after labs.
* **Local vs cloud:** For grading, you can use LocalStack / MinIO / Docker to avoid cloud costs; L13 demonstrates the real cloud step.
* **Credentials:** Never hardcode secrets. Vault + instance profiles are safer. For labs, dev tokens are fine but emphasize cleanup.
* **Troubleshooting:** If Docker Compose sees ports in use, stop the service using that port. Use `docker-compose logs -f` to read logs.
* **Order to teach:** Vault (L4) → Observability (L5) → Sonar/Snyk (L6) → C++ microservice (L7) → Integration (L8) → CI/CD (L9/10) → Storage (L11) → IaC (L12) → AWS deploy (L13).

---

If you want, I can:

* produce the **complete `docker-compose.yml`** that includes app, C++ service, Vault, Postgres, Prometheus, Grafana, Jaeger and MinIO (one big compose) — handy for a single `up -d` demo; **or**
* generate the **complete Terraform files** (main.tf, variables.tf, outputs.tf) with all resources wired (I’ll put placeholders for region/keys and include the IAM role for S3).

Which of those two would you like me to prepare now?
