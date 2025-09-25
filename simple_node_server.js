const http = require('http');
const url = require('url');

// Тестовые данные
const sessions = [
  {
    session_id: 'session-001',
    user_id: 'user-123',
    scenario_id: 'greeting-001',
    status: 'completed',
    start_time: '2025-09-24T14:30:00Z',
    message_count: 8,
    last_message: 'Спасибо за обращение!',
  },
  {
    session_id: 'session-002',
    scenario_id: 'help-001',
    status: 'active',
    start_time: '2025-09-24T15:00:00Z',
    message_count: 3,
    last_message: 'Тестируем API...',
  }
];

const scenarios = [
  {
    id: 'greeting-001',
    name: 'Приветствие пользователя',
    description: 'Простой сценарий приветствия с запросом имени',
    category: 'greeting',
    tags: ['приветствие', 'знакомство'],
    is_active: true,
    created_at: '2025-09-24T14:00:00Z',
    updated_at: '2025-09-24T14:00:00Z',
  },
  {
    id: 'help-001',
    name: 'Помощь пользователю',
    description: 'Сценарий предоставления помощи',
    category: 'support',
    tags: ['помощь', 'поддержка'],
    is_active: true,
    created_at: '2025-09-24T14:00:00Z',
    updated_at: '2025-09-24T14:00:00Z',
  }
];

const server = http.createServer((req, res) => {
  // CORS headers
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization');
  res.setHeader('Content-Type', 'application/json');

  if (req.method === 'OPTIONS') {
    res.writeHead(200);
    res.end();
    return;
  }

  const parsedUrl = url.parse(req.url, true);
  const path = parsedUrl.pathname;

  let response = {};

  if (req.method === 'GET') {
    if (path.includes('/chat/status')) {
      response = {
        service: 'chat-service',
        status: 'running',
        active_sessions: sessions.filter(s => s.status === 'active').length,
        total_sessions: sessions.length,
        timestamp: Date.now()
      };
    } else if (path.includes('/chat/sessions')) {
      response = {
        sessions: sessions,
        total: sessions.length,
        timestamp: Date.now()
      };
    } else if (path.includes('/scenarios')) {
      response = {
        scenarios: scenarios,
        count: scenarios.length,
        timestamp: Date.now()
      };
    } else if (path.includes('/nlu/status')) {
      response = {
        service: 'nlu-service',
        status: 'running',
        role: 'natural_language_understanding',
        version: '1.0.0',
        timestamp: Date.now()
      };
    } else if (path.includes('/orchestrator/status')) {
      response = {
        service: 'orchestrator',
        status: 'running',
        role: 'main_coordinator',
        timestamp: Date.now()
      };
    } else {
      response = { status: 'ok', path: path };
    }
  } else if (req.method === 'POST') {
    if (path.includes('/chat/sessions')) {
      const newSessionId = 'session-' + Date.now();
      response = {
        session_id: newSessionId,
        status: 'created',
        timestamp: Date.now()
      };
    } else if (path.includes('/orchestrator/process')) {
      response = {
        session_id: 'test-session',
        bot_response: 'Привет! Я ваш банковский помощник. Що саме вас цікавить?',
        type: 'announce',
        context_updated: true,
        timestamp: Date.now()
      };
    } else {
      response = { status: 'ok', method: 'POST' };
    }
  }

  res.writeHead(200);
  res.end(JSON.stringify(response));
});

const PORT = 8099;
server.listen(PORT, () => {
  console.log(`🚀 Simple Node.js Mock API Server running on port ${PORT}`);
  console.log('📋 Available endpoints:');
  console.log('  - GET  /api/v1/chat/status');
  console.log('  - GET  /api/v1/chat/sessions');
  console.log('  - POST /api/v1/chat/sessions');
  console.log('  - GET  /api/v1/scenarios');
  console.log('  - GET  /api/v1/nlu/status');
  console.log('  - GET  /api/v1/orchestrator/status');
  console.log('  - POST /api/v1/orchestrator/process');
  console.log('✅ Server ready with real data!');
});
