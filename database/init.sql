-- ChatBot Platform Database Schema
-- PostgreSQL 15

-- Создание расширений
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

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
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    scenario_data JSONB -- JSON структура сценария
);

-- Таблица сессий чата
CREATE TABLE chat_sessions (
    session_id VARCHAR(255) PRIMARY KEY,
    user_id VARCHAR(255),
    scenario_id VARCHAR(255),
    status VARCHAR(50) DEFAULT 'active', -- active, completed, abandoned
    start_time TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    end_time TIMESTAMP WITH TIME ZONE,
    message_count INTEGER DEFAULT 0,
    last_message TEXT,
    context JSONB DEFAULT '{}', -- контекст сессии
    intents TEXT[], -- обнаруженные интенты
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (scenario_id) REFERENCES scenarios(id) ON DELETE SET NULL
);

-- Таблица сообщений диалогов
CREATE TABLE dialog_messages (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    session_id VARCHAR(255) NOT NULL,
    message_type VARCHAR(20) NOT NULL, -- user, bot, system
    content TEXT NOT NULL,
    intent VARCHAR(100),
    confidence DECIMAL(3,2),
    node_id VARCHAR(255), -- узел сценария
    timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    metadata JSONB DEFAULT '{}',
    
    FOREIGN KEY (session_id) REFERENCES chat_sessions(session_id) ON DELETE CASCADE
);

-- Таблица интентов NLU
CREATE TABLE nlu_intents (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    examples TEXT[] NOT NULL, -- примеры фраз
    confidence_threshold DECIMAL(3,2) DEFAULT 0.7,
    scenario_mapping VARCHAR(255), -- связь с сценарием
    usage_count INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (scenario_mapping) REFERENCES scenarios(id) ON DELETE SET NULL
);

-- Таблица системных логов
CREATE TABLE system_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    level VARCHAR(20) NOT NULL, -- DEBUG, INFO, WARN, ERROR
    service VARCHAR(100) NOT NULL,
    class_name VARCHAR(255),
    message TEXT NOT NULL,
    exception TEXT,
    context JSONB DEFAULT '{}',
    session_id VARCHAR(255), -- опционально
    
    FOREIGN KEY (session_id) REFERENCES chat_sessions(session_id) ON DELETE SET NULL
);

-- Таблица логов диалогов
CREATE TABLE dialog_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    session_id VARCHAR(255) NOT NULL,
    timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    event_type VARCHAR(50) NOT NULL, -- message, scenario_change, nlu_analysis, api_call
    details TEXT,
    context JSONB DEFAULT '{}',
    
    FOREIGN KEY (session_id) REFERENCES chat_sessions(session_id) ON DELETE CASCADE
);

-- Индексы для производительности
CREATE INDEX idx_chat_sessions_user_id ON chat_sessions(user_id);
CREATE INDEX idx_chat_sessions_status ON chat_sessions(status);
CREATE INDEX idx_chat_sessions_start_time ON chat_sessions(start_time);
CREATE INDEX idx_dialog_messages_session_id ON dialog_messages(session_id);
CREATE INDEX idx_dialog_messages_timestamp ON dialog_messages(timestamp);
CREATE INDEX idx_system_logs_timestamp ON system_logs(timestamp);
CREATE INDEX idx_system_logs_level ON system_logs(level);
CREATE INDEX idx_system_logs_service ON system_logs(service);
CREATE INDEX idx_dialog_logs_session_id ON dialog_logs(session_id);
CREATE INDEX idx_dialog_logs_timestamp ON dialog_logs(timestamp);

-- Тестовые данные
INSERT INTO scenarios (id, name, description, category, tags, scenario_data) VALUES
('greeting-001', 'Приветствие пользователя', 'Простой сценарий приветствия с запросом имени', 'greeting', 
 ARRAY['приветствие', 'знакомство'], 
 '{"start_node": "welcome", "nodes": [
   {"id": "welcome", "type": "announce", "parameters": {"message": "Привет! Как дела?"}, "next_nodes": ["ask_name"]},
   {"id": "ask_name", "type": "ask", "parameters": {"question": "Как вас зовут?"}, "next_nodes": ["greet_user"]}
 ]}'::jsonb),

('help-001', 'Помощь пользователю', 'Сценарий предоставления помощи', 'support', 
 ARRAY['помощь', 'поддержка'], 
 '{"start_node": "help_menu", "nodes": [
   {"id": "help_menu", "type": "announce", "parameters": {"message": "Чем могу помочь?"}, "next_nodes": []}
 ]}'::jsonb),

('main-menu-nlu-001', 'Главное меню с NLU', 'Основной сценарий с анализом интентов', 'main', 
 ARRAY['главное меню', 'банк', 'nlu'], 
 '{"start_node": "greeting", "nodes": [
   {"id": "greeting", "type": "announce", "parameters": {"message": "Привет! Я ваш банковский помощник. Що саме вас цікавить?"}, "next_nodes": ["wait_for_request"]},
   {"id": "wait_for_request", "type": "nlu-request", "parameters": {"prompt": "Анализируем ваш запрос..."}, "next_nodes": ["process_intent"]},
   {"id": "process_intent", "type": "condition", "parameters": {"conditions": {"greeting": "greeting_response", "check_balance": "balance_check", "default": "unknown_request"}}, "next_nodes": []},
   {"id": "greeting_response", "type": "announce", "parameters": {"message": "Рад вас приветствовать!"}, "next_nodes": ["wait_for_request"]},
   {"id": "balance_check", "type": "announce", "parameters": {"message": "Проверяю ваш баланс..."}, "next_nodes": ["wait_for_request"]},
   {"id": "unknown_request", "type": "announce", "parameters": {"message": "Извините, не понял ваш запрос. Попробуйте еще раз."}, "next_nodes": ["wait_for_request"]}
 ]}'::jsonb);

INSERT INTO nlu_intents (id, name, description, examples, scenario_mapping, usage_count) VALUES
('greeting', 'Приветствие', 'Пользователь здоровается', 
 ARRAY['Привет', 'Здравствуйте', 'Добрый день', 'Доброе утро', 'Добрый вечер'], 
 'greeting-001', 156),

('check_balance', 'Проверка баланса', 'Пользователь хочет узнать баланс', 
 ARRAY['Хочу проверить баланс', 'Какой у меня баланс?', 'Сколько денег на карте', 'Проверить остаток'], 
 'main-menu-nlu-001', 89),

('block_card', 'Блокировка карты', 'Пользователь хочет заблокировать карту', 
 ARRAY['Заблокировать карту', 'Нужно срочно заблокировать', 'Карта украдена', 'Блокировка карты'], 
 'main-menu-nlu-001', 45),

('unknown', 'Неизвестный запрос', 'Запрос не распознан', 
 ARRAY['Не знаю', 'Что-то другое'], 
 NULL, 12);

-- Тестовые сессии
INSERT INTO chat_sessions (session_id, user_id, scenario_id, status, message_count, last_message, intents) VALUES
('session-001', 'user-123', 'main-menu-nlu-001', 'completed', 8, 'Спасибо за обращение!', ARRAY['greeting', 'check_balance']),
('session-002', NULL, 'greeting-001', 'active', 3, 'Тестируем API...', ARRAY['greeting']),
('session-003', 'user-456', 'main-menu-nlu-001', 'abandoned', 2, 'Привет', ARRAY['greeting']);

-- Тестовые сообщения
INSERT INTO dialog_messages (session_id, message_type, content, intent, confidence, node_id) VALUES
('session-001', 'user', 'Привет', 'greeting', 0.95, NULL),
('session-001', 'bot', 'Привет! Я ваш банковский помощник. Що саме вас цікавить?', NULL, NULL, 'greeting'),
('session-001', 'user', 'Хочу проверить баланс', 'check_balance', 0.89, NULL),
('session-001', 'bot', 'Проверяю ваш баланс...', NULL, NULL, 'balance_check'),

('session-002', 'user', 'Привет', 'greeting', 0.92, NULL),
('session-002', 'bot', 'Привет! Как дела?', NULL, NULL, 'welcome'),

('session-003', 'user', 'Привет', 'greeting', 0.88, NULL),
('session-003', 'bot', 'Привет! Я ваш банковский помощник. Що саме вас цікавить?', NULL, NULL, 'greeting');

-- Тестовые системные логи
INSERT INTO system_logs (level, service, class_name, message, context, session_id) VALUES
('INFO', 'orchestrator', 'OrchestratorController', 'Processing message for session session-001: Привет', 
 '{"session_id": "session-001", "scenario": "main-menu-nlu-001"}'::jsonb, 'session-001'),
('DEBUG', 'nlu-service', 'NluService', 'Analyzing text: Привет', 
 '{"intent": "greeting", "confidence": 0.95}'::jsonb, 'session-001'),
('INFO', 'orchestrator', 'AdvancedScenarioEngine', 'Executing node: greeting (type: announce)', 
 '{"node_id": "greeting", "scenario": "main-menu-nlu-001"}'::jsonb, 'session-001'),
('ERROR', 'api-gateway', 'ProxyController', 'Failed to proxy request to chat-service', 
 '{"target_url": "http://localhost:8091/api/v1/chat/messages"}'::jsonb, NULL),
('WARN', 'chat-service', 'ChatController', 'Session session-002 not found, creating new session', 
 '{"session_id": "session-002"}'::jsonb, 'session-002');

-- Тестовые логи диалогов
INSERT INTO dialog_logs (session_id, event_type, details, context) VALUES
('session-001', 'message', 'User message: Привет', '{"message_type": "user", "content": "Привет"}'::jsonb),
('session-001', 'scenario_change', 'Started scenario: main-menu-nlu-001', '{"scenario_id": "main-menu-nlu-001", "node": "greeting"}'::jsonb),
('session-001', 'message', 'Bot response: Привет! Я ваш банковский помощник.', '{"message_type": "bot", "node": "greeting"}'::jsonb),
('session-001', 'nlu_analysis', 'NLU detected intent: greeting (confidence: 0.95)', '{"intent": "greeting", "confidence": 0.95, "entities": []}'::jsonb),
('session-001', 'api_call', 'API call to balance service', '{"api_url": "/api/balance", "method": "GET", "status": 200}'::jsonb);

-- Функция для обновления updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Триггеры для автоматического обновления updated_at
CREATE TRIGGER update_scenarios_updated_at BEFORE UPDATE ON scenarios FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_chat_sessions_updated_at BEFORE UPDATE ON chat_sessions FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_nlu_intents_updated_at BEFORE UPDATE ON nlu_intents FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
