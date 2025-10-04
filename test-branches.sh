#!/bin/bash

echo "🌿 Тестирование системы веток"
echo "=============================="

SCENARIO_ID="context-edit-test"

echo "1. Получаем список веток (должен быть пустой):"
curl -s "http://localhost:8093/api/v1/scenarios/$SCENARIO_ID/branches" | jq '.'

echo -e "\n2. Создаем ветку feature/new-greeting:"
curl -X POST "http://localhost:8093/api/v1/scenarios/$SCENARIO_ID/branches/feature%2Fnew-greeting?author=developer1" \
  -H "Content-Type: application/json" | jq '.'

echo -e "\n3. Получаем список веток:"
curl -s "http://localhost:8093/api/v1/scenarios/$SCENARIO_ID/branches" | jq '.'

echo -e "\n4. Получаем историю изменений:"
curl -s "http://localhost:8093/api/v1/scenarios/$SCENARIO_ID/branches/history" | jq '.'

echo -e "\n5. Получаем сценарий из main ветки:"
curl -s "http://localhost:8093/api/v1/scenarios/$SCENARIO_ID" | jq '.scenario_data.nodes[0].parameters.message'

echo -e "\n6. Получаем сценарий из feature ветки:"
curl -s -H "X-Branch: feature/new-greeting" "http://localhost:8093/api/v1/scenarios/$SCENARIO_ID" | jq '.scenario_data.nodes[0].parameters.message'

echo -e "\n✅ Тест системы веток завершен!"
