#!/usr/bin/env python3
"""
Внешний API Mock для тестирования ChatBot Platform
Порт: 8181 (как в сценариях)
"""

from flask import Flask, request, jsonify
import requests
import json
import time

app = Flask(__name__)

# Конфигурация ChatBot Platform
CHATBOT_API = "http://localhost:8092/api/v1"

@app.route('/api/data', methods=['GET', 'POST'])
def mock_data_api():
    """Имитация внешнего API с данными"""
    if request.method == 'GET':
        return jsonify({
            "endpoints": ["/api/data", "/api/info", "/api/users"],
            "service": "external-api",
            "stats": {
                "memory_usage": "256MB",
                "requests": 127,
                "uptime": "2 hours"
            },
            "version": "2.1.0",
            "timestamp": int(time.time())
        })
    
    # POST запрос
    data = request.get_json() or {}
    return jsonify({
        "message": "Data received successfully",
        "received_data": data,
        "processed": True,
        "id": f"req_{int(time.time())}",
        "status": "ok"
    })

@app.route('/api/info')
def mock_info_api():
    """Информационный API"""
    return jsonify({
        "service_name": "Mock External API",
        "description": "Тестовый сервис для ChatBot Platform",
        "features": ["data_processing", "user_management", "analytics"],
        "health": "healthy",
        "load": 0.3
    })

@app.route('/api/users')
def mock_users_api():
    """API пользователей"""
    return jsonify({
        "users": [
            {"id": 1, "name": "Иван Петров", "role": "admin"},
            {"id": 2, "name": "Мария Сидорова", "role": "user"},
            {"id": 3, "name": "Алексей Козлов", "role": "moderator"}
        ],
        "total": 3,
        "active": 2
    })

@app.route('/chat/start', methods=['POST'])
def start_chat():
    """Запуск диалога с ChatBot Platform"""
    try:
        # Создаем сессию
        session_response = requests.post(f"{CHATBOT_API}/chat/sessions")
        if session_response.status_code != 200:
            return jsonify({"error": "Failed to create chat session"}), 500
        
        session_data = session_response.json()
        session_id = session_data.get('session_id')
        
        print(f"✅ Создана сессия: {session_id}")
        print(f"📝 Начальное сообщение: {session_data.get('initial_message', 'Нет сообщения')}")
        
        return jsonify({
            "status": "chat_started",
            "session_id": session_id,
            "initial_message": session_data.get('initial_message'),
            "instructions": "Используйте POST /chat/message для отправки сообщений"
        })
        
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@app.route('/chat/message', methods=['POST'])
def send_message():
    """Отправка сообщения в диалог"""
    data = request.get_json()
    session_id = data.get('session_id')
    message = data.get('message')
    
    if not session_id or not message:
        return jsonify({"error": "session_id и message обязательны"}), 400
    
    try:
        # Отправляем сообщение
        response = requests.post(f"{CHATBOT_API}/chat/messages", json={
            "session_id": session_id,
            "content": message
        })
        
        if response.status_code != 200:
            return jsonify({"error": "Failed to send message"}), 500
        
        bot_response = response.json()
        
        print(f"👤 Пользователь: {message}")
        print(f"🤖 Бот: {bot_response.get('bot_response', 'Нет ответа')}")
        
        return jsonify({
            "status": "message_sent",
            "user_message": message,
            "bot_response": bot_response.get('bot_response'),
            "session_active": bot_response.get('session_active', True)
        })
        
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@app.route('/chat/demo', methods=['POST'])
def demo_conversation():
    """Демо диалог - полный цикл"""
    try:
        # 1. Создаем сессию
        session_response = requests.post(f"{CHATBOT_API}/chat/sessions")
        session_data = session_response.json()
        session_id = session_data.get('session_id')
        
        print(f"\n🚀 ДЕМО ДИАЛОГ НАЧАТ")
        print(f"📋 Сессия: {session_id}")
        print(f"🤖 Бот: {session_data.get('initial_message', 'Привет!')}")
        
        # Если первый узел - announce, продолжаем автоматически
        if session_data.get('node_type') == 'announce':
            continue_response = requests.post(f"{CHATBOT_API}/chat/continue", json={
                "session_id": session_id
            })
            if continue_response.status_code == 200:
                continue_data = continue_response.json()
                print(f"🤖 Бот (продолжение): {continue_data.get('bot_response', '')}")
        
        # 2. Отправляем тестовые сообщения
        messages = [
            "тест",
            "-",
            "-",
            "-",
            "-"
        ]
        
        responses = []
        for msg in messages:
            time.sleep(1)  # Пауза между сообщениями
            
            response = requests.post(f"{CHATBOT_API}/chat/messages", json={
                "session_id": session_id,
                "content": msg
            })
            
            if response.status_code == 200:
                bot_response = response.json()
                print(f"👤 Пользователь: {msg}")
                print(f"🤖 Бот: {bot_response.get('bot_response', 'Нет ответа')}")
                
                responses.append({
                    "user": msg,
                    "bot": bot_response.get('bot_response'),
                    "timestamp": int(time.time())
                })
            else:
                print(f"❌ Ошибка отправки: {msg} - Статус: {response.status_code}")
                try:
                    error_data = response.json()
                    print(f"❌ Детали ошибки: {error_data}")
                except:
                    print(f"❌ Текст ошибки: {response.text}")
        
        print(f"✅ ДЕМО ЗАВЕРШЕН\n")
        
        return jsonify({
            "status": "demo_completed",
            "session_id": session_id,
            "conversation": responses,
            "total_messages": len(responses)
        })
        
    except Exception as e:
        print(f"❌ Ошибка демо: {e}")
        return jsonify({"error": str(e)}), 500

@app.route('/health')
def health():
    """Health check"""
    return jsonify({
        "status": "UP",
        "service": "external-api-mock",
        "chatbot_available": check_chatbot_health()
    })

def check_chatbot_health():
    """Проверка доступности ChatBot Platform"""
    try:
        response = requests.get(f"{CHATBOT_API}/health", timeout=2)
        return response.status_code == 200
    except:
        return False

if __name__ == '__main__':
    print("🚀 Запуск External API Mock")
    print("📡 Порт: 8181")
    print("🔗 Endpoints:")
    print("   GET  /api/data     - Тестовые данные")
    print("   POST /api/data     - Обработка данных")
    print("   GET  /api/info     - Информация о сервисе")
    print("   GET  /api/users    - Список пользователей")
    print("   POST /chat/start   - Запуск диалога")
    print("   POST /chat/message - Отправка сообщения")
    print("   POST /chat/demo    - Полный демо диалог")
    print("   GET  /health       - Статус сервиса")
    print()
    
    app.run(host='0.0.0.0', port=8282, debug=True)
