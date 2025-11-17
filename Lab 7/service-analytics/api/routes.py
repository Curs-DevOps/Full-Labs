from flask import Blueprint, jsonify, request
from models import SensorReading

api = Blueprint('api', __name__, url_prefix='/api')

analytics_service = None


def init_routes(service):
    global analytics_service
    analytics_service = service


@api.route('/analytics/process', methods=['POST'])
def process_data():
    try:
        data = request.json
        if not data:
            return jsonify({'error': 'No data provided'}), 400
        
        reading = SensorReading.from_dict(data)
        analytics_service.add_reading(reading)
        
        return jsonify({
            'message': 'Data processed',
            'total_readings': len(analytics_service.readings)
        }), 200
        
    except Exception as e:
        return jsonify({'error': str(e)}), 500


@api.route('/analytics/stats', methods=['GET'])
def get_stats():
    try:
        stats = analytics_service.get_stats()
        if not stats:
            return jsonify({'error': 'No data available'}), 404
        return jsonify(stats), 200
    except Exception as e:
        return jsonify({'error': str(e)}), 500


@api.route('/analytics/alerts', methods=['GET'])
def get_alerts():
    try:
        alerts = analytics_service.check_alerts()
        return jsonify({'alerts': alerts, 'count': len(alerts)}), 200
    except Exception as e:
        return jsonify({'error': str(e)}), 500


@api.route('/analytics/analyze', methods=['POST'])
def analyze_data():
    """Gateway sends sensor data here for instant analysis"""
    try:
        data = request.json
        if not data:
            return jsonify({'error': 'No data provided'}), 400
        
        reading = SensorReading.from_dict(data)
        analytics_service.add_reading(reading)
        
        stats = analytics_service.get_stats()
        alerts = analytics_service.check_alerts()
        
        return jsonify({
            'status': 'success',
            'data': reading.to_dict(),
            'stats': stats,
            'alerts': alerts
        }), 200
        
    except Exception as e:
        return jsonify({'error': str(e)}), 500