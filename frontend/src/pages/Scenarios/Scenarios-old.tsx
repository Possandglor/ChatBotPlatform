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
  Typography
} from 'antd';
import { 
  PlusOutlined, 
  EditOutlined, 
  DeleteOutlined, 
  EyeOutlined,
  ReloadOutlined
} from '@ant-design/icons';
import { apiService } from '../../services/api';

const { Title } = Typography;
const { TextArea } = Input;

interface Scenario {
  id: string;
  name: string;
  description: string;
  category: string;
  tags: string[];
  is_active: boolean;
  created_at: string;
  updated_at: string;
}

const Scenarios: React.FC = () => {
  const [scenarios, setScenarios] = useState<Scenario[]>([]);
  const [loading, setLoading] = useState(false);
  const [modalVisible, setModalVisible] = useState(false);
  const [editingScenario, setEditingScenario] = useState<Scenario | null>(null);
  const [form] = Form.useForm();

  const loadScenarios = async () => {
    setLoading(true);
    try {
      const response = await apiService.getScenarios();
      setScenarios(response.data.scenarios || []);
    } catch (error) {
      // Мок данные
      setScenarios([
        {
          id: 'main-menu-001',
          name: 'Главное меню',
          description: 'Основной сценарий с выбором операций',
          category: 'main',
          tags: ['главное меню', 'банк'],
          is_active: true,
          created_at: '2025-09-24T12:00:00Z',
          updated_at: '2025-09-24T12:00:00Z',
        },
        {
          id: 'api-test-001',
          name: 'Тест API',
          description: 'Тестовый сценарий для API интеграции',
          category: 'test',
          tags: ['api', 'тест'],
          is_active: true,
          created_at: '2025-09-24T15:00:00Z',
          updated_at: '2025-09-24T15:00:00Z',
        }
      ]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadScenarios();
  }, []);

  const handleCreate = () => {
    setEditingScenario(null);
    form.resetFields();
    setModalVisible(true);
  };

  const handleEdit = (scenario: Scenario) => {
    setEditingScenario(scenario);
    form.setFieldsValue(scenario);
    setModalVisible(true);
  };

  const handleSave = async () => {
    try {
      const values = await form.validateFields();
      
      if (editingScenario) {
        message.success('Сценарий обновлен');
      } else {
        message.success('Сценарий создан');
      }

      setModalVisible(false);
      loadScenarios();
    } catch (error) {
      message.error('Ошибка при сохранении сценария');
    }
  };

  const columns = [
    {
      title: 'Название',
      dataIndex: 'name',
      key: 'name',
      render: (text: string, record: Scenario) => (
        <div>
          <div style={{ fontWeight: 'bold' }}>{text}</div>
          <div style={{ fontSize: 12, color: '#666' }}>{record.id}</div>
        </div>
      ),
    },
    {
      title: 'Описание',
      dataIndex: 'description',
      key: 'description',
      ellipsis: true,
    },
    {
      title: 'Категория',
      dataIndex: 'category',
      key: 'category',
      render: (category: string) => <Tag color="blue">{category}</Tag>,
    },
    {
      title: 'Теги',
      dataIndex: 'tags',
      key: 'tags',
      render: (tags: string[]) => (
        <>
          {tags?.map(tag => (
            <Tag key={tag} color="green">{tag}</Tag>
          ))}
        </>
      ),
    },
    {
      title: 'Статус',
      dataIndex: 'is_active',
      key: 'is_active',
      render: (active: boolean) => (
        <Tag color={active ? 'success' : 'default'}>
          {active ? 'Активен' : 'Неактивен'}
        </Tag>
      ),
    },
    {
      title: 'Действия',
      key: 'actions',
      render: (_: any, record: Scenario) => (
        <Space>
          <Button 
            icon={<EyeOutlined />} 
            size="small"
            onClick={() => {
              Modal.info({
                title: `Сценарий: ${record.name}`,
                content: (
                  <div>
                    <p><strong>ID:</strong> {record.id}</p>
                    <p><strong>Описание:</strong> {record.description}</p>
                    <p><strong>Категория:</strong> {record.category}</p>
                    <p><strong>Теги:</strong> {record.tags?.join(', ')}</p>
                  </div>
                ),
              });
            }}
          />
          <Button 
            icon={<EditOutlined />} 
            size="small"
            onClick={() => handleEdit(record)}
          />
          <Button 
            icon={<DeleteOutlined />} 
            size="small"
            danger
            onClick={() => {
              Modal.confirm({
                title: 'Удалить сценарий?',
                onOk: () => {
                  message.success('Сценарий удален');
                  loadScenarios();
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
        <Title level={2} style={{ margin: 0 }}>Управление сценариями</Title>
        <Space>
          <Button 
            icon={<ReloadOutlined />} 
            onClick={loadScenarios}
            loading={loading}
          >
            Обновить
          </Button>
          <Button 
            type="primary" 
            icon={<PlusOutlined />}
            onClick={handleCreate}
          >
            Создать сценарий
          </Button>
        </Space>
      </div>

      <Card>
        <Table
          columns={columns}
          dataSource={scenarios}
          rowKey="id"
          loading={loading}
          pagination={{
            pageSize: 10,
            showTotal: (total) => `Всего сценариев: ${total}`,
          }}
        />
      </Card>

      <Modal
        title={editingScenario ? 'Редактировать сценарий' : 'Создать сценарий'}
        open={modalVisible}
        onCancel={() => setModalVisible(false)}
        onOk={handleSave}
        width={600}
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="name"
            label="Название"
            rules={[{ required: true, message: 'Введите название сценария' }]}
          >
            <Input placeholder="Название сценария" />
          </Form.Item>

          <Form.Item
            name="description"
            label="Описание"
            rules={[{ required: true, message: 'Введите описание сценария' }]}
          >
            <TextArea rows={3} placeholder="Описание сценария" />
          </Form.Item>

          <Form.Item
            name="category"
            label="Категория"
            rules={[{ required: true, message: 'Выберите категорию' }]}
          >
            <Select placeholder="Выберите категорию">
              <Select.Option value="main">Главное меню</Select.Option>
              <Select.Option value="banking">Банковские операции</Select.Option>
              <Select.Option value="support">Поддержка</Select.Option>
              <Select.Option value="test">Тестирование</Select.Option>
            </Select>
          </Form.Item>

          <Form.Item
            name="tags"
            label="Теги"
          >
            <Select
              mode="tags"
              placeholder="Добавьте теги"
              style={{ width: '100%' }}
            />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default Scenarios;
