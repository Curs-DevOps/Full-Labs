package unitbv.devops.gateway.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsResponse {

    private String status;

    private Stats stats;

    private List<Alert> alerts;

    @JsonProperty("data")
    private SensorData data;
}
