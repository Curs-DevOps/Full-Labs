# IoT Sensor Monitoring Application

This project is a complete, full-stack application that demonstrates a microservices-based architecture for monitoring and analyzing data from an IoT sensor. The system is composed of four independent services, all containerized with Docker and managed by Docker Compose.

## Architecture

The application follows a classic microservice pattern, with a central gateway that acts as the single entry point for the user-facing frontend.

![Architecture Diagram](https://i.imgur.com/9yZ3B5s.png)

1.  **Frontend**: A single-page web application that provides a dashboard for visualizing sensor data and analytics in real-time.
2.  **API Gateway**: A Spring Boot service that routes requests from the frontend to the appropriate backend services. It orchestrates calls to fetch data and then analyze it.
3.  **Sensor Service**: A C++ application that simulates an IoT device. It runs a bare-metal HTTP server built from scratch with sockets and serves randomly generated temperature and humidity data.
4.  **Analytics Service**: A Python Flask service that consumes sensor data, calculates statistics (min, max, average), and generates alerts based on predefined thresholds.

## Services

| Service                                                   | Technology      | Description                                                                                             |
| --------------------------------------------------------- | --------------- | ------------------------------------------------------------------------------------------------------- |
| [Frontend](./frontend/README.md)                          | HTML, CSS, JS   | A static web dashboard served by Nginx that polls the gateway for live data.                            |
| [API Gateway](./gateway/README.md)                        | Java (Spring)   | The central entry point. Fetches data from the sensor and enriches it with data from the analytics service. |
| [Analytics Service](./service-analytics/README.md)        | Python (Flask)  | Consumes sensor data, calculates statistics, and checks for alerts.                                     |
| [Sensor Service](./service-sensor/README.md)              | C++             | Simulates a sensor and exposes an API built from scratch with raw sockets.                              |

For more detailed information about each service, please refer to the `README.md` file located in its respective directory.

## How to Run the Application

The entire application stack is designed to be run with Docker and Docker Compose.

### Prerequisites

- **Docker**: Ensure Docker is installed and running on your system.
- **Docker Compose**: Ensure you have Docker Compose installed (it is included with Docker Desktop).

### Running the Stack

1.  **Clone the repository** (if you haven't already).

2.  **Navigate to the project root directory** (where this `README.md` file is located).

3.  **Run Docker Compose**:
    ```sh
    docker-compose up --build
    ```
    - The `--build` flag tells Docker Compose to build the images for each service from their respective Dockerfiles before starting the containers.
    - The services will start in the correct order, as `depends_on` conditions are set in the `docker-compose.yml` file to manage startup dependencies.

### Accessing the Application

Once all the containers are up and running, you can access the different parts of the system:

- **Frontend Dashboard**:
  - URL: `http://localhost:8082`
  - This is the main user interface. Click "Start Live Updates" to begin polling for data.

- **API Gateway**:
  - URL: `http://localhost:8081/api/gateway/data`
  - This endpoint provides the combined data feed that the frontend consumes.

- **Sensor Service**:
  - URL: `http://localhost:8080`
  - You can directly access the raw C++ sensor service here.

- **Analytics Service**:
  - URL: `http://localhost:6000/health`
  - You can hit the health check endpoint of the analytics service to confirm it's running.

### Stopping the Application

To stop all the running containers, press `Ctrl + C` in the terminal where `docker-compose` is running, or run the following command from the project root in another terminal:

```sh
docker-compose down
```
