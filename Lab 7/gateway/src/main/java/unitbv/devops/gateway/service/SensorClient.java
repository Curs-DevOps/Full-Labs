package unitbv.devops.gateway.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;
import unitbv.devops.gateway.model.SensorData;

@Slf4j
@Service
public class SensorClient {

    private final RestTemplate restTemplate;
    private final String sensorServiceUrl;

    public SensorClient(
            RestTemplate restTemplate,
            @Value("${sensor.service.url}") String sensorServiceUrl) {
        this.restTemplate = restTemplate;
        this.sensorServiceUrl = sensorServiceUrl;
    }

    /**
     * Fetches sensor data from C++ sensor service
     * @return SensorData from the sensor
     */
    public SensorData getSensorData() {
        try {
            log.info("Fetching data from sensor service: {}", sensorServiceUrl);

            SensorData sensorData = restTemplate.getForObject(
                    sensorServiceUrl,
                    SensorData.class
            );

            log.info("Received sensor data: {}", sensorData);
            return sensorData;

        } catch (RestClientException e) {
            log.error("Error calling sensor service: {}", e.getMessage());
            return createErrorResponse();
        }
    }

    /**
     * Creates a default error response when sensor service is unavailable
     */
    private SensorData createErrorResponse() {
        SensorData errorData = new SensorData();
        errorData.setSensorId("ERROR");
        errorData.setTemperature(0.0);
        errorData.setHumidity(0.0);
        errorData.setTimestamp(java.time.LocalDateTime.now().toString());
        errorData.setStatus("Sensor service unavailable");
        return errorData;
    }
}