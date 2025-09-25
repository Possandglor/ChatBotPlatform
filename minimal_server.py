#!/usr/bin/env python3
import http.server
import socketserver
import json
import uuid
from urllib.parse import urlparse

# Тестовые данные
sessions_data = [
    {
        'session_id': 'session-001',
        'user_id': 'user-123',
        'scenario_id': 'greeting-001',
        'status': 'completed',
        'start_time': '2025-09-24T14:30:00Z',
        'message_count': 8,
        'last_message': 'Спасибо за обращение!',
    },
    {
        'session_id': 'session-002',
        'scenario_id': 'help-001',
        'status': 'active',
        'start_time': '2025-09-24T15:00:00Z',
        'message_count': 3,
        'last_message': 'Тестируем API...',
    }
]

scenarios_data = [
    {
        'id': 'greeting-001',
        'name': 'Приветствие пользователя',
        'description': 'Простой сценарий приветствия с запросом имени',
        'category': 'greeting',
        'tags': ['приветствие', 'знакомство'],
        'is_active': True,
        'created_at': '2025-09-24T14:00:00Z',
        'updated_at': '2025-09-24T14:00:00Z',
    },
    {
        'id': 'help-001',
        'name': 'Помощь пользователю',
        'description': 'Сценарий предоставления помощи',
        'category': 'support',
        'tags': ['помощь', 'поддержка'],
        'is_active': True,
        'created_at': '2025-09-24T14:00:00Z',
        'updated_at': '2025-09-24T14:00:00Z',
    }
]

class Handler(http.server.SimpleHTTPRequestHandler):
    def do_GET(self):
        self.send_response(200)
        self.send_header('Content-type', 'application/json')
        self.send_header('Access-Control-Allow-Origin', '*')
        self.end_headers()
        
        path = self.path
        
        if '/chat/status' in path:
            response = {
                "service": "chat-service", 
                "status": "running", 
                "active_sessions": len([s for s in sessions_data if s['status'] == 'active']),
                "total_sessions": len(sessions_data),
                "timestamp": 1758727000000
            }
        elif '/chat/sessions' in path:
            response = {
                "sessions": sessions_data,
                "total": len(sessions_data),
                "timestamp": 1758727000000
            }
        elif '/scenarios' in path:
            response = {
                "scenarios": scenarios_data, 
                "count": len(scenarios_data),
                "timestamp": 1758727000000
            }
        elif '/nlu/status' in path:
            response = {
                "service": "nlu-service", 
                "status": "running",
                "role": "natural_language_understanding",
                "version": "1.0.0",
                "timestamp": 1758727000000
            }
        elif '/orchestrator/status' in path:
            response = {
                "service": "orchestrator", 
                "status": "running",
                "role": "main_coordinator",
                "timestamp": 1758727000000
            }
        else:
            response = {"status": "ok", "path": path}
        
        self.wfile.write(json.dumps(response).encode())
    
    def do_POST(self):
        self.send_response(200)
        self.send_header('Content-type', 'application/json')
        self.send_header('Access-Control-Allow-Origin', '*')
        self.end_headers()
        
        path = self.path
        
        if '/chat/sessions' in path:
            new_session_id = str(uuid.uuid4())
            new_session = {
                'session_id': new_session_id,
                'status': 'active',
                'start_time': '2025-09-24T18:28:00Z',
                'message_count': 0,
                'last_message': '',
                'user_id': None,
                'scenario_id': None,
            }
            sessions_data.append(new_session)
            response = {"session_id": new_session_id, "status": "created", "timestamp": 1758727000000}
        elif '/orchestrator/process' in path:
            response = {
                "session_id": "test-session",
                "bot_response": "Привет! Я ваш банковский помощник. Що саме вас цікавить?", 
                "type": "announce",
                "context_updated": True,
                "timestamp": 1758727000000
            }
        elif '/nlu/analyze' in path:
            response = {
                "intent": "greeting",
                "confidence": 0.95,
                "entities": [],
                "suggested_scenario": "greeting-001",
                "timestamp": 1758727000000
            }
        else:
            response = {"status": "ok", "path": path}
        
        self.wfile.write(json.dumps(response).encode())
    
    def do_OPTIONS(self):
        self.send_response(200)
        self.send_header('Access-Control-Allow-Origin', '*')
        self.send_header('Access-Control-Allow-Methods', '*')
        self.send_header('Access-Control-Allow-Headers', '*')
        self.end_headers()

PORT = 8099
with socketserver.TCPServer(("", PORT), Handler) as httpd:
    print(f"🚀 Enhanced Mock API Server running on port {PORT}")
    print("📋 Available endpoints:")
    print("  - GET  /api/v1/chat/status")
    print("  - GET  /api/v1/chat/sessions")
    print("  - POST /api/v1/chat/sessions")
    print("  - GET  /api/v1/scenarios")
    print("  - GET  /api/v1/nlu/status")
    print("  - GET  /api/v1/orchestrator/status")
    print("  - POST /api/v1/orchestrator/process")
    print("  - POST /api/v1/nlu/analyze")
    print("✅ Server ready with real data!")
    httpd.serve_forever()
