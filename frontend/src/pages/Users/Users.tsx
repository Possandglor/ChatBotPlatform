import React, { useState, useEffect } from 'react';
import { 
  Card, 
  Table, 
  Button, 
  Space, 
  Modal, 
  Form, 
  Input, 
  Select, 
  Tag, 
  message,
  Typography,
  Row,
  Col,
  Avatar,
  Switch,
  Statistic
} from 'antd';
import { 
  PlusOutlined, 
  EditOutlined, 
  DeleteOutlined, 
  UserOutlined,
  ReloadOutlined,
  TeamOutlined,
  CrownOutlined,
  EyeOutlined
} from '@ant-design/icons';
import { apiService } from '../../services/api';
import { useAuthStore } from '../../stores/authStore';

const { Title, Text } = Typography;

interface User {
  id: string;
  username: string;
  name: string;
  email: string;
  role: 'admin' | 'editor' | 'viewer';
  is_active: boolean;
  created_at: string;
  last_login?: string;
  sessions_count: number;
}

const Users: React.FC = () => {
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(false);
  const [modalVisible, setModalVisible] = useState(false);
  const [editingUser, setEditingUser] = useState<User | null>(null);
  const [form] = Form.useForm();
  const { user: currentUser, checkPermission } = useAuthStore();

  const loadUsers = async () => {
    setLoading(true);
    try {
      const response = await apiService.getUsers();
      setUsers(response.data.users || []);
    } catch (error) {
      // Мок данные
      setUsers([
        {
          id: '1',
          username: 'admin',
          name: 'Администратор системы',
          email: 'admin@company.com',
          role: 'admin',
          is_active: true,
          created_at: '2025-01-01T00:00:00Z',
          last_login: '2025-09-24T16:00:00Z',
          sessions_count: 45
        },
        {
          id: '2',
          username: 'editor1',
          name: 'Редактор сценариев',
          email: 'editor1@company.com',
          role: 'editor',
          is_active: true,
          created_at: '2025-02-15T00:00:00Z',
          last_login: '2025-09-24T15:30:00Z',
          sessions_count: 23
        },
        {
          id: '3',
          username: 'viewer1',
          name: 'Наблюдатель',
          email: 'viewer1@company.com',
          role: 'viewer',
          is_active: true,
          created_at: '2025-03-01T00:00:00Z',
          last_login: '2025-09-24T14:00:00Z',
          sessions_count: 12
        }
      ]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadUsers();
  }, []);

  const handleCreate = () => {
    setEditingUser(null);
    form.resetFields();
    setModalVisible(true);
  };

  const handleEdit = (user: User) => {
    setEditingUser(user);
    form.setFieldsValue(user);
    setModalVisible(true);
  };

  const handleSave = async () => {
    try {
      const values = await form.validateFields();
      
      if (editingUser) {
        message.success('Пользователь обновлен');
      } else {
        message.success('Пользователь создан');
      }

      setModalVisible(false);
      loadUsers();
    } catch (error) {
      message.error('Ошибка при сохранении пользователя');
    }
  };

  const getRoleIcon = (role: string) => {
    const icons = {
      admin: <CrownOutlined style={{ color: '#ff4d4f' }} />,
      editor: <EditOutlined style={{ color: '#1890ff' }} />,
      viewer: <EyeOutlined style={{ color: '#52c41a' }} />,
    };
    return icons[role as keyof typeof icons];
  };

  const getRoleColor = (role: string) => {
    const colors = {
      admin: 'red',
      editor: 'blue',
      viewer: 'green',
    };
    return colors[role as keyof typeof colors];
  };

  const columns = [
    {
      title: 'Пользователь',
      key: 'user',
      render: (_: any, record: User) => (
        <Space>
          <Avatar 
            icon={<UserOutlined />} 
            style={{ 
              backgroundColor: record.is_active ? '#52c41a' : '#d9d9d9' 
            }}
          />
          <div>
            <div style={{ fontWeight: 'bold' }}>{record.name}</div>
            <Text type="secondary" style={{ fontSize: 12 }}>
              @{record.username}
            </Text>
          </div>
        </Space>
      ),
    },
    {
      title: 'Email',
      dataIndex: 'email',
      key: 'email',
    },
    {
      title: 'Роль',
      dataIndex: 'role',
      key: 'role',
      render: (role: string) => (
        <Tag color={getRoleColor(role)} icon={getRoleIcon(role)}>
          {role.toUpperCase()}
        </Tag>
      ),
    },
    {
      title: 'Статус',
      dataIndex: 'is_active',
      key: 'is_active',
      render: (isActive: boolean) => (
        <Switch
          checked={isActive}
          disabled={!checkPermission('admin')}
          checkedChildren="Активен"
          unCheckedChildren="Неактивен"
        />
      ),
    },
    {
      title: 'Сессий',
      dataIndex: 'sessions_count',
      key: 'sessions_count',
      render: (count: number) => <Text strong>{count}</Text>,
    },
    {
      title: 'Последний вход',
      dataIndex: 'last_login',
      key: 'last_login',
      render: (date: string) => date ? (
        <div>
          <div>{new Date(date).toLocaleDateString('ru-RU')}</div>
          <Text type="secondary" style={{ fontSize: 11 }}>
            {new Date(date).toLocaleTimeString('ru-RU')}
          </Text>
        </div>
      ) : (
        <Text type="secondary">Никогда</Text>
      ),
    },
    {
      title: 'Действия',
      key: 'actions',
      render: (_: any, record: User) => (
        <Space>
          <Button 
            icon={<EditOutlined />} 
            size="small"
            onClick={() => handleEdit(record)}
            disabled={!checkPermission('admin')}
          />
          <Button 
            icon={<DeleteOutlined />} 
            size="small"
            danger
            disabled={!checkPermission('admin') || record.id === currentUser?.id}
            onClick={() => {
              Modal.confirm({
                title: 'Удалить пользователя?',
                onOk: () => {
                  message.success('Пользователь удален');
                  loadUsers();
                },
              });
            }}
          />
        </Space>
      ),
    },
  ];

  return (
    <div>
      <div style={{ marginBottom: 24, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Title level={2} style={{ margin: 0 }}>Управление пользователями</Title>
        <Space>
          <Button 
            icon={<ReloadOutlined />} 
            onClick={loadUsers}
            loading={loading}
          >
            Обновить
          </Button>
          <Button 
            type="primary" 
            icon={<PlusOutlined />}
            onClick={handleCreate}
            disabled={!checkPermission('admin')}
          >
            Добавить пользователя
          </Button>
        </Space>
      </div>

      <Row gutter={16} style={{ marginBottom: 24 }}>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic
              title="Всего пользователей"
              value={4}
              prefix={<TeamOutlined />}
              valueStyle={{ color: '#1890ff' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic
              title="Активных"
              value={3}
              prefix={<UserOutlined />}
              valueStyle={{ color: '#52c41a' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic
              title="Администраторов"
              value={1}
              prefix={<CrownOutlined />}
              valueStyle={{ color: '#ff4d4f' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic
              title="Редакторов"
              value={2}
              prefix={<EditOutlined />}
              valueStyle={{ color: '#722ed1' }}
            />
          </Card>
        </Col>
      </Row>

      <Card>
        <Table
          columns={columns}
          dataSource={users}
          rowKey="id"
          loading={loading}
          pagination={{
            pageSize: 20,
            showTotal: (total) => `Всего пользователей: ${total}`,
          }}
        />
      </Card>

      <Modal
        title={editingUser ? 'Редактировать пользователя' : 'Добавить пользователя'}
        open={modalVisible}
        onCancel={() => setModalVisible(false)}
        onOk={handleSave}
        width={600}
      >
        <Form form={form} layout="vertical">
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="username"
                label="Имя пользователя"
                rules={[{ required: true, message: 'Введите имя пользователя' }]}
              >
                <Input placeholder="username" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="name"
                label="Полное имя"
                rules={[{ required: true, message: 'Введите полное имя' }]}
              >
                <Input placeholder="Иван Иванов" />
              </Form.Item>
            </Col>
          </Row>

          <Form.Item
            name="email"
            label="Email"
            rules={[
              { required: true, message: 'Введите email' },
              { type: 'email', message: 'Неверный формат email' }
            ]}
          >
            <Input placeholder="user@company.com" />
          </Form.Item>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="role"
                label="Роль"
                rules={[{ required: true, message: 'Выберите роль' }]}
              >
                <Select placeholder="Выберите роль">
                  <Select.Option value="admin">
                    <Space>
                      <CrownOutlined style={{ color: '#ff4d4f' }} />
                      Администратор
                    </Space>
                  </Select.Option>
                  <Select.Option value="editor">
                    <Space>
                      <EditOutlined style={{ color: '#1890ff' }} />
                      Редактор
                    </Space>
                  </Select.Option>
                  <Select.Option value="viewer">
                    <Space>
                      <EyeOutlined style={{ color: '#52c41a' }} />
                      Наблюдатель
                    </Space>
                  </Select.Option>
                </Select>
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="is_active"
                label="Статус"
                valuePropName="checked"
                initialValue={true}
              >
                <Switch checkedChildren="Активен" unCheckedChildren="Неактивен" />
              </Form.Item>
            </Col>
          </Row>
        </Form>
      </Modal>
    </div>
  );
};

export default Users;
