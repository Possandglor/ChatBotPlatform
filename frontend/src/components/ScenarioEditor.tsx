import React, { useState, useEffect } from 'react';
import { Card, Button, Input, Select, Form, Space, Divider, message, Modal } from 'antd';
import { PlusOutlined, DeleteOutlined, SaveOutlined, ImportOutlined } from '@ant-design/icons';
import { scenarioService } from '../services/scenarioService';

const { TextArea } = Input;
const { Option } = Select;

interface ScenarioNode {
  id: string;
  type: 'message' | 'input' | 'condition' | 'api_call' | 'transfer' | 'end' | 'scenario_jump' | 'menu';
  content?: string;
  variable?: string;
  condition?: string;
  api_url?: string;
  api_method?: string;
  target_scenario?: string;
  options?: Array<{ text: string; value: string; next_node?: string }>;
  next_node?: string;
  true_node?: string;
  false_node?: string;
}

interface Scenario {
  id?: string;
  name: string;
  description: string;
  trigger_intents: string[];
  nodes: ScenarioNode[];
}

interface ScenarioEditorProps {
  editingScenario?: any;
  onScenarioSaved?: () => void;
}

const ScenarioEditor: React.FC<ScenarioEditorProps> = ({ editingScenario, onScenarioSaved }) => {
  const [currentScenario, setCurrentScenario] = useState<Scenario>({
    name: '',
    description: '',
    trigger_intents: [],
    nodes: []
  });
  const [selectedNodeId, setSelectedNodeId] = useState<string>('');
  const [importModalVisible, setImportModalVisible] = useState(false);
  const [importText, setImportText] = useState('');

  // Загрузка редактируемого сценария
  useEffect(() => {
    if (editingScenario) {
      setCurrentScenario({
        id: editingScenario.id,
        name: editingScenario.name,
        description: editingScenario.description,
        trigger_intents: editingScenario.trigger_intents || [],
        nodes: editingScenario.nodes || []
      });
      message.info(`Загружен сценарий: ${editingScenario.name}`);
    }
  }, [editingScenario]);
  const [importJson, setImportJson] = useState('');

  const addNode = (type: ScenarioNode['type']) => {
    const newNode: ScenarioNode = {
      id: `node_${Date.now()}`,
      type,
      content: type === 'message' ? 'Новое сообщение' : undefined,
      variable: type === 'input' ? 'user_input' : undefined,
      condition: type === 'condition' ? 'user_input == "да"' : undefined,
      api_url: type === 'api_call' ? 'http://localhost:8181/api/balance' : undefined,
      api_method: type === 'api_call' ? 'GET' : undefined,
      target_scenario: type === 'scenario_jump' ? 'main-menu-001' : undefined,
      options: type === 'menu' ? [{ text: 'Опция 1', value: '1' }] : undefined
    };

    setCurrentScenario(prev => ({
      ...prev,
      nodes: [...prev.nodes, newNode]
    }));
  };

  const updateNode = (nodeId: string, updates: Partial<ScenarioNode>) => {
    setCurrentScenario(prev => ({
      ...prev,
      nodes: prev.nodes.map(node => 
        node.id === nodeId ? { ...node, ...updates } : node
      )
    }));
  };

  const deleteNode = (nodeId: string) => {
    setCurrentScenario(prev => ({
      ...prev,
      nodes: prev.nodes.filter(node => node.id !== nodeId)
    }));
  };

  const saveScenario = async () => {
    try {
      if (currentScenario.id) {
        await scenarioService.updateScenario(currentScenario.id, currentScenario);
        message.success('Сценарий обновлен');
      } else {
        await scenarioService.createScenario(currentScenario);
        message.success('Сценарий создан');
      }
      onScenarioSaved?.(); // Вызываем callback для обновления списка
    } catch (error) {
      message.error('Ошибка сохранения сценария');
    }
  };

  const createNewScenario = () => {
    setCurrentScenario({
      name: '',
      description: '',
      trigger_intents: [],
      nodes: []
    });
    setSelectedNodeId('');
    message.info('Создание нового сценария');
  };

  const importScenario = () => {
    try {
      const imported = JSON.parse(importJson);
      // Ensure nodes array exists
      const scenario = {
        ...imported,
        nodes: imported.nodes || [],
        trigger_intents: imported.trigger_intents || []
      };
      setCurrentScenario(scenario);
      setImportModalVisible(false);
      message.success('Сценарий импортирован');
    } catch (error) {
      message.error('Ошибка импорта: неверный JSON');
    }
  };

  const renderNodeEditor = (node: ScenarioNode) => (
    <Card 
      key={node.id}
      size="small"
      title={`${node.type.toUpperCase()} - ${node.id}`}
      extra={
        <Button 
          type="text" 
          danger 
          icon={<DeleteOutlined />}
          onClick={() => deleteNode(node.id)}
        />
      }
      style={{ 
        marginBottom: 16,
        border: selectedNodeId === node.id ? '2px solid #52c41a' : '1px solid #d9d9d9'
      }}
      onClick={() => setSelectedNodeId(node.id)}
    >
      {node.type === 'message' && (
        <TextArea
          placeholder="Текст сообщения"
          value={node.content}
          onChange={(e) => updateNode(node.id, { content: e.target.value })}
          rows={2}
        />
      )}

      {node.type === 'input' && (
        <Space direction="vertical" style={{ width: '100%' }}>
          <Input
            placeholder="Переменная для сохранения"
            value={node.variable}
            onChange={(e) => updateNode(node.id, { variable: e.target.value })}
          />
          <TextArea
            placeholder="Текст запроса"
            value={node.content}
            onChange={(e) => updateNode(node.id, { content: e.target.value })}
            rows={2}
          />
        </Space>
      )}

      {node.type === 'condition' && (
        <Space direction="vertical" style={{ width: '100%' }}>
          <Input
            placeholder="Условие (например: user_input == 'да')"
            value={node.condition}
            onChange={(e) => updateNode(node.id, { condition: e.target.value })}
          />
          <Input
            placeholder="ID узла при true"
            value={node.true_node}
            onChange={(e) => updateNode(node.id, { true_node: e.target.value })}
          />
          <Input
            placeholder="ID узла при false"
            value={node.false_node}
            onChange={(e) => updateNode(node.id, { false_node: e.target.value })}
          />
        </Space>
      )}

      {node.type === 'api_call' && (
        <Space direction="vertical" style={{ width: '100%' }}>
          <Input
            placeholder="URL API"
            value={node.api_url}
            onChange={(e) => updateNode(node.id, { api_url: e.target.value })}
          />
          <Select
            value={node.api_method}
            onChange={(value) => updateNode(node.id, { api_method: value })}
            style={{ width: '100%' }}
          >
            <Option value="GET">GET</Option>
            <Option value="POST">POST</Option>
          </Select>
        </Space>
      )}

      {node.type === 'scenario_jump' && (
        <Input
          placeholder="ID целевого сценария"
          value={node.target_scenario}
          onChange={(e) => updateNode(node.id, { target_scenario: e.target.value })}
        />
      )}

      {node.type === 'menu' && (
        <Space direction="vertical" style={{ width: '100%' }}>
          <TextArea
            placeholder="Текст меню"
            value={node.content}
            onChange={(e) => updateNode(node.id, { content: e.target.value })}
            rows={2}
          />
          {node.options?.map((option, index) => (
            <Space key={index} style={{ width: '100%' }}>
              <Input
                placeholder="Текст опции"
                value={option.text}
                onChange={(e) => {
                  const newOptions = [...(node.options || [])];
                  newOptions[index] = { ...option, text: e.target.value };
                  updateNode(node.id, { options: newOptions });
                }}
              />
              <Input
                placeholder="Значение"
                value={option.value}
                onChange={(e) => {
                  const newOptions = [...(node.options || [])];
                  newOptions[index] = { ...option, value: e.target.value };
                  updateNode(node.id, { options: newOptions });
                }}
              />
            </Space>
          ))}
        </Space>
      )}

      {!['condition', 'end'].includes(node.type) && (
        <Input
          placeholder="ID следующего узла"
          value={node.next_node}
          onChange={(e) => updateNode(node.id, { next_node: e.target.value })}
          style={{ marginTop: 8 }}
        />
      )}
    </Card>
  );

  return (
    <div style={{ padding: 24 }}>
      <Card title="Редактор сценариев">
        <Space direction="vertical" style={{ width: '100%' }}>
          <Space>
            <Button 
              type="primary" 
              icon={<SaveOutlined />}
              onClick={saveScenario}
            >
              {currentScenario.id ? 'Обновить сценарий' : 'Создать сценарий'}
            </Button>
            <Button 
              icon={<PlusOutlined />}
              onClick={createNewScenario}
            >
              Новый сценарий
            </Button>
            <Button 
              icon={<ImportOutlined />}
              onClick={() => setImportModalVisible(true)}
            >
              Импорт JSON
            </Button>
          </Space>

          <Divider />

          <Form layout="vertical">
            <Form.Item label="Название сценария">
              <Input
                value={currentScenario.name}
                onChange={(e) => setCurrentScenario(prev => ({ ...prev, name: e.target.value }))}
                placeholder="Например: Проверка баланса"
              />
            </Form.Item>

            <Form.Item label="Описание">
              <TextArea
                value={currentScenario.description}
                onChange={(e) => setCurrentScenario(prev => ({ ...prev, description: e.target.value }))}
                placeholder="Описание сценария"
                rows={2}
              />
            </Form.Item>

            <Form.Item label="Триггерные интенты (через запятую)">
              <Input
                value={(currentScenario.trigger_intents || []).join(', ')}
                onChange={(e) => setCurrentScenario(prev => ({ 
                  ...prev, 
                  trigger_intents: e.target.value.split(',').map(s => s.trim()).filter(Boolean)
                }))}
                placeholder="check_balance, balance_inquiry"
              />
            </Form.Item>
          </Form>

          <Divider />

          <div>
            <h3>Добавить узел:</h3>
            <Space wrap>
              <Button onClick={() => addNode('message')} type="dashed">
                <PlusOutlined /> Сообщение
              </Button>
              <Button onClick={() => addNode('input')} type="dashed">
                <PlusOutlined /> Ввод
              </Button>
              <Button onClick={() => addNode('condition')} type="dashed">
                <PlusOutlined /> Условие
              </Button>
              <Button onClick={() => addNode('api_call')} type="dashed">
                <PlusOutlined /> API вызов
              </Button>
              <Button onClick={() => addNode('menu')} type="dashed">
                <PlusOutlined /> Меню
              </Button>
              <Button onClick={() => addNode('scenario_jump')} type="dashed">
                <PlusOutlined /> Переход
              </Button>
              <Button onClick={() => addNode('transfer')} type="dashed">
                <PlusOutlined /> Оператор
              </Button>
              <Button onClick={() => addNode('end')} type="dashed">
                <PlusOutlined /> Конец
              </Button>
            </Space>
          </div>

          <Divider />

          <div>
            <h3>Узлы сценария:</h3>
            {(currentScenario.nodes || []).map(renderNodeEditor)}
          </div>
        </Space>
      </Card>

      <Modal
        title="Импорт сценария из JSON"
        open={importModalVisible}
        onOk={importScenario}
        onCancel={() => setImportModalVisible(false)}
        width={800}
      >
        <TextArea
          value={importJson}
          onChange={(e) => setImportJson(e.target.value)}
          placeholder="Вставьте JSON сценария здесь..."
          rows={20}
        />
      </Modal>
    </div>
  );
};

export default ScenarioEditor;
