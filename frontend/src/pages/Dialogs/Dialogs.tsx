import React, { useState, useEffect } from 'react';
import { Table, Card, Tag, Button, Modal, Timeline, Typography, Space, message } from 'antd';
import { EyeOutlined, FileTextOutlined, MessageOutlined, RobotOutlined } from '@ant-design/icons';

const { Title, Text } = Typography;

interface Dialog {
  session_id: string;
  scenario_id: string;
  status: string;
  message_count: number;
  last_message: string;
  start_time: number;
  updated_at: number;
}

interface DialogMessage {
  sender: 'user' | 'bot';
  message: string;
  timestamp: number;
}

interface DialogDetails {
  session_id: string;
  scenario_id: string;
  status: string;
  message_count: number;
  messages: DialogMessage[];
  start_time: number;
  updated_at: number;
}

interface LogEntry {
  id: string;
  timestamp: string;
  message: string;
  action: string;
  user_message: string;
  bot_response: string;
  response_type: string;
  next_node: string;
  level: string;
  service: string;
  class: string;
}

const DialogsPage: React.FC = () => {
  const [dialogs, setDialogs] = useState<Dialog[]>([]);
  const [loading, setLoading] = useState(false);
  const [selectedDialog, setSelectedDialog] = useState<DialogDetails | null>(null);
  const [dialogModalVisible, setDialogModalVisible] = useState(false);
  const [logsModalVisible, setLogsModalVisible] = useState(false);
  const [dialogLogs, setDialogLogs] = useState<LogEntry[]>([]);
  const [logsLoading, setLogsLoading] = useState(false);

  const fetchDialogs = async () => {
    setLoading(true);
    try {
      const response = await fetch('/api/v1/chat/sessions');
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }
      const text = await response.text();
      if (!text) {
        setDialogs([]);
        return;
      }
      const data = JSON.parse(text);
      setDialogs(data.sessions || []);
    } catch (error) {
      console.error('Error fetching dialogs:', error);
      setDialogs([]);
    } finally {
      setLoading(false);
    }
  };

  const fetchDialogDetails = async (sessionId: string) => {
    try {
      // Find dialog in current dialogs list
      const dialog = dialogs.find(d => d.session_id === sessionId);
      if (dialog) {
        let messages: DialogMessage[] = [];
        
        // Get real messages from API
        try {
          const messagesResponse = await fetch(`/api/v1/chat/sessions/${dialog.session_id}/messages`);
          if (messagesResponse.ok) {
            const messagesData = await messagesResponse.json();
            if (messagesData.messages && messagesData.messages.length > 0) {
              messages = messagesData.messages.map((msg: any) => ({
                sender: msg.sender || msg.type,
                message: msg.message || msg.content,
                timestamp: new Date(msg.timestamp).getTime()
              }));
            }
          }
        } catch (error) {
          console.error('Error fetching messages for dialog:', error);
        }
        
        // Convert Dialog to DialogDetails format
        const dialogDetails: DialogDetails = {
          session_id: dialog.session_id,
          scenario_id: dialog.scenario_id || 'Не указан',
          status: dialog.status,
          message_count: dialog.message_count,
          messages: messages,
          start_time: new Date(dialog.start_time).getTime(),
          updated_at: Date.now()
        };
        setSelectedDialog(dialogDetails);
        setDialogModalVisible(true);
      } else {
        message.error('Диалог не найден');
      }
    } catch (error) {
      console.error('Error fetching dialog details:', error);
      message.error('Ошибка загрузки деталей диалога');
    }
  };

  const fetchDialogLogs = async (sessionId: string) => {
    setLogsLoading(true);
    try {
      // Get real logs from messages
      try {
        const messagesResponse = await fetch(`/api/v1/chat/sessions/${sessionId}/messages`);
        if (messagesResponse.ok) {
          const messagesData = await messagesResponse.json();
          if (messagesData.messages && messagesData.messages.length > 0) {
            const realLogs: LogEntry[] = [];
            
            messagesData.messages.forEach((msg: any, index: number) => {
              const isBot = msg.sender === 'bot';
              realLogs.push({
                id: (index + 1).toString(),
                timestamp: msg.timestamp,
                message: `${isBot ? 'Бот' : 'Пользователь'}: "${msg.message || msg.content}"`,
                level: 'INFO',
                service: isBot ? 'chat-service' : 'chat-service',
                class: isBot ? 'ScenarioBasedChatService' : 'SimpleChatController',
                action: isBot ? 'BOT_RESPONSE' : 'USER_MESSAGE',
                user_message: isBot ? '' : (msg.message || msg.content),
                bot_response: isBot ? (msg.message || msg.content) : '',
                response_type: isBot ? 'announce' : 'input',
                next_node: isBot ? 'wait_for_input' : 'processing'
              });
            });
            
            setDialogLogs(realLogs);
            setLogsModalVisible(true);
            return;
          }
        }
      } catch (error) {
        console.error('Error fetching real logs:', error);
      }
      
      // Fallback if no real logs found
      message.error('Логи диалога не найдены');
    } catch (error) {
      console.error('Error fetching dialog logs:', error);
      message.error('Ошибка загрузки логов диалога');
    } finally {
      setLogsLoading(false);
    }
  };

  useEffect(() => {
    fetchDialogs();
    const interval = setInterval(fetchDialogs, 10000);
    return () => clearInterval(interval);
  }, []);

  const columns = [
    {
      title: 'ID Сессии',
      dataIndex: 'session_id',
      key: 'session_id',
      render: (text: string) => <Text code>{text.substring(0, 8)}...</Text>,
    },
    {
      title: 'Сценарий',
      dataIndex: 'scenario_id',
      key: 'scenario_id',
      render: (text: string) => <Tag color="blue">{text}</Tag>,
    },
    {
      title: 'Статус',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => (
        <Tag color={status === 'active' ? 'green' : 'orange'}>{status}</Tag>
      ),
    },
    {
      title: 'Сообщений',
      dataIndex: 'message_count',
      key: 'message_count',
      render: (count: number) => <Text strong>{count}</Text>,
    },
    {
      title: 'Последнее сообщение',
      dataIndex: 'last_message',
      key: 'last_message',
      render: (text: string) => (
        <Text ellipsis style={{ maxWidth: 200 }}>
          {text}
        </Text>
      ),
    },
    {
      title: 'Время начала',
      dataIndex: 'start_time',
      key: 'start_time',
      render: (timestamp: number) => new Date(timestamp).toLocaleString('ru-RU'),
    },
    {
      title: 'Действия',
      key: 'actions',
      render: (_: any, record: Dialog) => (
        <Button
          type="primary"
          icon={<EyeOutlined />}
          onClick={() => fetchDialogDetails(record.session_id)}
        >
          Просмотр
        </Button>
      ),
    },
  ];

  return (
    <div style={{ padding: '24px' }}>
      <Card>
        <Title level={2}>Диалоги</Title>
        <Table
          columns={columns}
          dataSource={dialogs}
          rowKey="session_id"
          loading={loading}
          pagination={{ pageSize: 10 }}
        />
      </Card>

      {/* Модальное окно деталей диалога */}
      <Modal
        title={`Диалог: ${selectedDialog?.session_id?.substring(0, 8)}...`}
        open={dialogModalVisible}
        onCancel={() => setDialogModalVisible(false)}
        width={800}
        footer={[
          <Button
            key="logs"
            icon={<FileTextOutlined />}
            onClick={() => {
              if (selectedDialog) {
                fetchDialogLogs(selectedDialog.session_id);
              }
            }}
            loading={logsLoading}
          >
            Логи диалога
          </Button>,
          <Button key="close" onClick={() => setDialogModalVisible(false)}>
            Закрыть
          </Button>,
        ]}
      >
        {selectedDialog && (
          <div>
            <Space direction="vertical" style={{ width: '100%', marginBottom: 16 }}>
              <Text><strong>Сценарий:</strong> <Tag color="blue">{selectedDialog.scenario_id}</Tag></Text>
              <Text><strong>Статус:</strong> <Tag color={selectedDialog.status === 'active' ? 'green' : 'orange'}>{selectedDialog.status}</Tag></Text>
              <Text><strong>Сообщений:</strong> {selectedDialog.message_count}</Text>
              <Text><strong>Начало:</strong> {new Date(selectedDialog.start_time).toLocaleString('ru-RU')}</Text>
            </Space>

            <Title level={4}>Сообщения:</Title>
            <Timeline
              items={selectedDialog.messages?.map((msg, index) => ({
                key: index,
                dot: msg.sender === 'user' ? <MessageOutlined /> : <RobotOutlined />,
                color: msg.sender === 'user' ? 'blue' : 'green',
                children: (
                  <div>
                    <Text strong>{msg.sender === 'user' ? 'Пользователь' : 'Бот'}:</Text>
                    <br />
                    <Text>{msg.message}</Text>
                    <br />
                    <Text type="secondary" style={{ fontSize: '12px' }}>
                      {new Date(msg.timestamp).toLocaleString('ru-RU')}
                    </Text>
                  </div>
                ),
              })) || []}
            />
          </div>
        )}
      </Modal>

      {/* Модальное окно логов диалога */}
      <Modal
        title={`Логи диалога: ${selectedDialog?.session_id?.substring(0, 8)}...`}
        open={logsModalVisible}
        onCancel={() => setLogsModalVisible(false)}
        width={1000}
        footer={[
          <Button key="close" onClick={() => setLogsModalVisible(false)}>
            Закрыть
          </Button>,
        ]}
      >
        <Timeline
          items={dialogLogs.map((log, index) => ({
            key: index,
            color: log.level === 'ERROR' ? 'red' : 'blue',
            children: (
              <div>
                <Text strong>{log.action}</Text>
                <br />
                <Text>Пользователь: "{log.user_message}"</Text>
                <br />
                <Text>Бот: "{log.bot_response}"</Text>
                <br />
                <Space>
                  <Tag color="purple">{log.response_type}</Tag>
                  <Tag color="orange">{log.next_node}</Tag>
                  <Tag color="gray">{log.service}</Tag>
                </Space>
                <br />
                <Text type="secondary" style={{ fontSize: '12px' }}>
                  {new Date(log.timestamp).toLocaleString('ru-RU')}
                </Text>
              </div>
            ),
          }))}
        />
      </Modal>
    </div>
  );
};

export default DialogsPage;
