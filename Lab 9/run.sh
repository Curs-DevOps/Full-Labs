#!/bin/bash
echo "Starting Full Stack (Infra + App)..."
docker-compose -p infra -f infra/docker-compose.yml up --build -d

echo "Waiting for Grafana to be ready..."
# Loop until Grafana health check returns 200 OK
until curl -sf http://localhost:3000/api/health > /dev/null; do
    echo "Waiting for Grafana..."
    sleep 5
done
echo "Grafana is up!"

# Start the app as a separate project, connecting to the same network
docker-compose -p infra-app -f app-compose.yml up --build -d app
