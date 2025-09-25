const express = require('express');
const cors = require('cors');
const { v4: uuidv4 } = require('uuid');

const app = express();
app.use(cors());
app.use(express.json());

// In-memory storage
let sessions = new Map();
let messages = new Map();
let scenarios = [
  {
    id: 'greeting-001',
    name: 'Приветствие пользователя',
    description: 'Простой сценарий приветствия с запросом имени',
    category: 'greeting',
    tags: ['приветствие', 'знакомство'],
    is_active: true,
    created_at: new Date().toISOString(),
    updated_at: new Date().toISOString(),
  },
  {
    id: 'help-001',
    name: 'Помощь пользователю',
    description: 'Сценарий предоставления помощи',
    category: 'support',
    tags: ['помощь', 'поддержка'],
    is_active: true,
    created_at: new Date().toISOString(),
    updated_at: new Date().toISOString(),
  }
];

// Тестовые диалоги
sessions.set('session-001', {
  session_id: 'session-001',
  user_id: 'user-123',
  scenario_id: 'greeting-001',
  status: 'completed',
  start_time: '2025-09-24T14:30:00Z',
  message_count: 8,
  last_message: 'Спасибо за обращение!',
});

sessions.set('session-002', {
  session_id: 'session-002',
  scenario_id: 'help-001',
  status: 'active',
  start_time: '2025-09-24T15:00:00Z',
  message_count: 3,
  last_message: 'Тестируем API...',
});

// Chat Service API
app.get('/api/v1/chat/status', (req, res) => {
  res.json({
    service: 'chat-service',
    status: 'running',
    role: 'session_manager',
    active_sessions: Array.from(sessions.values()).filter(s => s.status === 'active').length,
    total_sessions: sessions.size,
    timestamp: Date.now()
  });
});

app.post('/api/v1/chat/sessions', (req, res) => {
  const sessionId = uuidv4();
  const session = {
    session_id: sessionId,
    status: 'active',
    start_time: new Date().toISOString(),
    message_count: 0,
    last_message: '',
    user_id: null,
    scenario_id: null,
  };
  
  sessions.set(sessionId, session);
  messages.set(sessionId, []);
  
  res.json({
    session_id: sessionId,
    status: 'created',
    timestamp: Date.now()
  });
});

app.get('/api/v1/chat/sessions', (req, res) => {
  const sessionList = Array.from(sessions.values());
  res.json({
    sessions: sessionList,
    total: sessionList.length,
    timestamp: Date.now()
  });
});

app.post('/api/v1/chat/messages', (req, res) => {
  const { session_id, content, message_type = 'user', intent } = req.body;
  
  if (!session_id || !content) {
    return res.status(400).json({ error: 'Missing session_id or content' });
  }
  
  const session = sessions.get(session_id);
  if (!session) {
    return res.status(404).json({ error: 'Session not found' });
  }
  
  // Update session
  session.message_count++;
  session.last_message = content;
  
  // Add message
  const sessionMessages = messages.get(session_id) || [];
  sessionMessages.push({
    id: uuidv4(),
    session_id,
    type: message_type,
    content,
    intent,
    timestamp: new Date().toISOString()
  });
  messages.set(session_id, sessionMessages);
  
  res.json({
    session_id,
    message_saved: true,
    timestamp: Date.now()
  });
});

// Scenarios API
app.get('/api/v1/scenarios', (req, res) => {
  res.json({
    scenarios,
    count: scenarios.length,
    timestamp: Date.now()
  });
});

// NLU API
app.get('/api/v1/nlu/status', (req, res) => {
  res.json({
    service: 'nlu-service',
    status: 'running',
    role: 'natural_language_understanding',
    version: '1.0.0',
    timestamp: Date.now()
  });
});

app.post('/api/v1/nlu/analyze', (req, res) => {
  const { text } = req.body;
  
  // Simple intent detection
  let intent = 'unknown';
  let confidence = 0.5;
  
  if (text.toLowerCase().includes('привет') || text.toLowerCase().includes('здравствуй')) {
    intent = 'greeting';
    confidence = 0.95;
  } else if (text.toLowerCase().includes('баланс')) {
    intent = 'check_balance';
    confidence = 0.89;
  }
  
  res.json({
    intent,
    confidence,
    entities: [],
    suggested_scenario: intent === 'greeting' ? 'greeting-001' : 'help-001',
    timestamp: Date.now()
  });
});

// Orchestrator API
app.get('/api/v1/orchestrator/status', (req, res) => {
  res.json({
    service: 'orchestrator',
    status: 'running',
    role: 'main_coordinator',
    timestamp: Date.now()
  });
});

app.post('/api/v1/orchestrator/process', (req, res) => {
  const { session_id, message } = req.body;
  
  // Simple bot response
  let botResponse = 'Спасибо за ваше сообщение!';
  
  if (message.toLowerCase().includes('привет')) {
    botResponse = 'Привет! Я ваш банковский помощник. Що саме вас цікавить?';
  } else if (message.toLowerCase().includes('баланс')) {
    botResponse = 'Проверяю ваш баланс...';
  }
  
  res.json({
    session_id,
    user_message: message,
    bot_response: botResponse,
    type: 'announce',
    context_updated: true,
    timestamp: Date.now()
  });
});

const PORT = 8099;
app.listen(PORT, () => {
  console.log(`🚀 Mock API Server running on http://localhost:${PORT}`);
  console.log('📋 Available endpoints:');
  console.log('  - GET  /api/v1/chat/status');
  console.log('  - POST /api/v1/chat/sessions');
  console.log('  - GET  /api/v1/chat/sessions');
  console.log('  - POST /api/v1/chat/messages');
  console.log('  - GET  /api/v1/scenarios');
  console.log('  - GET  /api/v1/nlu/status');
  console.log('  - POST /api/v1/nlu/analyze');
  console.log('  - POST /api/v1/orchestrator/process');
});
