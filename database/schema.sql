-- PostgreSQL схема для Chatbot Platform
-- Дата: 24.09.2025

-- Таблица сценариев
CREATE TABLE scenarios (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(500) NOT NULL,
    description TEXT,
    version VARCHAR(50) DEFAULT '1.0',
    language VARCHAR(10) DEFAULT 'uk',
    category VARCHAR(100),
    tags TEXT[], -- массив тегов
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    created_by VARCHAR(255),
    scenario_data JSONB NOT NULL -- JSON структура узлов
);

-- Таблица переходов между сценариями
CREATE TABLE scenario_transitions (
    id SERIAL PRIMARY KEY,
    from_scenario_id VARCHAR(255) REFERENCES scenarios(id),
    to_scenario_id VARCHAR(255) REFERENCES scenarios(id),
    condition_type VARCHAR(50), -- 'intent', 'entity', 'context'
    condition_value VARCHAR(255),
    priority INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Таблица версий сценариев
CREATE TABLE scenario_versions (
    id SERIAL PRIMARY KEY,
    scenario_id VARCHAR(255) REFERENCES scenarios(id),
    version VARCHAR(50),
    scenario_data JSONB,
    created_at TIMESTAMP DEFAULT NOW(),
    created_by VARCHAR(255),
    change_description TEXT
);

-- Таблица сессий (для Redis fallback)
CREATE TABLE chat_sessions (
    session_id VARCHAR(255) PRIMARY KEY,
    user_id VARCHAR(255),
    current_scenario_id VARCHAR(255),
    current_node_id VARCHAR(255),
    context_data JSONB,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    expires_at TIMESTAMP DEFAULT NOW() + INTERVAL '24 hours'
);

-- Индексы для быстрого поиска
CREATE INDEX idx_scenarios_category ON scenarios(category);
CREATE INDEX idx_scenarios_tags ON scenarios USING GIN(tags);
CREATE INDEX idx_scenarios_active ON scenarios(is_active);
CREATE INDEX idx_scenario_data ON scenarios USING GIN(scenario_data);
CREATE INDEX idx_transitions_from ON scenario_transitions(from_scenario_id);
CREATE INDEX idx_transitions_condition ON scenario_transitions(condition_type, condition_value);
CREATE INDEX idx_sessions_user ON chat_sessions(user_id);
CREATE INDEX idx_sessions_expires ON chat_sessions(expires_at);

-- Вставка тестовых данных
INSERT INTO scenarios (id, name, description, category, tags, scenario_data) VALUES 
(
    'main-menu-nlu-001',
    'Главное меню с NLU',
    'Основной сценарий с анализом естественного языка',
    'main',
    ARRAY['nlu', 'главное меню', 'банк'],
    '{
        "start_node": "greeting",
        "nodes": [
            {
                "id": "greeting",
                "type": "announce",
                "parameters": {
                    "message": "Привет! Я ваш банковский помощник. Що саме вас цікавить?"
                },
                "next_nodes": ["wait_for_request"]
            },
            {
                "id": "wait_for_request",
                "type": "ask",
                "parameters": {
                    "question": "Опишите, чем могу помочь:",
                    "inputType": "text"
                },
                "next_nodes": ["nlu_analysis"]
            },
            {
                "id": "nlu_analysis",
                "type": "nlu-request",
                "parameters": {
                    "service": "nlu-service",
                    "endpoint": "/api/v1/nlu/analyze"
                },
                "conditions": {
                    "success": "route_by_intent",
                    "error": "nlu_error"
                }
            },
            {
                "id": "route_by_intent",
                "type": "condition",
                "parameters": {
                    "condition": "context.intent"
                },
                "conditions": {
                    "check_balance": "balance_flow",
                    "block_card": "block_flow",
                    "close_account": "close_flow",
                    "unknown": "unknown_intent",
                    "default": "clarification"
                }
            },
            {
                "id": "balance_flow",
                "type": "announce",
                "parameters": {
                    "message": "💳 Баланс карты ****1234: 15,250.50 грн\\n💰 Доступно: 15,250.50 грн"
                },
                "next_nodes": ["ask_more_help"]
            },
            {
                "id": "block_flow",
                "type": "announce",
                "parameters": {
                    "message": "🔒 Карта успешно заблокирована!\\n📱 SMS с подтверждением отправлено"
                },
                "next_nodes": ["ask_more_help"]
            },
            {
                "id": "close_flow",
                "type": "announce",
                "parameters": {
                    "message": "📋 Заявка на закрытие создана\\n👤 Менеджер свяжется в течение дня"
                },
                "next_nodes": ["ask_more_help"]
            },
            {
                "id": "unknown_intent",
                "type": "announce",
                "parameters": {
                    "message": "Не смог понять ваш запрос. Соединяю с оператором..."
                },
                "next_nodes": ["end_session"]
            },
            {
                "id": "clarification",
                "type": "announce",
                "parameters": {
                    "message": "Уточните, пожалуйста:\\n💳 Баланс карты\\n🔒 Блокировка карты\\n📋 Закрытие счета\\n💸 Перевод денег"
                },
                "next_nodes": ["wait_for_request"]
            },
            {
                "id": "nlu_error",
                "type": "announce",
                "parameters": {
                    "message": "Произошла техническая ошибка. Соединяю с оператором..."
                },
                "next_nodes": ["end_session"]
            },
            {
                "id": "ask_more_help",
                "type": "ask",
                "parameters": {
                    "question": "Могу ли еще чем-то помочь?"
                },
                "next_nodes": ["parse_more_help"]
            },
            {
                "id": "parse_more_help",
                "type": "nlu-request",
                "parameters": {
                    "service": "nlu-service",
                    "endpoint": "/api/v1/nlu/analyze"
                },
                "conditions": {
                    "success": "check_more_help",
                    "error": "goodbye"
                }
            },
            {
                "id": "check_more_help",
                "type": "condition",
                "parameters": {
                    "condition": "context.intent"
                },
                "conditions": {
                    "check_balance": "balance_flow",
                    "block_card": "block_flow",
                    "close_account": "close_flow",
                    "default": "goodbye"
                }
            },
            {
                "id": "goodbye",
                "type": "announce",
                "parameters": {
                    "message": "Спасибо за обращение! До свидания! 👋"
                },
                "next_nodes": ["end_session"]
            },
            {
                "id": "end_session",
                "type": "announce",
                "parameters": {
                    "message": "Сессия завершена."
                },
                "next_nodes": []
            }
        ]
    }'::jsonb
);

-- Переходы между сценариями
INSERT INTO scenario_transitions (from_scenario_id, to_scenario_id, condition_type, condition_value, priority) VALUES
('main-menu-nlu-001', 'balance-check-001', 'intent', 'check_balance', 1),
('main-menu-nlu-001', 'card-blocking-001', 'intent', 'block_card', 1),
('main-menu-nlu-001', 'account-closure-001', 'intent', 'close_account', 1),
('main-menu-nlu-001', 'operator-transfer-001', 'intent', 'unknown', 0);

-- Функция для обновления updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Триггер для автоматического обновления updated_at
CREATE TRIGGER update_scenarios_updated_at BEFORE UPDATE ON scenarios
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_sessions_updated_at BEFORE UPDATE ON chat_sessions
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
