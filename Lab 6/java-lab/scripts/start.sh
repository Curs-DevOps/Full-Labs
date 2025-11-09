#!/bin/sh

set -e

echo "=== Starting Java Lab Application ==="

# Wait for credentials to be available
echo "Waiting for Vault credentials..."
while [ ! -f /vault-creds/role-id ] || [ ! -f /vault-creds/secret-id ] || [ ! -f /vault-creds/wrap-token ]; do
  sleep 1
done

# Read credentials
export VAULT_ROLE_ID=$(cat /vault-creds/role-id)
export VAULT_SECRET_ID=$(cat /vault-creds/secret-id)
export VAULT_WRAP_TOKEN=$(cat /vault-creds/wrap-token)

echo "Credentials loaded successfully"
echo "Role ID: $VAULT_ROLE_ID"
echo "Secret ID: ${VAULT_SECRET_ID:0:20}..."
echo "Wrap Token: ${VAULT_WRAP_TOKEN:0:20}..."

# Start Spring Boot
echo "Starting Spring Boot application..."
#
# Added the JVM flags for container support
#
exec java -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -jar /app/app.jar
