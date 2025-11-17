from models import SensorReading
from statistics import mean

class AnalyticsService:
    def __init__(self):
        self.readings = []
        self.temp_threshold_high = 30
        self.temp_threshold_low = 10
        self.humidity_threshold_high = 80
        self.humidity_threshold_low = 20
    
    def add_reading(self, reading):
        """Add a new sensor reading"""
        self.readings.append(reading)
        if len(self.readings) > 100:
            self.readings.pop(0)
    
    def get_stats(self):
        """Calculate statistics from readings"""
        if not self.readings:
            return None
        
        temps = [r.temperature for r in self.readings]
        humidities = [r.humidity for r in self.readings]
        
        return {
            'average_temperature': round(mean(temps), 2),
            'min_temperature': round(min(temps), 2),
            'max_temperature': round(max(temps), 2),
            'average_humidity': round(mean(humidities), 2),
            'min_humidity': round(min(humidities), 2),
            'max_humidity': round(max(humidities), 2),
            'total_readings': len(self.readings)
        }
    
    def check_alerts(self):
        """Check for any alerts based on thresholds"""
        if not self.readings:
            return []
        
        alerts = []
        latest = self.readings[-1]
        
        if latest.temperature > self.temp_threshold_high:
            alerts.append({
                'type': 'HIGH_TEMPERATURE',
                'message': f'Temperature {latest.temperature}°C exceeds threshold {self.temp_threshold_high}°C',
                'sensor_id': latest.sensor_id
            })
        
        if latest.temperature < self.temp_threshold_low:
            alerts.append({
                'type': 'LOW_TEMPERATURE',
                'message': f'Temperature {latest.temperature}°C below threshold {self.temp_threshold_low}°C',
                'sensor_id': latest.sensor_id
            })
        
        if latest.humidity > self.humidity_threshold_high:
            alerts.append({
                'type': 'HIGH_HUMIDITY',
                'message': f'Humidity {latest.humidity}% exceeds threshold {self.humidity_threshold_high}%',
                'sensor_id': latest.sensor_id
            })
        
        if latest.humidity < self.humidity_threshold_low:
            alerts.append({
                'type': 'LOW_HUMIDITY',
                'message': f'Humidity {latest.humidity}% below threshold {self.humidity_threshold_low}%',
                'sensor_id': latest.sensor_id
            })
        
        return alerts