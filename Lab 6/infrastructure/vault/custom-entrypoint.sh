#!/bin/sh

export VAULT_ADDR="http://vault:8200"

# 1. Start the original Vault entrypoint in the background.
# This will find the 'CMD' (e.g., "server -config ...") and run it.
echo "Starting Vault server in the background..."
echo "CMD: $@"
/usr/local/bin/docker-entrypoint.sh "$@" &

# Get the Process ID of the backgrounded Vault server
VAULT_PID=$!

# 2. Wait for Vault to be ready to accept commands
echo "Waiting for Vault server to be ready..."

# We poll 'vault status' until we stop getting connection errors.
# We are ready to proceed if the server reports it is "Initialized" (and sealed)
# OR if it reports "server is not yet initialized" (ready for init).
while true; do
  # Get the status output (both stdout and stderr)
  STATUS_OUTPUT=$(vault status 2>&1)

  # Print the status for debugging
  echo "Current Vault status: $STATUS_OUTPUT"

  # Check for the ready conditions
  if echo "$STATUS_OUTPUT" | grep -q -e "Initialized" -e "server is not yet initialized"; then
    echo "Vault server is up and listening."
    break # Exit the loop
  fi

  echo "Waiting for Vault server to be ready..."
  sleep 1
done

# 3. Run the init.sh script (initializes and unseals)
echo "Running init.sh..."
/vault/scripts/init.sh
INIT_STATUS=$?

# Check if init.sh failed
if [ $INIT_STATUS -ne 0 ]; then
  echo "Error running init.sh. Exiting."
  # Stop the background server and exit
  kill $VAULT_PID
  exit $INIT_STATUS
fi

# 4. Run the setup.sh script (configures AppRole, DB, etc.)
# We must read the token from the file, as the 'export' in init.sh
# does not carry over to this script's environment.
echo "Running setup.sh..."
export VAULT_TOKEN=$(cat /vault-keys/current-token)

if [ -z "$VAULT_TOKEN" ]; then
    echo "Could not read root token from /vault-keys/current-token. Cannot run setup."
    kill $VAULT_PID
    exit 1
fi

/vault/scripts/setup.sh
SETUP_STATUS=$?

# Check if setup.sh failed
if [ $SETUP_STATUS -ne 0 ]; then
  echo "Error running setup.sh. Exiting."
  kill $VAULT_PID
  exit $SETUP_STATUS
fi

echo "✅ Init and setup complete. Vault is running."

# 5. Bring the Vault server process to the foreground
# This ensures the container stays running and can be stopped gracefully (e.g., with 'docker stop')
wait $VAULT_PID
