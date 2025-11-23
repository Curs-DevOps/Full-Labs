# Homework — Microservices and Secret Management with HashiCorp Vault

You will extend your **Lab 6** project to retrieve secrets from HashiCorp Vault and expose them through a secure endpoint.

---

## Requirements

### 1. Vault Policy for KV Secrets (3pts)

*   Update the Vault setup (`infrastructure/vault/scripts/setup.sh` or a similar script) to:
    *   Enable the Key-Value (KV) secrets engine at a specific path (e.g., `secret/`).
    *   Create a new policy that grants `read` access to the KV secrets engine.
    *   Attach this policy to the `auth-app` role created for the Spring Boot application.
*   Manually (or via script) add a sample secret (e.g., `secret/data/apikey`) in Vault with a key-value pair (e.g., `key=supersecretapikey123`).

---

### 2. Secure Endpoint for Secret Retrieval (5pts)

*   Create a new endpoint `/api/secrets/apikey` in your Spring Boot application.
*   This endpoint should securely retrieve the secret value from the Vault KV store.
*   Follow a layered architecture for this implementation:
    *   **Controller (`SecretController`)**: Handles the incoming HTTP request and calls the service layer.
    *   **Service (`SecretService`)**: Contains the business logic for retrieving the secret. It should depend on a repository interface.
    *   **Repository (`SecretRepository` interface)**: Defines the contract for secret retrieval.
    *   **Implementation (`VaultSecretRepository`)**: Implements the `SecretRepository` interface and interacts with Vault to read the secret. Use the `VaultTemplate` provided by Spring Cloud Vault.

---

### 3. Docker & Environment (2pts)

*   Ensure your `docker-compose.yml` setup remains functional and the Java application can communicate with both Vault and PostgreSQL.
*   The application should start without errors, successfully authenticate with Vault using AppRole, and be ready to serve requests.

---

## Guidelines

*   Start from your **Lab 6 project**.
*   Focus on understanding how Spring Cloud Vault abstracts the interaction with Vault's API.
*   Use the existing `VaultTemplate` bean for interacting with Vault. You might need to inject it into your `VaultSecretRepository`.

---

### Example LLM Prompt

> I'm trying to read a KV secret from HashiCorp Vault in my Spring Boot application using Spring Cloud Vault.
>
> 1.  How do I configure my application to read from a KV secrets engine enabled at the 'secret/' path?
> 2.  Can you show me an example of how to use `VaultTemplate` to read a secret from `secret/data/apikey`?
> 3.  What is the best practice for structuring the code to separate the Vault interaction logic from the controller and service layers?

---

## Deliverables

Submit:

*   Updated source code for the `java-lab` application.
*   Updated Vault configuration scripts (`setup.sh` or other relevant files).
*   Short screenshots or console logs showing:
    *   The new policy in the Vault UI.
    *   The sample secret stored in the Vault KV engine.
    *   A `curl` or Postman request to your new `/api/secrets/apikey` endpoint successfully returning the secret value.
    *   The application logs showing a successful connection and authentication to Vault on startup.
