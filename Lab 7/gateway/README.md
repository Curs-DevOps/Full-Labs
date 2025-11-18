# Gateway Service

This service acts as the central API Gateway for the entire application. It is the single entry point for the frontend, providing a unified API that orchestrates calls to the various backend microservices (`service-sensor` and `service-analytics`). It is a Java application built using the Spring Boot framework.

## Technologies

- **Java 17**: The core application logic is written in Java.
- **Spring Boot**: The application is built on the Spring Boot framework, which simplifies the creation of stand-alone, production-grade Spring-based applications.
- **Spring Web**: Used to create the RESTful API endpoints.
- **RestTemplate**: The standard Spring mechanism used for making synchronous HTTP requests to the other microservices.
- **Lombok**: A Java library used to reduce boilerplate code for models and logging.
- **Maven**: The project is built and managed using Apache Maven.
- **Docker**: The service is containerized for easy deployment.

## How It Works

The Gateway exposes a set of endpoints that the frontend can call. When a request comes in, the Gateway, in turn, calls the other microservices to fulfill the request. This decouples the frontend from the backend services, simplifying the overall architecture.

### Core Workflow (`/api/gateway/data`)

1.  The Gateway receives a `GET` request on its primary endpoint.
2.  It makes an HTTP `GET` request to `service-sensor` to fetch the latest sensor data.
3.  It then makes an HTTP `POST` request to `service-analytics`, sending the newly acquired sensor data in the request body.
4.  `service-analytics` processes the data and returns a JSON object with statistics and any triggered alerts.
5.  The Gateway combines the original sensor data with the analytics response into a single, unified JSON object.
6.  This combined response is sent back to the client (the frontend).

### Service Communication

- **`SensorClient`**: A dedicated service class responsible for communicating with `service-sensor`. It makes a `GET` request to the sensor's IP address and port.
- **`AnalyticsClient`**: A service class for communicating with `service-analytics`. It makes a `POST` request, sending sensor data to the analytics endpoint.

The URLs for the downstream services are configured in the `application.properties` file and injected into these clients, allowing for flexible configuration in different environments (e.g., via Docker Compose environment variables).

### API Endpoints

- **`GET /api/gateway/data`**: The main endpoint. Fetches data from the sensor, gets analytics for it, and returns a combined response.
- **`GET /api/gateway/health`**: A health check endpoint to verify that the Gateway service is operational.
- **`GET /api/gateway/sensor`**: A passthrough endpoint to get data directly from the sensor service without hitting the analytics service.
- **`POST /api/gateway/analytics`**: A passthrough endpoint that allows a client to post sensor data and get an analytics response for it.

## How to Run

The service is designed to be run as a Docker container and is managed by the project's main Docker Compose configuration.

### Building the Container

The project uses a multi-stage Dockerfile that first builds the application using Maven and then creates a slim final image with the compiled JAR file.

You can build the image manually:

```sh
# From the gateway directory
docker build -t gateway-service .
```

### Running with Docker Compose

To start this service along with all others, navigate to the project root and run:

```sh
docker-compose up
```

The Docker Compose file will automatically build the image and run the container, injecting the necessary environment variables for the downstream service URLs.
