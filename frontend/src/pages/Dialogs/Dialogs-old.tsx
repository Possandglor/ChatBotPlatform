import React, { useState, useEffect } from 'react';
import { 
  Card, 
  Table, 
  Button, 
  Space, 
  Input, 
  Select, 
  Tag, 
  Modal,
  Typography,
  Row,
  Col
} from 'antd';
import { 
  SearchOutlined, 
  EyeOutlined, 
  ReloadOutlined,
  MessageOutlined,
  UserOutlined
} from '@ant-design/icons';
import { apiService } from '../../services/api';

const { Title, Text } = Typography;

interface Dialog {
  session_id: string;
  user_id?: string;
  scenario_name: string;
  status: 'active' | 'completed' | 'abandoned';
  start_time: string;
  message_count: number;
  last_message: string;
}

const Dialogs: React.FC = () => {
  const [dialogs, setDialogs] = useState<Dialog[]>([]);
  const [loading, setLoading] = useState(false);
  const [searchText, setSearchText] = useState('');
  const [statusFilter, setStatusFilter] = useState<string>('');

  const loadDialogs = async () => {
    setLoading(true);
    try {
      const response = await apiService.getDialogs();
      setDialogs(response.data.dialogs || []);
    } catch (error) {
      // Мок данные
      setDialogs([
        {
          session_id: 'session-001',
          user_id: 'user-123',
          scenario_name: 'Главное меню',
          status: 'completed',
          start_time: '2025-09-24T14:30:00Z',
          message_count: 8,
          last_message: 'Спасибо за обращение!',
        },
        {
          session_id: 'session-002',
          scenario_name: 'Тест API',
          status: 'active',
          start_time: '2025-09-24T15:00:00Z',
          message_count: 3,
          last_message: 'Тестируем API...',
        },
        {
          session_id: 'session-003',
          user_id: 'user-456',
          scenario_name: 'Главное меню',
          status: 'abandoned',
          start_time: '2025-09-24T13:15:00Z',
          message_count: 2,
          last_message: 'Привет',
        }
      ]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadDialogs();
  }, [searchText, statusFilter]);

  const handleViewDialog = (dialog: Dialog) => {
    Modal.info({
      title: `Диалог: ${dialog.session_id}`,
      width: 600,
      content: (
        <div>
          <p><strong>Сценарий:</strong> {dialog.scenario_name}</p>
          <p><strong>Статус:</strong> <Tag color={getStatusColor(dialog.status)}>{getStatusText(dialog.status)}</Tag></p>
          <p><strong>Сообщений:</strong> {dialog.message_count}</p>
          <p><strong>Начало:</strong> {new Date(dialog.start_time).toLocaleString('ru-RU')}</p>
          <p><strong>Последнее сообщение:</strong> {dialog.last_message}</p>
        </div>
      ),
    });
  };

  const getStatusColor = (status: string) => {
    const colors = {
      active: 'processing',
      completed: 'success',
      abandoned: 'warning',
    };
    return colors[status as keyof typeof colors];
  };

  const getStatusText = (status: string) => {
    const texts = {
      active: 'Активен',
      completed: 'Завершен',
      abandoned: 'Прерван',
    };
    return texts[status as keyof typeof texts];
  };

  const columns = [
    {
      title: 'Сессия',
      dataIndex: 'session_id',
      key: 'session_id',
      render: (text: string) => (
        <Text code style={{ fontSize: 11 }}>{text.substring(0, 12)}...</Text>
      ),
    },
    {
      title: 'Пользователь',
      dataIndex: 'user_id',
      key: 'user_id',
      render: (userId: string) => userId ? (
        <Space>
          <UserOutlined />
          <Text>{userId}</Text>
        </Space>
      ) : (
        <Text type="secondary">Анонимный</Text>
      ),
    },
    {
      title: 'Сценарий',
      dataIndex: 'scenario_name',
      key: 'scenario_name',
    },
    {
      title: 'Статус',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => (
        <Tag color={getStatusColor(status)}>{getStatusText(status)}</Tag>
      ),
    },
    {
      title: 'Сообщений',
      dataIndex: 'message_count',
      key: 'message_count',
      render: (count: number) => (
        <Space>
          <MessageOutlined />
          <Text>{count}</Text>
        </Space>
      ),
    },
    {
      title: 'Начало',
      dataIndex: 'start_time',
      key: 'start_time',
      render: (date: string) => (
        <div>
          <div>{new Date(date).toLocaleDateString('ru-RU')}</div>
          <Text type="secondary" style={{ fontSize: 11 }}>
            {new Date(date).toLocaleTimeString('ru-RU')}
          </Text>
        </div>
      ),
    },
    {
      title: 'Действия',
      key: 'actions',
      render: (_: any, record: Dialog) => (
        <Button 
          icon={<EyeOutlined />} 
          size="small"
          onClick={() => handleViewDialog(record)}
        >
          Детали
        </Button>
      ),
    },
  ];

  return (
    <div>
      <div style={{ marginBottom: 24, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Title level={2} style={{ margin: 0 }}>Список диалогов</Title>
        <Button 
          icon={<ReloadOutlined />} 
          onClick={loadDialogs}
          loading={loading}
        >
          Обновить
        </Button>
      </div>

      <Card style={{ marginBottom: 16 }}>
        <Row gutter={16}>
          <Col xs={24} sm={12} md={8}>
            <Input
              placeholder="Поиск по тексту..."
              prefix={<SearchOutlined />}
              value={searchText}
              onChange={(e) => setSearchText(e.target.value)}
              allowClear
            />
          </Col>
          <Col xs={24} sm={12} md={6}>
            <Select
              placeholder="Статус"
              value={statusFilter}
              onChange={setStatusFilter}
              allowClear
              style={{ width: '100%' }}
            >
              <Select.Option value="active">Активные</Select.Option>
              <Select.Option value="completed">Завершенные</Select.Option>
              <Select.Option value="abandoned">Прерванные</Select.Option>
            </Select>
          </Col>
        </Row>
      </Card>

      <Card>
        <Table
          columns={columns}
          dataSource={dialogs}
          rowKey="session_id"
          loading={loading}
          pagination={{
            pageSize: 20,
            showTotal: (total, range) => 
              `${range[0]}-${range[1]} из ${total} диалогов`,
          }}
        />
      </Card>
    </div>
  );
};

export default Dialogs;
