#!/usr/bin/env python3
import http.server
import socketserver
import json
import uuid
import datetime
from urllib.parse import urlparse, parse_qs
import threading

# In-memory storage
sessions = {}
scenarios = [
    {
        'id': 'greeting-001',
        'name': 'Приветствие пользователя',
        'description': 'Простой сценарий приветствия с запросом имени',
        'category': 'greeting',
        'tags': ['приветствие', 'знакомство'],
        'is_active': True,
        'created_at': datetime.datetime.now().isoformat() + 'Z',
        'updated_at': datetime.datetime.now().isoformat() + 'Z',
    },
    {
        'id': 'help-001',
        'name': 'Помощь пользователю',
        'description': 'Сценарий предоставления помощи',
        'category': 'support',
        'tags': ['помощь', 'поддержка'],
        'is_active': True,
        'created_at': datetime.datetime.now().isoformat() + 'Z',
        'updated_at': datetime.datetime.now().isoformat() + 'Z',
    }
]

# Тестовые сессии
sessions['session-001'] = {
    'session_id': 'session-001',
    'user_id': 'user-123',
    'scenario_id': 'greeting-001',
    'status': 'completed',
    'start_time': '2025-09-24T14:30:00Z',
    'message_count': 8,
    'last_message': 'Спасибо за обращение!',
}

sessions['session-002'] = {
    'session_id': 'session-002',
    'scenario_id': 'help-001',
    'status': 'active',
    'start_time': '2025-09-24T15:00:00Z',
    'message_count': 3,
    'last_message': 'Тестируем API...',
}

class MockAPIHandler(http.server.BaseHTTPRequestHandler):
    def do_OPTIONS(self):
        self.send_response(200)
        self.send_header('Access-Control-Allow-Origin', '*')
        self.send_header('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, OPTIONS')
        self.send_header('Access-Control-Allow-Headers', 'Content-Type, Authorization')
        self.end_headers()
    
    def do_GET(self):
        self.send_response(200)
        self.send_header('Content-type', 'application/json')
        self.send_header('Access-Control-Allow-Origin', '*')
        self.end_headers()
        
        path = urlparse(self.path).path
        
        if path == '/api/v1/chat/status':
            active_sessions = len([s for s in sessions.values() if s['status'] == 'active'])
            response = {
                'service': 'chat-service',
                'status': 'running',
                'role': 'session_manager',
                'active_sessions': active_sessions,
                'total_sessions': len(sessions),
                'timestamp': int(datetime.datetime.now().timestamp() * 1000)
            }
        elif path == '/api/v1/chat/sessions':
            response = {
                'sessions': list(sessions.values()),
                'total': len(sessions),
                'timestamp': int(datetime.datetime.now().timestamp() * 1000)
            }
        elif path == '/api/v1/scenarios':
            response = {
                'scenarios': scenarios,
                'count': len(scenarios),
                'timestamp': int(datetime.datetime.now().timestamp() * 1000)
            }
        elif path == '/api/v1/nlu/status':
            response = {
                'service': 'nlu-service',
                'status': 'running',
                'role': 'natural_language_understanding',
                'version': '1.0.0',
                'timestamp': int(datetime.datetime.now().timestamp() * 1000)
            }
        elif path == '/api/v1/orchestrator/status':
            response = {
                'service': 'orchestrator',
                'status': 'running',
                'role': 'main_coordinator',
                'timestamp': int(datetime.datetime.now().timestamp() * 1000)
            }
        else:
            response = {'error': 'Not found'}
        
        self.wfile.write(json.dumps(response).encode())
    
    def do_POST(self):
        content_length = int(self.headers['Content-Length'])
        post_data = self.rfile.read(content_length)
        
        self.send_response(200)
        self.send_header('Content-type', 'application/json')
        self.send_header('Access-Control-Allow-Origin', '*')
        self.end_headers()
        
        path = urlparse(self.path).path
        
        try:
            data = json.loads(post_data.decode('utf-8')) if post_data else {}
        except:
            data = {}
        
        if path == '/api/v1/chat/sessions':
            session_id = str(uuid.uuid4())
            session = {
                'session_id': session_id,
                'status': 'active',
                'start_time': datetime.datetime.now().isoformat() + 'Z',
                'message_count': 0,
                'last_message': '',
                'user_id': None,
                'scenario_id': None,
            }
            sessions[session_id] = session
            
            response = {
                'session_id': session_id,
                'status': 'created',
                'timestamp': int(datetime.datetime.now().timestamp() * 1000)
            }
        elif path == '/api/v1/chat/messages':
            session_id = data.get('session_id')
            content = data.get('content')
            
            if session_id and session_id in sessions:
                sessions[session_id]['message_count'] += 1
                sessions[session_id]['last_message'] = content
            
            response = {
                'session_id': session_id,
                'message_saved': True,
                'timestamp': int(datetime.datetime.now().timestamp() * 1000)
            }
        elif path == '/api/v1/orchestrator/process':
            session_id = data.get('session_id')
            message = data.get('message', '')
            
            bot_response = 'Спасибо за ваше сообщение!'
            if 'привет' in message.lower():
                bot_response = 'Привет! Я ваш банковский помощник. Що саме вас цікавить?'
            elif 'баланс' in message.lower():
                bot_response = 'Проверяю ваш баланс...'
            
            response = {
                'session_id': session_id,
                'user_message': message,
                'bot_response': bot_response,
                'type': 'announce',
                'context_updated': True,
                'timestamp': int(datetime.datetime.now().timestamp() * 1000)
            }
        elif path == '/api/v1/nlu/analyze':
            text = data.get('text', '')
            intent = 'unknown'
            confidence = 0.5
            
            if 'привет' in text.lower():
                intent = 'greeting'
                confidence = 0.95
            elif 'баланс' in text.lower():
                intent = 'check_balance'
                confidence = 0.89
            
            response = {
                'intent': intent,
                'confidence': confidence,
                'entities': [],
                'suggested_scenario': 'greeting-001' if intent == 'greeting' else 'help-001',
                'timestamp': int(datetime.datetime.now().timestamp() * 1000)
            }
        else:
            response = {'error': 'Not found'}
        
        self.wfile.write(json.dumps(response).encode())

if __name__ == '__main__':
    PORT = 8099
    with socketserver.TCPServer(("", PORT), MockAPIHandler) as httpd:
        print(f'🚀 Simple Mock API Server running on http://localhost:{PORT}')
        print('📋 Available endpoints:')
        print('  - GET  /api/v1/chat/status')
        print('  - POST /api/v1/chat/sessions')
        print('  - GET  /api/v1/chat/sessions')
        print('  - POST /api/v1/chat/messages')
        print('  - GET  /api/v1/scenarios')
        print('  - GET  /api/v1/nlu/status')
        print('  - POST /api/v1/nlu/analyze')
        print('  - POST /api/v1/orchestrator/process')
        print('✅ Server ready!')
        httpd.serve_forever()
