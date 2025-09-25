#!/bin/bash

echo "=== Тестирование автоматической загрузки стартового сценария ==="
echo

# Запуск сервисов в фоне
echo "Запуск Scenario Service..."
java -jar scenario-service/target/scenario-service-1.0.0-SNAPSHOT-runner.jar &
SCENARIO_PID=$!
sleep 5

echo "Запуск Chat Service..."
java -jar chat-service/target/chat-service-1.0.0-SNAPSHOT-runner.jar &
CHAT_PID=$!
sleep 5

echo "Сервисы запущены. Тестирование..."
echo

# Проверка стартового сценария
echo "1. Проверка доступности стартового сценария:"
curl -s http://localhost:8093/api/v1/scenarios/entry-point | jq '.'
echo

# Создание новой сессии
echo "2. Создание новой сессии (должна автоматически загрузить стартовый сценарий):"
SESSION_RESPONSE=$(curl -s -X POST http://localhost:8091/api/v1/chat/sessions)
echo $SESSION_RESPONSE | jq '.'
SESSION_ID=$(echo $SESSION_RESPONSE | jq -r '.session_id')
echo

# Получение истории сообщений (должно содержать начальное сообщение)
echo "3. История сообщений сессии (должно быть начальное сообщение из сценария):"
curl -s "http://localhost:8091/api/v1/chat/sessions/$SESSION_ID/messages" | jq '.'
echo

# Отправка сообщения
echo "4. Отправка сообщения пользователя:"
curl -s -X POST http://localhost:8091/api/v1/chat/messages \
  -H "Content-Type: application/json" \
  -d "{\"session_id\": \"$SESSION_ID\", \"message\": \"Привет\"}" | jq '.'
echo

# Завершение тестирования
echo "Завершение тестирования..."
kill $SCENARIO_PID $CHAT_PID 2>/dev/null
echo "Готово!"
