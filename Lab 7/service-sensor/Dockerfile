# Use GCC base image
FROM gcc:latest

# Set working directory
WORKDIR /app

# Copy source file
COPY src/main.cpp .

# Compile directly (no CMake needed)
RUN g++ -o sensor_service main.cpp -pthread -std=c++17

# Expose port
EXPOSE 8080

# Run the service
CMD ["./sensor_service"]