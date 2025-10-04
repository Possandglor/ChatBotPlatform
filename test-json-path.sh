#!/bin/bash

echo "🧪 Тестирование глубокого извлечения JSON параметров"
echo "=================================================="

# Цвета для вывода
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}1. Проверяем что mock сервер работает...${NC}"
HEALTH=$(curl -s http://localhost:8181/health)
if [[ $? -eq 0 ]]; then
    echo -e "${GREEN}✅ Mock сервер доступен${NC}"
else
    echo -e "${RED}❌ Mock сервер недоступен. Запустите: node json-path-mock-server.js${NC}"
    exit 1
fi

echo -e "\n${BLUE}2. Проверяем Orchestrator...${NC}"
ORCHESTRATOR_HEALTH=$(curl -s http://localhost:8092/api/v1/health)
if [[ $? -eq 0 ]]; then
    echo -e "${GREEN}✅ Orchestrator доступен${NC}"
else
    echo -e "${RED}❌ Orchestrator недоступен${NC}"
    exit 1
fi

echo -e "\n${BLUE}3. Тестируем извлечение данных из mock API...${NC}"

echo -e "${YELLOW}Простое поле:${NC}"
curl -s http://localhost:8181/api/complex-data | jq -r '.service'

echo -e "${YELLOW}Вложенный объект:${NC}"
curl -s http://localhost:8181/api/complex-data | jq -r '.server.name'

echo -e "${YELLOW}Глубокая вложенность:${NC}"
curl -s http://localhost:8181/api/complex-data | jq -r '.server.stats.memory_usage'

echo -e "${YELLOW}Первый элемент массива:${NC}"
curl -s http://localhost:8181/api/complex-data | jq -r '.endpoints[0]'

echo -e "${YELLOW}Объект в массиве:${NC}"
curl -s http://localhost:8181/api/complex-data | jq -r '.users[0].name'

echo -e "${YELLOW}Глубокий путь в массиве:${NC}"
curl -s http://localhost:8181/api/complex-data | jq -r '.users[0].profile.settings.theme'

echo -e "${YELLOW}Массив в объекте в массиве:${NC}"
curl -s http://localhost:8181/api/complex-data | jq -r '.users[1].permissions[0]'

echo -e "${YELLOW}Очень глубокий путь:${NC}"
curl -s http://localhost:8181/api/complex-data | jq -r '.data.analytics.reports[0].metrics.conversion_rate'

echo -e "\n${BLUE}4. Создаем демонстрационный JSON для тестирования подстановки...${NC}"

# Создаем тестовый JSON с данными
cat > /tmp/test_context.json << 'EOF'
{
  "api_response": {
    "service": "ChatBot Platform",
    "server": {
      "name": "orchestrator-service",
      "stats": {
        "memory_usage": "256MB",
        "cpu_usage": "15%"
      }
    },
    "endpoints": [
      "/api/v1/chat/message",
      "/api/v1/scenarios/execute"
    ],
    "users": [
      {
        "name": "Олександр Петренко",
        "profile": {
          "settings": {
            "theme": "dark",
            "language": "uk"
          }
        },
        "permissions": ["read", "write", "admin"]
      },
      {
        "name": "Марія Іваненко",
        "permissions": ["read", "write"]
      }
    ],
    "data": {
      "analytics": {
        "reports": [
          {
            "metrics": {
              "conversion_rate": "23.5%",
              "avg_response_time": "1.2s"
            }
          }
        ]
      }
    }
  }
}
EOF

echo -e "${GREEN}✅ Тестовые данные созданы в /tmp/test_context.json${NC}"

echo -e "\n${BLUE}5. Примеры JSONPath выражений для новой функциональности:${NC}"
echo -e "${YELLOW}Простые пути:${NC}"
echo "  {context.api_response.service} → ChatBot Platform"
echo "  {api_response.service} → ChatBot Platform (без префикса context)"

echo -e "${YELLOW}Вложенные объекты:${NC}"
echo "  {context.api_response.server.name} → orchestrator-service"
echo "  {context.api_response.server.stats.memory_usage} → 256MB"

echo -e "${YELLOW}Массивы:${NC}"
echo "  {context.api_response.endpoints[0]} → /api/v1/chat/message"
echo "  {context.api_response.endpoints[1]} → /api/v1/scenarios/execute"

echo -e "${YELLOW}Объекты в массивах:${NC}"
echo "  {context.api_response.users[0].name} → Олександр Петренко"
echo "  {context.api_response.users[0].profile.settings.theme} → dark"

echo -e "${YELLOW}Массивы в объектах в массивах:${NC}"
echo "  {context.api_response.users[0].permissions[0]} → read"
echo "  {context.api_response.users[1].permissions[0]} → read"

echo -e "${YELLOW}Очень глубокие пути:${NC}"
echo "  {context.api_response.data.analytics.reports[0].metrics.conversion_rate} → 23.5%"

echo -e "\n${GREEN}🎉 Тестирование завершено!${NC}"
echo -e "${BLUE}Теперь вы можете использовать эти JSONPath выражения в сценариях чат-бота.${NC}"
