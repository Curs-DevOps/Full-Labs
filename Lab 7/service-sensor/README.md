# Service-Sensor

This service simulates a real-time IoT sensor that generates temperature and humidity data. It's a standalone C++ application that implements a basic HTTP server from scratch using raw sockets to expose a single API endpoint.

## Technologies

- **C++**: The core application logic is written in C++.
- **Sockets**: The HTTP server is built using the native socket libraries (`<sys/socket.h>` on Linux/macOS and `<winsock2.h>` on Windows) to handle network communication.
- **Docker**: The service is containerized for easy deployment and integration with the rest of the system.

## How It Works

The service runs a continuous loop, listening for incoming TCP connections. When a client (like the API Gateway) connects, the service generates a new set of sensor data, formats it as a JSON object, wraps it in an HTTP response, and sends it back to the client.

### API Endpoint

- **Endpoint**: `/`
- **Method**: `GET`
- **Response**: A JSON object containing the latest sensor data.
  ```json
  {
    "sensor_id": "SENSOR-1",
    "status": "OK",
    "temperature": 22.5,
    "humidity": 55.8,
    "timestamp": "Tue Nov 18 10:00:00 2025"
  }
  ```

### Deep Dive: The HTTP Server from Scratch

The server is implemented without any external web frameworks, relying solely on the C++ standard library and the platform's native socket API. Here is a breakdown of how it works, function by function:

1.  **`socket(AF_INET, SOCK_STREAM, 0)`**
    - **Purpose**: Creates a new socket (a network communication endpoint).
    - **`AF_INET`**: Specifies the address family, in this case, IPv4.
    - **`SOCK_STREAM`**: Specifies the socket type, which provides a reliable, two-way, connection-based byte stream (TCP).
    - **`0`**: Specifies the protocol. For `SOCK_STREAM` with `AF_INET`, this defaults to TCP.
    - **Returns**: A file descriptor (an integer) for the new socket, or -1 on error.

2.  **`setsockopt(serverSocket, SOL_SOCKET, SO_REUSEADDR, ...)`**
    - **Purpose**: Configures the socket to allow immediate reuse of the port after the server is shut down. This is useful for development, as it prevents "Address already in use" errors when restarting the server quickly.

3.  **`bind(serverSocket, ...)`**
    - **Purpose**: Assigns a specific address and port to the created socket.
    - **`serverSocket`**: The file descriptor of our socket.
    - **`struct sockaddr_in`**: A structure holding the address information:
        - `sin_family`: Must be `AF_INET`.
        - `sin_addr.s_addr`: The IP address to bind to. `INADDR_ANY` means the server will accept connections on any of the machine's available network interfaces.
        - `sin_port`: The port number. `htons(8080)` converts the port number `8080` from host byte order to network byte order, which is essential for network communication.
    - **Returns**: 0 on success, -1 on error.

4.  **`listen(serverSocket, 3)`**
    - **Purpose**: Puts the socket into a passive "listening" state, waiting for incoming client connections.
    - **`serverSocket`**: The file descriptor of our bound socket.
    - **`3`**: The "backlog" size—the maximum number of pending connections that can be queued up before the system starts refusing new ones.

5.  **`accept(serverSocket, NULL, NULL)`**
    - **Purpose**: Accepts an incoming connection from the queue.
    - **Behavior**: This function blocks (waits) until a client connects.
    - **Returns**: A **new** socket file descriptor for the accepted connection. All communication with this specific client will happen through this new socket, while the original `serverSocket` remains open, listening for other new connections.

6.  **`std::thread(handleClient, ...).detach()`**
    - **Purpose**: To handle multiple clients concurrently, a new thread is created for each accepted connection.
    - **`handleClient`**: This function contains the logic for interacting with the client.
    - **`.detach()`**: The thread is detached, meaning the main server loop doesn't have to wait for it to finish. It runs independently until the client communication is complete.

7.  **`recv(clientSocket, buffer, 1024, 0)`**
    - **Purpose**: Reads data from the client's socket into a buffer. In this simple server, we read the incoming HTTP request but don't process it, as the server always sends the same type of response.

8.  **`send(clientSocket, responseStr.c_str(), ...)`**
    - **Purpose**: Sends data back to the client.
    - **Logic**: The code manually constructs a valid HTTP/1.1 response string. This includes:
        - The status line: `HTTP/1.1 200 OK`
        - Headers: `Content-Type`, `Content-Length`, and `Access-Control-Allow-Origin` (to permit cross-origin requests from the frontend).
        - An empty line (`\r\n`) to separate headers from the body.
        - The body: The JSON data string.
    - This raw string is then sent over the client's socket.

9.  **`close(clientSocket)`**
    - **Purpose**: Closes the connection with the specific client once the response has been sent, freeing up the socket descriptor.

## How to Run

The service is designed to be run as a Docker container.

### Building the Container

You can build the image using the Dockerfile provided in this directory:

```sh
docker build -t service-sensor .
```

### Running with Docker Compose

The entire application stack is managed by the `docker-compose.yml` file in the project root. To start this service along with all others, run:

```sh
docker-compose up
```

The Docker Compose file will automatically build the image and run the container.
