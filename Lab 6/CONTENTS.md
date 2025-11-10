
# 🧪 Laboratory 6 — Microservices and Secret Management with HashiCorp Vault

  

## 🎯 Objective

In this laboratory, you will extend the Authentication API from the previous labs into a microservice-oriented architecture where secrets (such as database credentials) are securely managed using HashiCorp Vault. By the end of this lab, you will understand microservices, the importance of secret management, and how to integrate Vault with a Spring Boot application to dynamically manage database credentials.

  

## 0) Introduction: Microservices and Secret Management

  

### 🧩 What Are Microservices?

Microservices are small, independent services that communicate with each other through APIs. Each service is responsible for a specific functionality and can be developed, deployed, and scaled independently.

  

**Key characteristics:**

- Independently deployable and scalable.

- Loosely coupled and communicate via REST or messaging.

- Can use different programming languages and databases.

- Easier maintenance and fault isolation.

  

**Example:**

An e-commerce system might include:

-  `auth-service`: handles authentication and registration.

-  `product-service`: manages product information.

-  `order-service`: processes customer orders.

  

Each of these services can be deployed separately and communicate via well-defined APIs.

  

### 🔐 What Is Secret Management?

Secret management is the practice of securely storing and controlling access to sensitive data such as passwords, tokens, and certificates. Hardcoding credentials in `application.yml` or `.env` files is dangerous and leads to potential security breaches.

  

**Secret management systems** like HashiCorp Vault provide:

- Secure, centralized storage for secrets.

- Fine-grained access control via policies.

- Automatic secret rotation and expiration.

- Audit logging of access to secrets.

  

Vault can also generate dynamic secrets, meaning credentials are created on demand and automatically expire after a defined period.

  

## 1) Why Use HashiCorp Vault

Vault serves as a secure gateway between applications and sensitive data. It ensures that applications never store passwords directly but retrieve them securely when needed.

  

In this lab, Vault will:

1. Store and issue credentials for PostgreSQL.

2. Authenticate the Spring Boot app using AppRole.

3. Generate short-lived database credentials.

4. Automatically revoke credentials after expiration (default: 1 hour).

  

Vault goes through three phases before it becomes operational:

-  **Initialization**: Generates encryption keys and a root token.

-  **Unseal**: Unlocks Vault using the unseal keys.

-  **Ready**: Starts serving requests and managing secrets.

  

Once initialized, Vault can enable authentication methods (e.g., AppRole), define access policies, and activate secret engines such as `database/`.

  

## 2) Lab Overview

This lab contains three main containers managed through Docker Compose:

  

| Service | Role | Description |
|----------|------|-------------|
| **PostgreSQL** | Database | Stores users for the Authentication API. Vault connects to it to generate temporary credentials. |
| **Vault** | Secret manager | Manages and issues secrets dynamically. |
| **Spring Boot App** | Application | Retrieves credentials securely from Vault and connects to the database. |

  

Folder structure:

```

Lab 6/

├─ infrastructure/
│ ├─ docker-compose.yml
│ └─ vault/
│ ├─ Dockerfile.custom-vault
│ ├─ config/vault.hcl
│ ├─ custom-entrypoint.sh
│ └─ scripts/
│ ├─ init.sh
│ └─ setup.sh
└─ java-lab/
├─ Dockerfile
├─ scripts/start.sh
└─ src/main/...

```

  

## 3) Step 1 — Prepare the Environment

Ensure Docker and Docker Compose are installed. Clone the repository:

```

git clone https://github.com/Curs-DevOps/Full-Labs.git

cd "Full-Labs/Lab 6/infrastructure"

```

  

## 4) Step 2 — Custom Vault Image

Instead of using the default Vault image, this lab builds a custom image defined in `Lab 6/infrastructure/vault/Dockerfile.custom-vault`. It automates the full Vault setup process:

- Builds from the official `vault:latest` image.

- Loads configuration from `config/vault.hcl`.

- Uses `custom-entrypoint.sh` to:

1. Start Vault in the background.

2. Run `scripts/init.sh` to initialize and unseal Vault.

3. Run `scripts/setup.sh` to enable AppRole authentication, create database roles, and configure the database secrets engine.

- Saves generated credentials (role ID, secret ID) into the shared volume `vault-creds/`.

  

## 5) Step 3 — Docker Compose Setup

The file `Lab 6/infrastructure/docker-compose.yml` defines the three containers:

```

version: "3.8"

services:

postgres:

image: postgres:16

container_name: lab6-postgres

environment:

POSTGRES_DB: authdb

POSTGRES_USER: authuser

POSTGRES_PASSWORD: authpass

ports:

- "5432:5432"

volumes:

- pgdata:/var/lib/postgresql/data

  

vault:

build:

context: ./vault

dockerfile: Dockerfile.custom-vault

container_name: lab6-vault

ports:

- "8200:8200"

volumes:

- vault-creds:/vault-creds

cap_add:

- IPC_LOCK

  

java-lab:

build:

context: ../java-lab

dockerfile: Dockerfile

container_name: lab6-java

depends_on:

- vault

- postgres

environment:

SPRING_CLOUD_VAULT_URI: http://vault:8200

SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/authdb

volumes:

- vault-creds:/vault-creds:ro

ports:

- "8080:8080"

  

volumes:

pgdata:

vault-creds:

```

  

## 6) Step 4 — Running the Setup

Start all containers:

```

docker compose up --build

```

Expected output includes:

```

Vault initialized and unsealed

Policy and AppRole configured

Database secrets engine enabled

Credentials saved to /vault-creds

```

Check the volume to confirm the files `role-id` and `secret-id` exist in `vault-creds/`. They will be used by the Java app to authenticate with Vault.

  

Access Vault’s web UI at http://localhost:8200.

  

## 7) Step 5 — Spring Boot Application

The application in `Lab 6/java-lab` extends the authentication API and integrates Vault for credential retrieval.

  

**Highlights:**

- No hardcoded credentials.

- Uses Spring Cloud Vault with AppRole authentication.

- Reads Vault configuration via environment variables (`SPRING_CLOUD_VAULT_URI`, `VAULT_ROLE_ID`, `VAULT_SECRET_ID`).

- Receives temporary PostgreSQL credentials from Vault.

  

Dockerfile for the app uses a multi-stage build:

```

FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY . .

RUN mvn clean package -DskipTests

  

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]

```

  

## 8) Step 6 — Testing

Once containers are running:

1. Check Java app logs:

```

docker logs -f lab6-java

```

Expected message:

```

Started AuthenticationApiApplication

```

2. Verify health endpoint:

```

curl http://localhost:8080/actuator/health

```

3. Test user registration and login endpoints.

4. Inspect PostgreSQL users:

```

docker exec -it lab6-postgres psql -U authuser -d authdb

\du

```

Vault should have generated temporary DB users.

  

## 9) Step 7 — Dynamic Credentials

Vault’s database secrets engine generates credentials dynamically based on policies. When the Spring Boot app authenticates, Vault creates temporary PostgreSQL credentials valid for one hour. After expiration, Vault automatically revokes them.

  

Currently, the application does not refresh credentials automatically. Implementing rotation requires additional configuration in Spring Cloud Vault.

  

## 10) Step 8 — Troubleshooting

| Problem | Cause | Solution |
|----------|--------|-----------|
| Vault connection refused | Vault not ready | Restart the Java container after Vault starts |
| Database access denied | Expired credentials | Restart containers or extend TTL in setup.sh |
| Vault UI shows no data | Invalid token | Use the token from `/vault-creds` |
| Spring errors on startup | Missing environment variables | Verify docker-compose configuration |

  

## 11) Homework and Optional Tasks

**Required Deliverables:**

1. Screenshot showing all containers running (postgres, vault, java-lab).

2. Screenshot of Vault’s Database Secrets Engine UI.

3. Short text file explaining: “Why is Vault safer for managing credentials than hardcoding them?”

  

**Optional Tasks:**

- Enable TLS for Vault using `vault/config/tls/`.

- Add a static secret to Vault and retrieve it from the application.

- Restart only the Java container to show that it retrieves new credentials dynamically.

  

## 12) Summary

In this lab, you learned how to integrate HashiCorp Vault with a Spring Boot microservice for secure secret management. You deployed a multi-container system where Vault automatically generates dynamic PostgreSQL credentials, demonstrating secure practices for cloud-based microservice architectures.

  

## 13) Resources

- [HashiCorp Vault Documentation](https://developer.hashicorp.com/vault)

- [Spring Cloud Vault Reference](https://docs.spring.io/spring-cloud-vault/docs/current/reference/html/)

- [PostgreSQL Docker Image](https://hub.docker.com/_/postgres)
