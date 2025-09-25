#!/usr/bin/env python3
from flask import Flask, request, jsonify
from flask_cors import CORS
import uuid
import datetime
import json

app = Flask(__name__)
CORS(app)

# In-memory storage
sessions = {}
messages = {}
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

# Тестовые диалоги
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

# Chat Service API
@app.route('/api/v1/chat/status', methods=['GET'])
def chat_status():
    active_sessions = len([s for s in sessions.values() if s['status'] == 'active'])
    return jsonify({
        'service': 'chat-service',
        'status': 'running',
        'role': 'session_manager',
        'active_sessions': active_sessions,
        'total_sessions': len(sessions),
        'timestamp': int(datetime.datetime.now().timestamp() * 1000)
    })

@app.route('/api/v1/chat/sessions', methods=['POST'])
def create_session():
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
    messages[session_id] = []
    
    return jsonify({
        'session_id': session_id,
        'status': 'created',
        'timestamp': int(datetime.datetime.now().timestamp() * 1000)
    })

@app.route('/api/v1/chat/sessions', methods=['GET'])
def get_sessions():
    session_list = list(sessions.values())
    return jsonify({
        'sessions': session_list,
        'total': len(session_list),
        'timestamp': int(datetime.datetime.now().timestamp() * 1000)
    })

@app.route('/api/v1/chat/messages', methods=['POST'])
def add_message():
    data = request.get_json()
    session_id = data.get('session_id')
    content = data.get('content')
    message_type = data.get('message_type', 'user')
    intent = data.get('intent')
    
    if not session_id or not content:
        return jsonify({'error': 'Missing session_id or content'}), 400
    
    if session_id not in sessions:
        return jsonify({'error': 'Session not found'}), 404
    
    # Update session
    sessions[session_id]['message_count'] += 1
    sessions[session_id]['last_message'] = content
    
    # Add message
    if session_id not in messages:
        messages[session_id] = []
    
    messages[session_id].append({
        'id': str(uuid.uuid4()),
        'session_id': session_id,
        'type': message_type,
        'content': content,
        'intent': intent,
        'timestamp': datetime.datetime.now().isoformat() + 'Z'
    })
    
    return jsonify({
        'session_id': session_id,
        'message_saved': True,
        'timestamp': int(datetime.datetime.now().timestamp() * 1000)
    })

# Scenarios API
@app.route('/api/v1/scenarios', methods=['GET'])
def get_scenarios():
    return jsonify({
        'scenarios': scenarios,
        'count': len(scenarios),
        'timestamp': int(datetime.datetime.now().timestamp() * 1000)
    })

# NLU API
@app.route('/api/v1/nlu/status', methods=['GET'])
def nlu_status():
    return jsonify({
        'service': 'nlu-service',
        'status': 'running',
        'role': 'natural_language_understanding',
        'version': '1.0.0',
        'timestamp': int(datetime.datetime.now().timestamp() * 1000)
    })

@app.route('/api/v1/nlu/analyze', methods=['POST'])
def analyze_text():
    data = request.get_json()
    text = data.get('text', '')
    
    # Simple intent detection
    intent = 'unknown'
    confidence = 0.5
    
    text_lower = text.lower()
    if 'привет' in text_lower or 'здравствуй' in text_lower:
        intent = 'greeting'
        confidence = 0.95
    elif 'баланс' in text_lower:
        intent = 'check_balance'
        confidence = 0.89
    
    return jsonify({
        'intent': intent,
        'confidence': confidence,
        'entities': [],
        'suggested_scenario': 'greeting-001' if intent == 'greeting' else 'help-001',
        'timestamp': int(datetime.datetime.now().timestamp() * 1000)
    })

# Orchestrator API
@app.route('/api/v1/orchestrator/status', methods=['GET'])
def orchestrator_status():
    return jsonify({
        'service': 'orchestrator',
        'status': 'running',
        'role': 'main_coordinator',
        'timestamp': int(datetime.datetime.now().timestamp() * 1000)
    })

@app.route('/api/v1/orchestrator/process', methods=['POST'])
def process_message():
    data = request.get_json()
    session_id = data.get('session_id')
    message = data.get('message')
    
    # Simple bot response
    bot_response = 'Спасибо за ваше сообщение!'
    
    message_lower = message.lower()
    if 'привет' in message_lower:
        bot_response = 'Привет! Я ваш банковский помощник. Що саме вас цікавить?'
    elif 'баланс' in message_lower:
        bot_response = 'Проверяю ваш баланс...'
    
    return jsonify({
        'session_id': session_id,
        'user_message': message,
        'bot_response': bot_response,
        'type': 'announce',
        'context_updated': True,
        'timestamp': int(datetime.datetime.now().timestamp() * 1000)
    })

if __name__ == '__main__':
    print('🚀 Mock API Server starting on http://localhost:8099')
    print('📋 Available endpoints:')
    print('  - GET  /api/v1/chat/status')
    print('  - POST /api/v1/chat/sessions')
    print('  - GET  /api/v1/chat/sessions')
    print('  - POST /api/v1/chat/messages')
    print('  - GET  /api/v1/scenarios')
    print('  - GET  /api/v1/nlu/status')
    print('  - POST /api/v1/nlu/analyze')
    print('  - POST /api/v1/orchestrator/process')
    
    app.run(host='0.0.0.0', port=8099, debug=False)
