# Service-Analytics

This service is responsible for consuming sensor data, performing real-time analysis, and providing statistical insights and alerts. It is a Python-based microservice built with the Flask web framework.

## Technologies

- **Python**: The core application logic is written in Python.
- **Flask**: A lightweight web framework used to create the REST API.
- **Flask-CORS**: A Flask extension for handling Cross-Origin Resource Sharing (CORS), making the API accessible from the web frontend.
- **Docker**: The service is containerized for consistent deployment and scalability.

## How It Works

The service maintains an in-memory list of the last 100 sensor readings. It exposes several API endpoints that allow other services (primarily the API Gateway) to submit new data and query for analytics.

### Core Functionality

- **Data Storage**: Stores a sliding window of the most recent 100 sensor readings.
- **Statistical Analysis**: Calculates metrics such as minimum, maximum, and average for both temperature and humidity.
- **Alerting**: Checks the latest sensor reading against predefined thresholds and generates alerts for high/low temperature or humidity.

### API Endpoints

The service is accessible via the API Gateway, but its internal endpoints are defined as follows:

- **`POST /api/analytics/analyze`**: The primary endpoint used by the gateway. It accepts a sensor data JSON object, adds it to the data set, and returns a comprehensive response containing the latest stats and any new alerts.

- **`POST /api/analytics/process`**: A simpler endpoint to submit a new sensor reading.

- **`GET /api/analytics/stats`**: Returns a JSON object with the current statistics (min, max, average) for all stored readings.

- **`GET /api/analytics/alerts`**: Returns a list of any alerts triggered by the most recent sensor reading.

- **`GET /health`**: A standard health check endpoint to confirm the service is running.

## How to Run

The service is designed to be run as a Docker container and is managed by the project's main Docker Compose configuration.

### Building the Container

You can build the image using the Dockerfile provided in this directory:

```sh
docker build -t service-analytics .
```

### Running with Docker Compose

To start this service along with all others in the application stack, navigate to the project root and run:

```sh
docker-compose up
```

The Docker Compose file will automatically build the image, install the Python dependencies from `requirements.txt`, and run the Flask application.
