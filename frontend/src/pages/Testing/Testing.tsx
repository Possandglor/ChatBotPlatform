import React, { useState, useRef, useEffect } from 'react';
import { Card, Input, Button, Space, Typography, Tag, Alert } from 'antd';
import { SendOutlined, DeleteOutlined, ReloadOutlined } from '@ant-design/icons';
import { apiService } from '../../services/api';

const { Text, Title } = Typography;

interface Message {
  id: string;
  type: 'user' | 'bot';
  content: string;
  timestamp: Date;
}

const Testing: React.FC = () => {
  const [messages, setMessages] = useState<Message[]>([]);
  const [inputValue, setInputValue] = useState('');
  const [sessionId, setSessionId] = useState<string | null>(null);
  const [context, setContext] = useState<any>({});
  const [loading, setLoading] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  const createSession = async () => {
    try {
      setLoading(true);
      const response = await apiService.createChatSession();
      const newSessionId = (response.data as any).session_id;
      const initialMessage = (response.data as any).initial_message || 'Привет! Я готов к тестированию. Напишите что-нибудь для начала диалога.';
      const nodeType = (response.data as any).node_type;
      
      setSessionId(newSessionId);
      setMessages([]);
      setContext({});
      
      const welcomeMessage: Message = {
        id: Date.now().toString(),
        type: 'bot',
        content: initialMessage,
        timestamp: new Date(),
      };
      setMessages([welcomeMessage]);
      
      // Если это announce - автоматически продолжаем
      if (nodeType === 'announce') {
        setTimeout(() => continueSession(newSessionId), 500);
      }
    } catch (error) {
      const mockSessionId = `test-session-${Date.now()}`;
      setSessionId(mockSessionId);
      setMessages([{
        id: Date.now().toString(),
        type: 'bot',
        content: 'Тестовая сессия создана. Начинаем диалог!',
        timestamp: new Date(),
      }]);
    } finally {
      setLoading(false);
    }
  };

  const continueSession = async (sessionId: string) => {
    try {
      const response = await apiService.continueSession(sessionId);
      const botResponse = (response.data as any).bot_response;
      const nodeType = (response.data as any).node_type;
      
      if (botResponse) {
        const botMessage: Message = {
          id: Date.now().toString(),
          type: 'bot',
          content: botResponse,
          timestamp: new Date(),
        };
        setMessages(prev => [...prev, botMessage]);
        
        // Если это announce - продолжаем автоматически (кроме завершения диалога и ошибок)
        if (nodeType === 'announce' && 
            !botResponse.includes('Диалог завершен') && 
            !botResponse.includes('Ошибка выполнения сценария') &&
            !botResponse.includes('Ошибка конфигурации')) {
          setTimeout(() => continueSession(sessionId), 500);
        } else if (nodeType === 'exit' || botResponse.includes('Диалог завершен')) {
          console.log('Диалог завершен');
        } else if (nodeType === 'transfer') {
          console.log('Передача оператору');
        }
        
        // ИСПРАВЛЕНО: Обновляем контекст из ответа
        if ((response.data as any).context) {
          setContext((response.data as any).context);
        }
      }
    } catch (error) {
      console.error('Ошибка продолжения сессии:', error);
    }
  };

  const sendMessage = async () => {
    if (!inputValue.trim() || !sessionId) return;

    const userMessage: Message = {
      id: Date.now().toString(),
      type: 'user',
      content: inputValue,
      timestamp: new Date(),
    };

    setMessages(prev => [...prev, userMessage]);
    setInputValue('');
    setLoading(true);

    try {
      const response = await apiService.sendMessage(sessionId, inputValue);
      const botResponse = (response.data as any).bot_response;
      const nodeType = (response.data as any).node_type;
      
      const botMessage: Message = {
        id: (Date.now() + 1).toString(),
        type: 'bot',
        content: botResponse || 'Ответ получен',
        timestamp: new Date(),
      };

      setMessages(prev => [...prev, botMessage]);
      
      // Если это announce - автоматически продолжаем (кроме завершения диалога)
      if (nodeType === 'announce' && !botResponse.includes('Диалог завершен')) {
        setTimeout(() => continueSession(sessionId), 500);
      } else if (nodeType === 'exit' || botResponse.includes('Диалог завершен')) {
        console.log('Диалог завершен');
      } else if (nodeType === 'transfer') {
        console.log('Передача оператору');
      }
      
      if ((response.data as any).context) {
        setContext((response.data as any).context);
      }
    } catch (error) {
      const mockBotMessage: Message = {
        id: (Date.now() + 1).toString(),
        type: 'bot',
        content: `Мок ответ на: "${inputValue}". Система в тестовом режиме.`,
        timestamp: new Date(),
      };
      
      setMessages(prev => [...prev, mockBotMessage]);
      
      setContext((prev: any) => ({
        ...prev,
        last_message: inputValue,
        message_count: (prev.message_count || 0) + 1,
        timestamp: new Date().toISOString(),
      }));
    } finally {
      setLoading(false);
    }
  };

  const endSession = () => {
    setSessionId(null);
    setMessages([]);
    setContext({});
  };

  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      sendMessage();
    }
  };

  return (
    <div>
      <div style={{ marginBottom: 24, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Title level={2} style={{ margin: 0 }}>Тестирование чат-бота</Title>
        <Space>
          {sessionId && (
            <Button 
              icon={<DeleteOutlined />} 
              onClick={endSession}
              danger
            >
              Завершить диалог
            </Button>
          )}
          <Button 
            type="primary"
            icon={<ReloadOutlined />} 
            onClick={createSession}
            loading={loading}
          >
            {sessionId ? 'Новая сессия' : 'Начать тестирование'}
          </Button>
        </Space>
      </div>

      {sessionId && (
        <Alert
          message={`Активная сессия: ${sessionId}`}
          type="info"
          style={{ marginBottom: 16 }}
        />
      )}

      <div style={{ display: 'flex', gap: 16, height: 'calc(100vh - 200px)' }}>
        <Card 
          title="Диалог" 
          style={{ flex: 2, display: 'flex', flexDirection: 'column' }}
          styles={{ body: { flex: 1, display: 'flex', flexDirection: 'column', padding: 0 } }}
        >
          <div style={{ 
            flex: 1, 
            padding: 16, 
            overflowY: 'auto',
            backgroundColor: '#fafafa',
            minHeight: 400,
          }}>
            {messages.map((message) => (
              <div
                key={message.id}
                style={{
                  marginBottom: 12,
                  display: 'flex',
                  justifyContent: message.type === 'user' ? 'flex-end' : 'flex-start',
                }}
              >
                <div
                  style={{
                    maxWidth: '70%',
                    padding: '8px 12px',
                    borderRadius: 8,
                    backgroundColor: message.type === 'user' ? '#52c41a' : '#e6f7ff',
                    color: message.type === 'user' ? 'white' : '#000',
                  }}
                >
                  <div>{message.content}</div>
                  <div style={{ 
                    fontSize: 11, 
                    opacity: 0.7, 
                    marginTop: 4,
                    textAlign: 'right',
                  }}>
                    {message.timestamp.toLocaleTimeString()}
                  </div>
                </div>
              </div>
            ))}
            <div ref={messagesEndRef} />
          </div>
          
          <div style={{ padding: 16, borderTop: '1px solid #d9d9d9' }}>
            <Space.Compact style={{ width: '100%' }}>
              <Input
                value={inputValue}
                onChange={(e) => setInputValue(e.target.value)}
                onKeyPress={handleKeyPress}
                placeholder="Введите сообщение..."
                disabled={!sessionId || loading}
              />
              <Button
                type="primary"
                icon={<SendOutlined />}
                onClick={sendMessage}
                disabled={!sessionId || !inputValue.trim() || loading}
                loading={loading}
              >
                Отправить
              </Button>
            </Space.Compact>
          </div>
        </Card>

        <Card 
          title="Контекст сессии" 
          style={{ flex: 1 }}
        >
          {sessionId ? (
            <div>
              <div style={{ marginBottom: 16 }}>
                <Text strong>ID сессии:</Text>
                <br />
                <Text code>{sessionId}</Text>
              </div>
              
              <div style={{ marginBottom: 16 }}>
                <Text strong>Сообщений:</Text> {messages.length}
              </div>
              
              <div style={{ marginBottom: 16 }}>
                <Text strong>Статус:</Text> <Tag color="green">Активна</Tag>
              </div>

              {Object.keys(context).length > 0 && (
                <div>
                  <Text strong>Переменные контекста:</Text>
                  <div style={{ marginTop: 8, fontSize: 12, fontFamily: 'monospace' }}>
                    {Object.entries(context).map(([key, value]) => (
                      <div key={key} style={{ marginBottom: 8 }}>
                        <Tag color="blue">{key}</Tag>
                        <Text code>{JSON.stringify(value)}</Text>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>
          ) : (
            <div style={{ textAlign: 'center', color: '#666' }}>
              <p>Создайте сессию для начала тестирования</p>
            </div>
          )}
        </Card>
      </div>
    </div>
  );
};

export default Testing;
