#!/usr/bin/env python3
"""
Mock API сервер для тестирования глубокого извлечения JSON параметров
Запуск: python json-path-mock-server.py
"""

from flask import Flask, jsonify
from flask_cors import CORS

app = Flask(__name__)
CORS(app)

@app.route('/api/complex-data', methods=['GET'])
def complex_data():
    """Возвращает сложную JSON структуру для тестирования"""
    return jsonify({
        "service": "ChatBot Platform",
        "version": "1.0.0",
        "timestamp": "2025-01-04T12:52:49Z",
        "server": {
            "name": "orchestrator-service",
            "port": 8092,
            "status": "running",
            "stats": {
                "memory_usage": "256MB",
                "cpu_usage": "15%",
                "uptime": "2h 30m",
                "requests_count": 1247
            }
        },
        "endpoints": [
            "/api/v1/chat/message",
            "/api/v1/scenarios/execute",
            "/api/v1/nlu/analyze"
        ],
        "users": [
            {
                "id": 1,
                "name": "Олександр Петренко",
                "email": "alex@example.com",
                "profile": {
                    "avatar": "avatar1.jpg",
                    "settings": {
                        "theme": "dark",
                        "language": "uk",
                        "notifications": True
                    }
                },
                "permissions": ["read", "write", "admin"]
            },
            {
                "id": 2,
                "name": "Марія Іваненко",
                "email": "maria@example.com",
                "profile": {
                    "avatar": "avatar2.jpg",
                    "settings": {
                        "theme": "light",
                        "language": "en",
                        "notifications": False
                    }
                },
                "permissions": ["read", "write"]
            }
        ],
        "data": {
            "analytics": {
                "total_sessions": 15420,
                "active_users": 342,
                "reports": [
                    {
                        "id": "daily_report",
                        "date": "2025-01-04",
                        "metrics": {
                            "sessions": 1247,
                            "messages": 8934,
                            "conversion_rate": "23.5%",
                            "avg_response_time": "1.2s"
                        }
                    },
                    {
                        "id": "weekly_report", 
                        "date": "2025-01-01",
                        "metrics": {
                            "sessions": 8765,
                            "messages": 45231,
                            "conversion_rate": "28.1%",
                            "avg_response_time": "0.9s"
                        }
                    }
                ]
            },
            "config": {
                "max_session_duration": 3600,
                "supported_languages": ["uk", "en", "ru"],
                "features": {
                    "nlu_enabled": True,
                    "llm_integration": True,
                    "voice_support": False
                }
            }
        }
    })

@app.route('/health', methods=['GET'])
def health():
    """Health check endpoint"""
    return jsonify({"status": "ok", "service": "json-path-mock-server"})

if __name__ == '__main__':
    print("🚀 Starting JSON Path Mock Server on http://localhost:8181")
    print("📊 Test endpoint: http://localhost:8181/api/complex-data")
    app.run(host='0.0.0.0', port=8181, debug=True)
