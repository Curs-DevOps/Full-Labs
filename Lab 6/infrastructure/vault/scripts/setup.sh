#!/bin/sh

# Skip TLS verification for self-signed certificates
# export VAULT_SKIP_VERIFY=1

echo "=== Configuring Vault ==="

# Enable AppRole
echo "Enabling AppRole authentication..."
vault auth enable approle 2>/dev/null || echo "AppRole already enabled"

# Create policy with read-only permissions
echo "Creating read-only policy..."
vault policy write java-lab-readonly - <<EOF
# Read database credentials only
path "database/creds/java-lab-rw" {
  capabilities = ["read"]
}
EOF

# Create AppRole with read-only policy
echo "Creating AppRole with read-only permissions..."
vault write auth/approle/role/java-lab-role \
    token_policies="java-lab-readonly" \
    token_ttl=1h \
    token_max_ttl=4h \
    secret_id_ttl=0

# Get Role ID
echo "Getting Role ID..."
ROLE_ID=$(vault read -field=role_id auth/approle/role/java-lab-role/role-id)

# Generate wrapped secret-id (valid for 5 minutes)
echo "Generating wrapped Secret ID..."
WRAP_TOKEN=$(vault write -field=wrapping_token -wrap-ttl=300s -f auth/approle/role/java-lab-role/secret-id)

# Unwrap the Secret ID using the wrapping token
echo "Unwrapping Secret ID..."
SECRET_ID=$(VAULT_TOKEN=$WRAP_TOKEN vault unwrap -field=secret_id)

echo "Role ID: $ROLE_ID"
echo "Secret ID: $SECRET_ID"
echo "Wrap Token: $WRAP_TOKEN"

# Enable database secrets
echo "Enabling database secrets engine..."
vault secrets enable database 2>/dev/null || echo "Database secrets already enabled"

# Configure database
echo "Configuring database connection..."
vault write database/config/authdb \
    plugin_name="postgresql-database-plugin" \
    allowed_roles="java-lab-rw" \
    connection_url="postgresql://{{username}}:{{password}}@postgres:5432/authdb" \
    username="authuser" \
    password="authpass" \
    password_authentication="scram-sha-256"

# Create database role
echo "Creating database role..."
vault write database/roles/java-lab-rw \
    db_name="authdb" \
    creation_statements="CREATE ROLE \"{{name}}\" WITH LOGIN PASSWORD '{{password}}' VALID UNTIL '{{expiration}}'; \
        GRANT CONNECT ON DATABASE authdb TO \"{{name}}\"; \
        GRANT USAGE ON SCHEMA public TO \"{{name}}\"; \
        GRANT CREATE ON SCHEMA public TO \"{{name}}\"; \
        GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO \"{{name}}\"; \
        ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO \"{{name}}\";" \
    default_ttl="1h" \
    max_ttl="24h"

# Test credential generation
echo ""
echo "=== Testing Credential Generation ==="
vault read database/creds/java-lab-rw

# Save credentials to shared volume
echo "$ROLE_ID" > /vault-creds/role-id
echo "$SECRET_ID" > /vault-creds/secret-id
echo "$WRAP_TOKEN" > /vault-creds/wrap-token

echo ""
echo "✅ Vault configuration complete!"
echo "AppRole has READ-ONLY access to database credentials"
