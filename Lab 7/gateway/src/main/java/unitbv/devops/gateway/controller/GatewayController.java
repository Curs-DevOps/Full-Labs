package unitbv.devops.gateway.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import unitbv.devops.gateway.model.AnalyticsResponse;
import unitbv.devops.gateway.model.GatewayResponse;
import unitbv.devops.gateway.model.SensorData;
import unitbv.devops.gateway.service.AnalyticsClient;
import unitbv.devops.gateway.service.SensorClient;

@Slf4j
@RestController
@RequestMapping("/api/gateway")
@CrossOrigin(origins = "*")
public class GatewayController {

    private final SensorClient sensorClient;
    private final AnalyticsClient analyticsClient;

    public GatewayController(SensorClient sensorClient, AnalyticsClient analyticsClient) {
        this.sensorClient = sensorClient;
        this.analyticsClient = analyticsClient;
    }

    /**
     * Main endpoint: Fetches sensor data and gets analytics
     * GET /api/gateway/data
     */
    @GetMapping("/data")
    public ResponseEntity<GatewayResponse> getSensorDataWithAnalytics() {
        try {
            log.info("Received request for sensor data with analytics");

            // Step 1: Get sensor data from C++ service
            SensorData sensorData = sensorClient.getSensorData();
            log.debug("Sensor data retrieved: {}", sensorData);

            // Step 2: Send sensor data to Python analytics service
            AnalyticsResponse analytics = analyticsClient.analyzeData(sensorData);
            log.debug("Analytics received: {}", analytics);

            // Step 3: Combine and return response
            GatewayResponse response = new GatewayResponse(sensorData, analytics);
            log.info("Returning combined response");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error processing request: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Health check endpoint
     * GET /api/gateway/health
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        log.info("Health check requested");
        return ResponseEntity.ok("Gateway service is running");
    }

    /**
     * Get only sensor data (without analytics)
     * GET /api/gateway/sensor
     */
    @GetMapping("/sensor")
    public ResponseEntity<SensorData> getSensorDataOnly() {
        try {
            log.info("Received request for sensor data only");
            SensorData sensorData = sensorClient.getSensorData();
            return ResponseEntity.ok(sensorData);

        } catch (Exception e) {
            log.error("Error fetching sensor data: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get only analytics for provided sensor data
     * POST /api/gateway/analytics
     */
    @PostMapping("/analytics")
    public ResponseEntity<AnalyticsResponse> getAnalyticsOnly(@RequestBody SensorData sensorData) {
        try {
            log.info("Received request for analytics only");
            log.debug("Sensor data: {}", sensorData);

            AnalyticsResponse analytics = analyticsClient.analyzeData(sensorData);
            return ResponseEntity.ok(analytics);

        } catch (Exception e) {
            log.error("Error fetching analytics: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}