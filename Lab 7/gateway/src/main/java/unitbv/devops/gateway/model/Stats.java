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
public class Stats {

    @JsonProperty("average_temperature")
    private double averageTemperature;

    @JsonProperty("min_temperature")
    private double minTemperature;

    @JsonProperty("max_temperature")
    private double maxTemperature;

    @JsonProperty("average_humidity")
    private double averageHumidity;

    @JsonProperty("min_humidity")
    private double minHumidity;

    @JsonProperty("max_humidity")
    private double maxHumidity;

    @JsonProperty("total_readings")
    private int totalReadings;

    @Override
    public String toString() {
        return "Stats{" +
                "averageTemperature=" + averageTemperature +
                ", minTemperature=" + minTemperature +
                ", maxTemperature=" + maxTemperature +
                ", averageHumidity=" + averageHumidity +
                ", minHumidity=" + minHumidity +
                ", maxHumidity=" + maxHumidity +
                ", totalReadings=" + totalReadings +
                '}';
    }
}
