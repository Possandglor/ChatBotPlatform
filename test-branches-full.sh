#!/bin/bash

echo "🌿 Полный тест системы веток"
echo "============================"

SCENARIO_ID="context-edit-test"

echo "1. Инициализируем main ветку (получаем сценарий):"
curl -s "http://localhost:8093/api/v1/scenarios/$SCENARIO_ID" > /dev/null
echo "✅ Main ветка инициализирована"

echo -e "\n2. Получаем список веток:"
curl -s "http://localhost:8093/api/v1/scenarios/$SCENARIO_ID/branches" | jq '.'

echo -e "\n3. Создаем ветку feature/new-greeting:"
curl -X POST "http://localhost:8093/api/v1/scenarios/$SCENARIO_ID/branches/feature%2Fnew-greeting?author=developer1" \
  -H "Content-Type: application/json" | jq '.'

echo -e "\n4. Создаем ветку fix/bug-123:"
curl -X POST "http://localhost:8093/api/v1/scenarios/$SCENARIO_ID/branches/fix%2Fbug-123?author=developer2" \
  -H "Content-Type: application/json" | jq '.'

echo -e "\n5. Получаем обновленный список веток:"
curl -s "http://localhost:8093/api/v1/scenarios/$SCENARIO_ID/branches" | jq '.'

echo -e "\n6. Получаем сценарий из main ветки:"
curl -s "http://localhost:8093/api/v1/scenarios/$SCENARIO_ID" | jq '.scenario_data.nodes[0].parameters.message // "null"'

echo -e "\n7. Получаем сценарий из feature ветки:"
curl -s -H "X-Branch: feature/new-greeting" "http://localhost:8093/api/v1/scenarios/$SCENARIO_ID" | jq '.scenario_data.nodes[0].parameters.message // "null"'

echo -e "\n8. Сливаем feature ветку с main:"
curl -X POST "http://localhost:8093/api/v1/scenarios/$SCENARIO_ID/branches/feature%2Fnew-greeting/merge?target=main&author=developer1" \
  -H "Content-Type: application/json" | jq '.'

echo -e "\n9. Получаем историю изменений:"
curl -s "http://localhost:8093/api/v1/scenarios/$SCENARIO_ID/branches/history" | jq '.history'

echo -e "\n✅ Полный тест системы веток завершен!"
