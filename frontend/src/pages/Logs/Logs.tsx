import React, { useState, useEffect } from 'react';
import { 
  Card, 
  Table, 
  Button, 
  Space, 
  Input, 
  Select, 
  Tag, 
  Typography,
  Row,
  Col,
  Tabs,
  Modal
} from 'antd';
import { 
  SearchOutlined, 
  ReloadOutlined,
  EyeOutlined,
  BugOutlined,
  InfoCircleOutlined,
  WarningOutlined,
  CloseCircleOutlined,
  MessageOutlined
} from '@ant-design/icons';
import { apiService } from '../../services/api';

const { Title, Text } = Typography;
const { TabPane } = Tabs;

interface LogEntry {
  id: string;
  timestamp: string;
  level: 'DEBUG' | 'INFO' | 'WARN' | 'ERROR';
  service: string;
  className: string;
  message: string;
  exception?: string;
  session_id?: string;
}

const Logs: React.FC = () => {
  const [systemLogs, setSystemLogs] = useState<LogEntry[]>([]);
  const [loading, setLoading] = useState(false);
  const [searchText, setSearchText] = useState('');
  const [levelFilter, setLevelFilter] = useState<string>('');
  const [serviceFilter, setServiceFilter] = useState<string>('');
  const [selectedLog, setSelectedLog] = useState<LogEntry | null>(null);
  const [modalVisible, setModalVisible] = useState(false);

  const loadSystemLogs = async () => {
    setLoading(true);
    try {
      const response = await apiService.getLogs();
      setSystemLogs(response.data.logs || []);
    } catch (error) {
      // Мок данные
      setSystemLogs([
        {
          id: '1',
          timestamp: '2025-09-24T16:35:00Z',
          level: 'INFO',
          service: 'orchestrator',
          className: 'OrchestratorController',
          message: 'Processing message for session session-001: Хочу проверить баланс',
          session_id: 'session-001'
        },
        {
          id: '2',
          timestamp: '2025-09-24T16:35:01Z',
          level: 'DEBUG',
          service: 'nlu-service',
          className: 'NluService',
          message: 'Analyzing text: Хочу проверить баланс'
        },
        {
          id: '3',
          timestamp: '2025-09-24T16:34:30Z',
          level: 'ERROR',
          service: 'api-gateway',
          className: 'ProxyController',
          message: 'Failed to proxy request to chat-service',
          exception: 'java.net.ConnectException: Connection refused'
        },
        {
          id: '4',
          timestamp: '2025-09-24T16:34:00Z',
          level: 'WARN',
          service: 'chat-service',
          className: 'ChatController',
          message: 'Session session-002 not found, creating new session'
        }
      ]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadSystemLogs();
  }, [searchText, levelFilter, serviceFilter]);

  const getLevelIcon = (level: string) => {
    const icons = {
      DEBUG: <BugOutlined style={{ color: '#666' }} />,
      INFO: <InfoCircleOutlined style={{ color: '#1890ff' }} />,
      WARN: <WarningOutlined style={{ color: '#fa8c16' }} />,
      ERROR: <CloseCircleOutlined style={{ color: '#ff4d4f' }} />,
    };
    return icons[level as keyof typeof icons];
  };

  const getLevelColor = (level: string) => {
    const colors = {
      DEBUG: 'default',
      INFO: 'blue',
      WARN: 'orange',
      ERROR: 'red',
    };
    return colors[level as keyof typeof colors];
  };

  const handleViewDetails = (log: LogEntry) => {
    setSelectedLog(log);
    setModalVisible(true);
  };

  const systemLogColumns = [
    {
      title: 'Время',
      dataIndex: 'timestamp',
      key: 'timestamp',
      width: 160,
      render: (timestamp: string) => (
        <div>
          <div>{new Date(timestamp).toLocaleDateString('ru-RU')}</div>
          <Text type="secondary" style={{ fontSize: 11 }}>
            {new Date(timestamp).toLocaleTimeString('ru-RU')}
          </Text>
        </div>
      ),
    },
    {
      title: 'Уровень',
      dataIndex: 'level',
      key: 'level',
      width: 80,
      render: (level: string) => (
        <Tag color={getLevelColor(level)} icon={getLevelIcon(level)}>
          {level}
        </Tag>
      ),
    },
    {
      title: 'Сервис',
      dataIndex: 'service',
      key: 'service',
      width: 120,
      render: (service: string) => <Tag color="blue">{service}</Tag>,
    },
    {
      title: 'Класс',
      dataIndex: 'className',
      key: 'className',
      width: 150,
      ellipsis: true,
      render: (className: string) => <Text code>{className}</Text>,
    },
    {
      title: 'Сообщение',
      dataIndex: 'message',
      key: 'message',
      ellipsis: true,
      render: (message: string, record: LogEntry) => (
        <div>
          <div>{message}</div>
          {record.session_id && (
            <Text type="secondary" style={{ fontSize: 11 }}>
              Session: {record.session_id}
            </Text>
          )}
        </div>
      ),
    },
    {
      title: 'Действия',
      key: 'actions',
      width: 80,
      render: (_: any, record: LogEntry) => (
        <Button 
          icon={<EyeOutlined />} 
          size="small"
          onClick={() => handleViewDetails(record)}
        />
      ),
    },
  ];

  return (
    <div>
      <div style={{ marginBottom: 24, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Title level={2} style={{ margin: 0 }}>Логи системы</Title>
        <Button 
          icon={<ReloadOutlined />} 
          onClick={loadSystemLogs}
          loading={loading}
        >
          Обновить
        </Button>
      </div>

      <Tabs defaultActiveKey="system">
        <TabPane 
          tab={
            <span>
              <BugOutlined />
              Системные логи
            </span>
          } 
          key="system"
        >
          <Card style={{ marginBottom: 16 }}>
            <Row gutter={16}>
              <Col xs={24} sm={12} md={6}>
                <Input
                  placeholder="Поиск в логах..."
                  prefix={<SearchOutlined />}
                  value={searchText}
                  onChange={(e) => setSearchText(e.target.value)}
                  allowClear
                />
              </Col>
              <Col xs={24} sm={12} md={4}>
                <Select
                  placeholder="Уровень"
                  value={levelFilter}
                  onChange={setLevelFilter}
                  allowClear
                  style={{ width: '100%' }}
                >
                  <Select.Option value="DEBUG">DEBUG</Select.Option>
                  <Select.Option value="INFO">INFO</Select.Option>
                  <Select.Option value="WARN">WARN</Select.Option>
                  <Select.Option value="ERROR">ERROR</Select.Option>
                </Select>
              </Col>
              <Col xs={24} sm={12} md={6}>
                <Select
                  placeholder="Сервис"
                  value={serviceFilter}
                  onChange={setServiceFilter}
                  allowClear
                  style={{ width: '100%' }}
                >
                  <Select.Option value="api-gateway">API Gateway</Select.Option>
                  <Select.Option value="chat-service">Chat Service</Select.Option>
                  <Select.Option value="orchestrator">Orchestrator</Select.Option>
                  <Select.Option value="nlu-service">NLU Service</Select.Option>
                </Select>
              </Col>
            </Row>
          </Card>

          <Card>
            <Table
              columns={systemLogColumns}
              dataSource={systemLogs}
              rowKey="id"
              loading={loading}
              size="small"
              pagination={{
                pageSize: 50,
                showTotal: (total, range) => 
                  `${range[0]}-${range[1]} из ${total} записей`,
              }}
            />
          </Card>
        </TabPane>

        <TabPane 
          tab={
            <span>
              <MessageOutlined />
              Логи диалогов
            </span>
          } 
          key="dialogs"
        >
          <Card>
            <div style={{ textAlign: 'center', padding: 40, color: '#666' }}>
              <MessageOutlined style={{ fontSize: 48, marginBottom: 16 }} />
              <p>Логи диалогов будут отображаться здесь</p>
            </div>
          </Card>
        </TabPane>
      </Tabs>

      <Modal
        title="Детали лога"
        open={modalVisible}
        onCancel={() => setModalVisible(false)}
        footer={null}
        width={600}
      >
        {selectedLog && (
          <div>
            <Space direction="vertical" style={{ width: '100%' }} size="large">
              <div>
                <Title level={4}>Основная информация</Title>
                <div style={{ background: '#fafafa', padding: 16, borderRadius: 6 }}>
                  <p><strong>Время:</strong> {new Date(selectedLog.timestamp).toLocaleString('ru-RU')}</p>
                  <p><strong>Уровень:</strong> 
                    <Tag color={getLevelColor(selectedLog.level)} style={{ marginLeft: 8 }}>
                      {selectedLog.level}
                    </Tag>
                  </p>
                  <p><strong>Сервис:</strong> <Tag color="blue">{selectedLog.service}</Tag></p>
                  <p><strong>Класс:</strong> <Text code>{selectedLog.className}</Text></p>
                  {selectedLog.session_id && (
                    <p><strong>Сессия:</strong> <Text code>{selectedLog.session_id}</Text></p>
                  )}
                </div>
              </div>

              <div>
                <Title level={4}>Сообщение</Title>
                <div style={{ 
                  background: selectedLog.level === 'ERROR' ? '#fff2f0' : '#fafafa', 
                  padding: 16, 
                  borderRadius: 6,
                  border: selectedLog.level === 'ERROR' ? '1px solid #ffccc7' : 'none'
                }}>
                  {selectedLog.message}
                </div>
              </div>

              {selectedLog.exception && (
                <div>
                  <Title level={4}>Исключение</Title>
                  <div style={{ 
                    background: '#fff2f0', 
                    padding: 16, 
                    borderRadius: 6,
                    border: '1px solid #ffccc7'
                  }}>
                    <pre style={{ margin: 0, fontSize: 12, color: '#a8071a' }}>
                      {selectedLog.exception}
                    </pre>
                  </div>
                </div>
              )}
            </Space>
          </div>
        )}
      </Modal>
    </div>
  );
};

export default Logs;
