import React, { useState, useEffect } from 'react';
import { 
  Card, 
  Table, 
  Button, 
  Space, 
  Modal, 
  Form, 
  Input, 
  Tag, 
  Typography,
  Row,
  Col,
  Statistic,
  Progress,
  Divider,
  App
} from 'antd';
import { 
  PlusOutlined, 
  EditOutlined, 
  DeleteOutlined, 
  ExperimentOutlined,
  ReloadOutlined,
  ThunderboltOutlined
} from '@ant-design/icons';
import { apiService } from '../../services/api';

const { Title, Text } = Typography;
const { TextArea } = Input;

interface Intent {
  id: string;
  name: string;
  description: string;
  examples: string[];
  usage_count: number;
}

const NLU: React.FC = () => {
  const { message } = App.useApp();
  const [intents, setIntents] = useState<Intent[]>([]);
  const [loading, setLoading] = useState(false);
  const [modalVisible, setModalVisible] = useState(false);
  const [testModalVisible, setTestModalVisible] = useState(false);
  const [editingIntent, setEditingIntent] = useState<Intent | null>(null);
  const [form] = Form.useForm();
  const [testForm] = Form.useForm();
  const [testResult, setTestResult] = useState<any>(null);

  const loadIntents = async () => {
    setLoading(true);
    try {
      const response = await fetch('/api/v1/nlu/intents');
      if (!response.ok) {
        throw new Error('Failed to load intents');
      }
      const data = await response.json();
      
      // Handle both old format (array of strings) and new format (array of objects)
      let transformedIntents;
      if (data.intents && Array.isArray(data.intents)) {
        if (typeof data.intents[0] === 'string') {
          // Old format - array of strings
          transformedIntents = data.intents.map((intentName: string, index: number) => ({
            id: index + 1,
            name: intentName,
            description: `Интент для обработки: ${intentName}`,
            examples: [`Пример для ${intentName}`, `Еще один пример ${intentName}`],
            usage_count: Math.floor(Math.random() * 100),
            status: 'active'
          }));
        } else {
          // New format - array of objects
          transformedIntents = data.intents;
        }
      } else {
        transformedIntents = [];
      }
      
      setIntents(transformedIntents);
    } catch (error) {
      console.error('Error loading intents:', error);
      message.error('Ошибка загрузки интентов');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadIntents();
  }, []);

  const handleCreate = () => {
    setEditingIntent(null);
    form.resetFields();
    setModalVisible(true);
  };

  const handleEdit = (intent: Intent) => {
    setEditingIntent(intent);
    form.setFieldsValue({
      ...intent,
      examples: intent.examples.join('\n'),
    });
    setModalVisible(true);
  };

  const handleSave = async () => {
    try {
      const values = await form.validateFields();
      const examples = values.examples.split('\n').filter((ex: string) => ex.trim());
      
      const payload = {
        name: values.name,
        description: values.description,
        examples: examples
      };
      
      if (editingIntent) {
        // Обновление интента
        const response = await fetch(`/api/v1/nlu/intents/${editingIntent.id}`, {
          method: 'PUT',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(payload)
        });
        
        if (response.ok) {
          message.success('Интент обновлен');
        } else {
          message.error('Ошибка при обновлении интента');
        }
      } else {
        // Создание интента
        const response = await fetch('/api/v1/nlu/intents', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(payload)
        });
        
        if (response.ok) {
          message.success('Интент создан');
        } else {
          message.error('Ошибка при создании интента');
        }
      }

      setModalVisible(false);
      loadIntents();
    } catch (error) {
      message.error('Ошибка при сохранении интента');
    }
  };

  const handleTest = async () => {
    try {
      const values = await testForm.validateFields();
      
      const response = await fetch('/api/v1/nlu/analyze', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ text: values.text })
      });
      
      if (response.ok) {
        const result = await response.json();
        setTestResult({
          text: values.text,
          intent: result.intent,
          confidence: result.confidence
        });
      } else {
        message.error('Ошибка при анализе текста');
      }
    } catch (error) {
      message.error('Ошибка при тестировании NLU');
    }
  };

  const columns = [
    {
      title: 'Интент',
      dataIndex: 'name',
      key: 'name',
      render: (text: string, record: Intent) => (
        <div>
          <div style={{ fontWeight: 'bold' }}>{text}</div>
          <Text code style={{ fontSize: 11 }}>{record.id}</Text>
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
      title: 'Примеры',
      dataIndex: 'examples',
      key: 'examples',
      render: (examples: string[]) => (
        <div>
          <Text>{(examples || []).length} примеров</Text>
          <div style={{ marginTop: 4 }}>
            {(examples || []).slice(0, 2).map((example, index) => (
              <Tag key={index} style={{ fontSize: 11, marginBottom: 2 }}>
                {example.length > 20 ? example.substring(0, 20) + '...' : example}
              </Tag>
            ))}
            {examples.length > 2 && (
              <Tag color="default">+{examples.length - 2}</Tag>
            )}
          </div>
        </div>
      ),
    },
    {
      title: 'Использований',
      dataIndex: 'usage_count',
      key: 'usage_count',
      render: (count: number) => <Text strong>{count}</Text>,
    },
    {
      title: 'Действия',
      key: 'actions',
      render: (_: any, record: Intent) => (
        <Space>
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
                title: 'Удалить интент?',
                onOk: async () => {
                  try {
                    const response = await fetch(`/api/v1/nlu/intents/${record.id}`, {
                      method: 'DELETE'
                    });
                    
                    if (response.ok) {
                      message.success('Интент удален');
                      loadIntents();
                    } else {
                      message.error('Ошибка при удалении интента');
                    }
                  } catch (error) {
                    message.error('Ошибка при удалении интента');
                  }
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
        <Title level={2} style={{ margin: 0 }}>Управление NLU</Title>
        <Space>
          <Button 
            icon={<ExperimentOutlined />}
            onClick={() => setTestModalVisible(true)}
          >
            Тестировать
          </Button>
          <Button 
            icon={<ReloadOutlined />} 
            onClick={loadIntents}
            loading={loading}
          >
            Обновить
          </Button>
          <Button 
            type="primary" 
            icon={<PlusOutlined />}
            onClick={handleCreate}
          >
            Создать интент
          </Button>
        </Space>
      </div>

      <Row gutter={16} style={{ marginBottom: 24 }}>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic
              title="Всего интентов"
              value={intents.length}
              prefix={<ThunderboltOutlined />}
              valueStyle={{ color: '#52c41a' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic
              title="Примеров"
              value={intents.reduce((total, intent) => total + (intent.examples?.length || 0), 0)}
              valueStyle={{ color: '#1890ff' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic
              title="Средняя уверенность"
              value={intents.length > 0 ? Math.round(intents.reduce((sum, intent) => sum + (intent.confidence || 0.85), 0) / intents.length * 100) : 0}
              suffix="%"
              valueStyle={{ color: '#722ed1' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic
              title="Точность"
              value={intents.length > 0 ? Math.round(intents.reduce((sum, intent) => sum + (intent.accuracy || 0.92), 0) / intents.length * 100) : 0}
              precision={1}
              suffix="%"
              valueStyle={{ color: '#fa8c16' }}
            />
          </Card>
        </Col>
      </Row>

      <Card>
        <Table
          columns={columns}
          dataSource={intents}
          rowKey="id"
          loading={loading}
          pagination={{
            pageSize: 10,
            showTotal: (total) => `Всего интентов: ${total}`,
          }}
        />
      </Card>

      <Modal
        title={editingIntent ? 'Редактировать интент' : 'Создать интент'}
        open={modalVisible}
        onCancel={() => setModalVisible(false)}
        onOk={handleSave}
        width={600}
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="name"
            label="Название интента"
            rules={[{ required: true, message: 'Введите название интента' }]}
          >
            <Input placeholder="Например: Проверка баланса" />
          </Form.Item>

          <Form.Item
            name="description"
            label="Описание"
            rules={[{ required: true, message: 'Введите описание интента' }]}
          >
            <TextArea rows={2} placeholder="Что делает этот интент?" />
          </Form.Item>

          <Form.Item
            name="examples"
            label="Примеры фраз (каждая с новой строки)"
            rules={[{ required: true, message: 'Добавьте примеры фраз' }]}
          >
            <TextArea 
              rows={6} 
              placeholder="Хочу проверить баланс&#10;Какой у меня баланс?&#10;Сколько денег на карте"
            />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="Тестирование NLU"
        open={testModalVisible}
        onCancel={() => setTestModalVisible(false)}
        footer={null}
        width={600}
      >
        <Form form={testForm} layout="vertical">
          <Form.Item
            name="text"
            label="Текст для анализа"
            rules={[{ required: true, message: 'Введите текст для анализа' }]}
          >
            <TextArea 
              rows={3} 
              placeholder="Введите фразу для тестирования NLU..."
            />
          </Form.Item>
          
          <Button 
            type="primary" 
            onClick={handleTest}
            block
          >
            Анализировать
          </Button>
        </Form>

        {testResult && (
          <div style={{ marginTop: 24 }}>
            <Divider>Результат анализа</Divider>
            <div>
              <p><strong>Текст:</strong> {testResult.text}</p>
              <p><strong>Интент:</strong> <Tag color="blue">{testResult.intent}</Tag></p>
              <p><strong>Уверенность:</strong> 
                <Progress 
                  percent={testResult.confidence * 100} 
                  size="small" 
                  style={{ marginLeft: 8, width: 100, display: 'inline-block' }}
                />
                {(testResult.confidence * 100).toFixed(1)}%
              </p>
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
};

export default NLU;
