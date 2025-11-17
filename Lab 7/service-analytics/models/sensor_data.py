class SensorReading:
    def __init__(self, temperature, humidity, sensor_id):
        self.temperature = temperature
        self.humidity = humidity
        self.sensor_id = sensor_id
    
    def to_dict(self):
        return {
            'temperature': self.temperature,
            'humidity': self.humidity,
            'sensor_id': self.sensor_id
        }
    
    @staticmethod
    def from_dict(data):
        return SensorReading(
            temperature=data.get('temperature', 0),
            humidity=data.get('humidity', 0),
            sensor_id=data.get('sensor_id', 'unknown')
        )