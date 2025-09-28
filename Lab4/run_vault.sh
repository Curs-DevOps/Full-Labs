#!/bin/bash

podman run -d --cap-add=IPC_LOCK \
  -v "$(pwd)/config:/vault/config.d" \
  -v vault-data:/vault/file \
  -p 8200:8200 \
  hashicorp/vault:1.20.0 \
  server -config=/vault/config.d/vault.hcl
