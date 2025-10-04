#!/bin/bash

echo "✏️ Тестирование узла Context-Edit"
echo "================================="

# Цвета для вывода
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}1. Проверяем доступность сервисов...${NC}"

# Проверка Orchestrator
ORCHESTRATOR_HEALTH=$(curl -s http://localhost:8092/api/v1/health)
if [[ $? -eq 0 ]]; then
    echo -e "${GREEN}✅ Orchestrator доступен${NC}"
else
    echo -e "${RED}❌ Orchestrator недоступен${NC}"
    exit 1
fi

# Проверка Scenario Service
SCENARIO_HEALTH=$(curl -s http://localhost:8093/api/v1/scenarios)
if [[ $? -eq 0 ]]; then
    echo -e "${GREEN}✅ Scenario Service доступен${NC}"
else
    echo -e "${RED}❌ Scenario Service недоступен${NC}"
    exit 1
fi

echo -e "\n${BLUE}2. Проверяем что тестовый сценарий загружен...${NC}"
SCENARIO_EXISTS=$(curl -s http://localhost:8093/api/v1/scenarios/context-edit-test)
if [[ $? -eq 0 ]] && [[ "$SCENARIO_EXISTS" != *"not found"* ]]; then
    echo -e "${GREEN}✅ Тестовый сценарий context-edit-test найден${NC}"
else
    echo -e "${RED}❌ Тестовый сценарий не найден. Загрузите его командой:${NC}"
    echo -e "${YELLOW}curl -X POST http://localhost:8093/api/v1/scenarios -H \"Content-Type: application/json\" -d @context-edit-test-scenario.json${NC}"
    exit 1
fi

echo -e "\n${BLUE}3. Демонстрация возможностей узла context-edit...${NC}"

echo -e "\n${YELLOW}Поддерживаемые операции:${NC}"
echo "• SET - установить/изменить значение"
echo "• DELETE/REMOVE - удалить значение"  
echo "• ADD - добавить в массив"
echo "• MERGE - объединить объекты"
echo "• CLEAR - очистить контейнер"

echo -e "\n${YELLOW}Примеры операций:${NC}"

echo -e "${BLUE}SET операция:${NC}"
cat << 'EOF'
{
  "action": "set",
  "path": "user.name", 
  "value": "Олександр Петренко"
}
EOF

echo -e "\n${BLUE}Создание вложенных объектов:${NC}"
cat << 'EOF'
{
  "action": "set",
  "path": "user.profile.theme",
  "value": "dark"
}
EOF

echo -e "\n${BLUE}Добавление в массив:${NC}"
cat << 'EOF'
{
  "action": "add",
  "path": "user.permissions[]",
  "value": "admin"
}
EOF

echo -e "\n${BLUE}Объединение объектов:${NC}"
cat << 'EOF'
{
  "action": "merge",
  "path": "user.profile",
  "value": {
    "avatar": "avatar.jpg",
    "bio": "Описание пользователя"
  }
}
EOF

echo -e "\n${BLUE}Динамические значения с подстановкой:${NC}"
cat << 'EOF'
{
  "action": "set",
  "path": "computed.greeting",
  "value": "Привет, {context.user.name}! Тема: {context.user.profile.theme}"
}
EOF

echo -e "\n${YELLOW}Пример полного узла context-edit:${NC}"
cat << 'EOF'
{
  "id": "setup_user_data",
  "type": "context-edit",
  "parameters": {
    "operations": [
      {
        "action": "set",
        "path": "user.name",
        "value": "Новый пользователь"
      },
      {
        "action": "set", 
        "path": "user.profile.theme",
        "value": "light"
      },
      {
        "action": "add",
        "path": "user.permissions[]",
        "value": "read"
      },
      {
        "action": "merge",
        "path": "user.settings",
        "value": {
          "notifications": true,
          "language": "uk"
        }
      }
    ]
  },
  "next_nodes": ["next_step"]
}
EOF

echo -e "\n${GREEN}🎉 Узел context-edit готов к использованию!${NC}"
echo -e "${BLUE}Для полного тестирования запустите тестовый сценарий через веб-интерфейс или API.${NC}"
