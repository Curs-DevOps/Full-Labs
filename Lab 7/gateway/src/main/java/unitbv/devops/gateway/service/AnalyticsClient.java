package unitbv.devops.gateway.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;
import unitbv.devops.gateway.model.*;

import java.util.Collections;

@Slf4j
@Service
public class AnalyticsClient {

    private final RestTemplate restTemplate;
    private final String analyticsServiceUrl;

    public AnalyticsClient(
            RestTemplate restTemplate,
            @Value("${analytics.service.url}") String analyticsServiceUrl) {
        this.restTemplate = restTemplate;
        this.analyticsServiceUrl = analyticsServiceUrl;
    }

    public AnalyticsResponse analyzeData(SensorData sensorData) {
        try {
            log.info("Sending data to analytics service: {}", analyticsServiceUrl);
            log.debug("Sensor data: {}", sensorData);

            AnalyticsResponse response = restTemplate.postForObject(
                    analyticsServiceUrl + "/api/analytics/analyze",
                    sensorData,
                    AnalyticsResponse.class
            );

            log.info("Received analytics response: {}", response);
            return response;

        } catch (RestClientException e) {
            log.error("Error calling analytics service: {}", e.getMessage());
            return createErrorResponse(sensorData);
        }
    }

    private AnalyticsResponse createErrorResponse(SensorData sensorData) {

        // Create empty stats object
        Stats emptyStats = new Stats(
                0.0, 0.0, 0.0,
                0.0, 0.0, 0.0,
                0
        );

        Alert alert = new Alert(
                "ERROR",
                "Analytics service unavailable",
                sensorData != null ? sensorData.getSensorId() : "unknown"
        );

        AnalyticsResponse response = new AnalyticsResponse();
        response.setStatus("error");
        response.setStats(emptyStats);
        response.setAlerts(Collections.singletonList(alert));
        response.setData(sensorData);

        return response;
    }
}
