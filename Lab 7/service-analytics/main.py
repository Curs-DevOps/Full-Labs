from flask import Flask, jsonify
from flask_cors import CORS
from services.analytics_service import AnalyticsService
from api.routes import api, init_routes


def create_app():
    app = Flask(__name__)
    CORS(app)
    
    analytics_service = AnalyticsService()
    init_routes(analytics_service)
    app.register_blueprint(api)
    
    @app.route('/')
    def home():
        return jsonify({
            'service': 'Analytics Service',
            'status': 'running',
            'endpoints': {
                'health': '/health',
                'process': '/api/analytics/process',
                'stats': '/api/analytics/stats',
                'alerts': '/api/analytics/alerts',
                'analyze': '/api/analytics/analyze'
            }
        })
    
    @app.route('/health')
    def health():
        return jsonify({'status': 'healthy', 'service': 'analytics-service'})
    
    return app


app = create_app()

if __name__ == '__main__':
    print("Starting Analytics Service on http://localhost:5000")
    app.run(host='0.0.0.0', port=5000, debug=True)