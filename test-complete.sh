#!/bin/bash

echo "=== ПОЛНЫЙ ТЕСТ АВТОМАТИЧЕСКОЙ ЗАГРУЗКИ СТАРТОВОГО СЦЕНАРИЯ ==="
echo

# Проверка статуса сервисов
echo "1. Проверка статуса сервисов:"
echo "Scenario Service:"
curl -s http://localhost:8093/api/v1/scenarios/status | jq '.'
echo "Chat Service:"
curl -s http://localhost:8091/api/v1/chat/status | jq '.'
echo

# Проверка стартового сценария
echo "2. Проверка стартового сценария (точка входа):"
curl -s http://localhost:8093/api/v1/scenarios/entry-point | jq '.name, .is_entry_point, .scenario_data.start_node'
echo

# Создание новой сессии
echo "3. Создание новой сессии (автоматическая загрузка сценария):"
SESSION_RESPONSE=$(curl -s -X POST http://localhost:8091/api/v1/chat/sessions)
echo $SESSION_RESPONSE | jq '.'
SESSION_ID=$(echo $SESSION_RESPONSE | jq -r '.session_id')
INITIAL_MESSAGE=$(echo $SESSION_RESPONSE | jq -r '.initial_message')
echo

# Проверка что сообщение из сценария
echo "4. Проверка что начальное сообщение из сценария:"
if [ "$INITIAL_MESSAGE" = "Привет! Как дела?" ]; then
    echo "✅ УСПЕХ: Начальное сообщение загружено из сценария"
else
    echo "❌ ОШИБКА: Ожидалось 'Привет! Как дела?', получено '$INITIAL_MESSAGE'"
fi
echo

# Проверка истории сообщений
echo "5. История сообщений (должно быть начальное сообщение):"
curl -s "http://localhost:8091/api/v1/chat/sessions/$SESSION_ID/messages" | jq '.messages[0].message'
echo

# Отправка ответа пользователя
echo "6. Отправка ответа пользователя:"
curl -s -X POST http://localhost:8091/api/v1/chat/messages \
  -H "Content-Type: application/json" \
  -d "{\"session_id\": \"$SESSION_ID\", \"content\": \"Отлично!\"}" | jq '.'
echo

# Проверка обновленной истории
echo "7. Обновленная история сообщений:"
curl -s "http://localhost:8091/api/v1/chat/sessions/$SESSION_ID/messages" | jq '.count, .messages[] | {sender: (.sender // .type), message: (.message // .content)}'
echo

# Проверка списка всех сценариев
echo "8. Список всех сценариев:"
curl -s http://localhost:8093/api/v1/scenarios | jq '.scenarios[] | {name, is_entry_point, is_active}'
echo

echo "=== ТЕСТ ЗАВЕРШЕН ==="
