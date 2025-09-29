import React from 'react';
import { Form, Input, Select, Button, Space, Typography } from 'antd';

const { TextArea } = Input;
const { Title } = Typography;

interface ApiNodeFormProps {
  onSave: (data: any) => void;
  initialData?: any;
}

export const ApiNodeForm: React.FC<ApiNodeFormProps> = ({ onSave, initialData }) => {
  const [form] = Form.useForm();

  const handleSubmit = (values: any) => {
    const apiNode = {
      id: `node_${Date.now()}`,
      type: 'api_call',
      content: 'API запрос',
      parameters: {
        url: values.url,
        method: values.method || 'GET',
        body: values.body || '',
        headers: values.headers ? JSON.parse(values.headers) : {},
        timeout: values.timeout || 30000
      },
      next_nodes: []
    };
    onSave(apiNode);
  };

  return (
    <div style={{ padding: '20px', maxWidth: '600px' }}>
      <Title level={4}>Настройка API узла</Title>
      
      <Form
        form={form}
        layout="vertical"
        onFinish={handleSubmit}
        initialValues={initialData}
      >
        <Form.Item
          label="URL"
          name="url"
          rules={[{ required: true, message: 'Введите URL' }]}
        >
          <Input placeholder="http://localhost:8181/api/info" />
        </Form.Item>

        <Form.Item
          label="Метод"
          name="method"
        >
          <Select defaultValue="GET">
            <Select.Option value="GET">GET</Select.Option>
            <Select.Option value="POST">POST</Select.Option>
            <Select.Option value="PUT">PUT</Select.Option>
            <Select.Option value="DELETE">DELETE</Select.Option>
          </Select>
        </Form.Item>

        <Form.Item
          label="Тело запроса (JSON)"
          name="body"
        >
          <TextArea 
            rows={4} 
            placeholder='{"key": "value", "data": "{context.user_input}"}' 
          />
        </Form.Item>

        <Form.Item
          label="Заголовки (JSON)"
          name="headers"
        >
          <TextArea 
            rows={3} 
            placeholder='{"Authorization": "Bearer token", "Content-Type": "application/json"}' 
          />
        </Form.Item>

        <Form.Item
          label="Таймаут (мс)"
          name="timeout"
        >
          <Input type="number" placeholder="30000" />
        </Form.Item>

        <Form.Item>
          <Space>
            <Button type="primary" htmlType="submit">
              Сохранить
            </Button>
            <Button>
              Отмена
            </Button>
          </Space>
        </Form.Item>
      </Form>

      <div style={{ marginTop: '20px', padding: '10px', backgroundColor: '#f5f5f5' }}>
        <Title level={5}>Подстановка переменных:</Title>
        <ul>
          <li><code>{`{context.user_input}`}</code> - ввод пользователя</li>
          <li><code>{`{context.api_response.field}`}</code> - поле из API ответа</li>
          <li><code>{`{context.api_response.array[0]}`}</code> - элемент массива</li>
        </ul>
      </div>
    </div>
  );
};
