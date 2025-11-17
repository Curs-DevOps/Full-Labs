package unitbv.devops.gateway.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GatewayResponse {
    @JsonProperty("sensor_data")
    private SensorData sensorData;

    @JsonProperty("analytics")
    private AnalyticsResponse analytics;

    @Override
    public String toString() {
        return "GatewayResponse{" +
                "sensorData=" + sensorData +
                ", analytics=" + analytics +
                '}';
    }
}