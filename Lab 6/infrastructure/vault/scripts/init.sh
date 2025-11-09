#!/bin/sh

echo "=== Initializing Vault ==="

# Check if vault is already initialized
if vault status 2>/dev/null | grep -q "Initialized.*true"; then
    echo "Vault is already initialized"

    # Check if we have saved keys
    if [ -f /vault-keys/unseal-key ]; then
        echo "Using saved unseal key"
        UNSEAL_KEY=$(cat /vault-keys/unseal-key)
        ROOT_TOKEN=$(cat /vault-keys/root-token)
    else
        echo "Vault is initialized but keys are missing!"
        echo "You need to manually unseal vault and provide the root token"
        exit 1
    fi
else
    echo "Initializing Vault for the first time..."

    # Initialize vault with 1 key share and 1 key threshold (for development)
    INIT_OUTPUT=$(vault operator init -key-shares=1 -key-threshold=1 -format=json)

    UNSEAL_KEY=$(echo "$INIT_OUTPUT" | jq -r '.unseal_keys_b64[0]')
    ROOT_TOKEN=$(echo "$INIT_OUTPUT" | jq -r '.root_token')

    if [ -z "$UNSEAL_KEY" ] || [ -z "$ROOT_TOKEN" ]; then
      echo "Failed to parse Vault init output."
      exit 1
    fi

    # Save keys to volume (for persistence across restarts)
    echo "$UNSEAL_KEY" > /vault-keys/unseal-key
    echo "$ROOT_TOKEN" > /vault-keys/root-token
    chmod 600 /vault-keys/*

    echo "Vault initialized successfully"
    echo ""
    echo "IMPORTANT: Save these credentials securely!"
    echo "Unseal Key: $UNSEAL_KEY"
    echo "Root Token: $ROOT_TOKEN"
    echo ""
fi

# Unseal vault
if vault operator unseal "$UNSEAL_KEY"; then
    echo "Vault unsealed successfully"
else
    echo "Failed to unseal Vault"
    exit 1
fi

# Export root token for setup script
export VAULT_TOKEN="$ROOT_TOKEN"
echo "$ROOT_TOKEN" > /vault-keys/current-token

echo "Vault unsealed and ready"
