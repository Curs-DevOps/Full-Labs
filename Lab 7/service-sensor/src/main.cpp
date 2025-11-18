#include <iostream> // For input/output operations (like std::cout, std::cerr)
#include <thread>   // For creating and managing threads (to handle multiple clients)
#include <chrono>   // For time-related functions (though not directly used for timing here)
#include <ctime>    // For getting the current time (used for timestamping sensor data)
#include <sstream>  // For string stream manipulation (used to build JSON and HTTP responses)
#include <cstring>  // For C-style string manipulation (like memset for buffer)

// Conditional includes for Windows vs. Linux/macOS socket programming
#ifdef _WIN32
    #include <winsock2.h> // Windows Sockets API header
    #pragma comment(lib, "ws2_32.lib") // Link with the Winsock library
#else
    #include <sys/socket.h> // Standard socket definitions
    #include <netinet/in.h> // Internet address family (AF_INET)
    #include <unistd.h>     // For close() function (to close sockets)
#endif

// Structure to hold sensor data
struct SensorData {
    double temperature;
    double humidity;
    std::string timestamp;
    std::string sensorId;
    std::string status;
};

// Class to simulate a sensor and generate data
class Sensor {
private:
    double currentTemp;     // Current temperature value
    double currentHumidity; // Current humidity value

public:
    // Constructor: Initializes sensor with default values
    Sensor() : currentTemp(20.0), currentHumidity(50.0) {}

    // Generates new sensor data with slight variations
    SensorData generateData() {
        SensorData data;
        
        // Simulate temperature and humidity fluctuations
        currentTemp += ((rand() % 100) - 50) / 100.0; // +/- 0.5 degree
        currentHumidity += ((rand() % 100) - 50) / 100.0; // +/- 0.5 percent

        // Keep values within a realistic range
        if (currentTemp < 15.0) currentTemp = 15.0;
        if (currentTemp > 30.0) currentTemp = 30.0;
        if (currentHumidity < 30.0) currentHumidity = 30.0;
        if (currentHumidity > 80.0) currentHumidity = 80.0;

        data.temperature = currentTemp;
        data.humidity = currentHumidity;

        // Get current timestamp
        time_t now = time(0);
        data.timestamp = ctime(&now);
        // Remove trailing newline character from ctime output
        if (!data.timestamp.empty() && data.timestamp.back() == '\n') {
            data.timestamp.pop_back();
        }

        data.sensorId = "SENSOR-1"; // Hardcoded sensor ID
        data.status   = "OK";       // Hardcoded status

        return data;
    }

    // Converts SensorData object to a JSON string
    std::string toJSON(const SensorData& data) {
        std::ostringstream json;

        json << "{"
            << "\"sensor_id\":\"" << data.sensorId << "\","
            << "\"status\":\"" << data.status << "\","
            << "\"temperature\":" << data.temperature << ","
            << "\"humidity\":" << data.humidity << ","
            << "\"timestamp\":\"" << data.timestamp << "\""
            << "}";
        return json.str();
    }
};

void handleClient(int clientSocket, Sensor& sensor) {
    char buffer[1024] = {0};
    recv(clientSocket, buffer, 1024, 0);
    
    SensorData data = sensor.generateData();
    std::string jsonData = sensor.toJSON(data);
    
    std::ostringstream response;
    response << "HTTP/1.1 200 OK\r\n"
             << "Content-Type: application/json\r\n"
             << "Access-Control-Allow-Origin: *\r\n"
             << "Content-Length: " << jsonData.length() << "\r\n"
             << "\r\n"
             << jsonData;
    
    std::string responseStr = response.str();
    send(clientSocket, responseStr.c_str(), responseStr.length(), 0);
    
#ifdef _WIN32
    closesocket(clientSocket);
#else
    close(clientSocket);
#endif
}

int main() {
    std::cout << "Sensor Service Starting..." << std::endl;
    
#ifdef _WIN32
    WSADATA wsaData;
    WSAStartup(MAKEWORD(2, 2), &wsaData);
#endif
    
    int serverSocket = socket(AF_INET, SOCK_STREAM, 0);
    if (serverSocket < 0) {
        std::cerr << "Socket creation failed!" << std::endl;
        return 1;
    }
    
    int opt = 1;
    setsockopt(serverSocket, SOL_SOCKET, SO_REUSEADDR, (char*)&opt, sizeof(opt));
    
    struct sockaddr_in address;
    address.sin_family = AF_INET;
    address.sin_addr.s_addr = INADDR_ANY;
    address.sin_port = htons(8080);
    
    if (bind(serverSocket, (struct sockaddr*)&address, sizeof(address)) < 0) {
        std::cerr << "Bind failed!" << std::endl;
        return 1;
    }
    
    if (listen(serverSocket, 3) < 0) {
        std::cerr << "Listen failed!" << std::endl;
        return 1;
    }
    
    std::cout << "Sensor Service running on port 8080" << std::endl;
    
    Sensor sensor;
    
    while (true) {
        int clientSocket = accept(serverSocket, NULL, NULL);
        if (clientSocket >= 0) {
            std::thread(handleClient, clientSocket, std::ref(sensor)).detach();
        }
    }
    
#ifdef _WIN32
    closesocket(serverSocket);
    WSACleanup();
#else
    close(serverSocket);
#endif
    
    return 0;
}