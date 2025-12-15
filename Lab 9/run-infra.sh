#!/bin/bash
echo "Starting Observability Stack (Infra only)..."
docker-compose -f infra/docker-compose.yml up
