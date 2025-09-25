import React, { useState, useCallback, useMemo } from 'react';
import { Card, Button, Space, Typography, Drawer, Form, Input, Select, message } from 'antd';
import { SaveOutlined, PlayCircleOutlined, PlusOutlined } from '@ant-design/icons';
import ReactFlow, {
  Node,
  Edge,
  addEdge,
  Connection,
  useNodesState,
  useEdgesState,
  Controls,
  Background,
  MiniMap,
  NodeTypes,
  Handle,
  Position,
} from 'reactflow';
import 'reactflow/dist/style.css';

const { Title, Text } = Typography;
const { Option } = Select;

// Типы узлов
const NODE_TYPES = [
  { type: 'start', label: 'Старт', color: '#52c41a', outputs: 1 },
  { type: 'announce', label: 'Сообщение', color: '#52c41a', outputs: 1 },
  { type: 'ask', label: 'Вопрос', color: '#1890ff', outputs: 1 },
  { type: 'condition', label: 'Условие', color: '#faad14', outputs: 'multiple' },
  { type: 'api_call', label: 'API вызов', color: '#722ed1', outputs: 2 },
  { type: 'wait', label: 'Ожидание', color: '#eb2f96', outputs: 1 },
  { type: 'notification', label: 'Уведомление', color: '#13c2c2', outputs: 1 },
  { type: 'end', label: 'Конец', color: '#f5222d', outputs: 0 },
];

// Кастомный узел
const CustomNode = ({ data, selected }: any) => {
  const nodeType = NODE_TYPES.find(t => t.type === data.type);
  const hasInputs = data.type !== 'start'; // все узлы кроме start имеют входы
  const hasOutputs = data.type !== 'end'; // все узлы кроме end имеют выходы
  
  return (
    <div
      style={{
        padding: '10px 15px',
        borderRadius: '8px',
        border: selected ? '2px solid #1890ff' : '1px solid #d9d9d9',
        backgroundColor: 'white',
        minWidth: '120px',
        textAlign: 'center',
        boxShadow: selected ? '0 4px 12px rgba(24, 144, 255, 0.3)' : '0 2px 8px rgba(0,0,0,0.1)',
        position: 'relative',
      }}
    >
      {/* Входной Handle */}
      {hasInputs && (
        <Handle
          type="target"
          position={Position.Left}
          style={{
            background: '#555',
            width: '8px',
            height: '8px',
            left: '-4px',
          }}
        />
      )}
      
      {/* Выходной Handle */}
      {hasOutputs && (
        <Handle
          type="source"
          position={Position.Right}
          style={{
            background: nodeType?.color || '#555',
            width: '8px',
            height: '8px',
            right: '-4px',
          }}
        />
      )}
      
      {/* Множественные выходы для condition */}
      {data.type === 'condition' && (
        <>
          {data.parameters?.conditions?.map((condition: any, index: number) => (
            <Handle
              key={condition.key || index}
              type="source"
              position={Position.Right}
              id={condition.key || `condition_${index}`}
              style={{
                background: index === 0 ? '#52c41a' : index === 1 ? '#f5222d' : '#faad14',
                width: '8px',
                height: '8px',
                right: '-4px',
                top: `${30 + index * 20}%`,
              }}
            />
          )) || (
            // Дефолтные Handle'ы если условия не настроены
            <>
              <Handle
                type="source"
                position={Position.Right}
                id="true"
                style={{
                  background: '#52c41a',
                  width: '8px',
                  height: '8px',
                  right: '-4px',
                  top: '30%',
                }}
              />
              <Handle
                type="source"
                position={Position.Right}
                id="false"
                style={{
                  background: '#f5222d',
                  width: '8px',
                  height: '8px',
                  right: '-4px',
                  top: '70%',
                }}
              />
            </>
          )}
        </>
      )}
      
      {/* Два выхода для api_call */}
      {data.type === 'api_call' && (
        <>
          <Handle
            type="source"
            position={Position.Right}
            id="success"
            style={{
              background: '#52c41a',
              width: '8px',
              height: '8px',
              right: '-4px',
              top: '30%',
            }}
          />
          <Handle
            type="source"
            position={Position.Right}
            id="error"
            style={{
              background: '#f5222d',
              width: '8px',
              height: '8px',
              right: '-4px',
              top: '70%',
            }}
          />
        </>
      )}
      
      <div
        style={{
          width: '8px',
          height: '8px',
          borderRadius: '50%',
          backgroundColor: nodeType?.color || '#666',
          margin: '0 auto 8px',
        }}
      />
      <Text strong style={{ fontSize: '12px' }}>
        {nodeType?.label || data.type}
      </Text>
      <br />
      <Text type="secondary" style={{ fontSize: '10px' }}>
        {data.label || `${data.type}_${data.id?.slice(-3)}`}
      </Text>
    </div>
  );
};

const nodeTypes: NodeTypes = {
  custom: CustomNode,
};

const VisualEditor: React.FC = () => {
  const [nodes, setNodes, onNodesChange] = useNodesState([]);
  const [edges, setEdges, onEdgesChange] = useEdgesState([]);
  const [selectedNode, setSelectedNode] = useState<Node | null>(null);
  const [drawerVisible, setDrawerVisible] = useState(false);
  const [nodeCounter, setNodeCounter] = useState(1);

  // Добавление узла на полотно
  const addNode = useCallback((nodeType: string) => {
    const newNode: Node = {
      id: `${nodeType}_${nodeCounter}`,
      type: 'custom',
      position: { x: Math.random() * 400 + 100, y: Math.random() * 300 + 100 },
      data: {
        type: nodeType,
        label: `${nodeType}_${nodeCounter}`,
        parameters: {},
      },
    };
    
    setNodes((nds) => nds.concat(newNode));
    setNodeCounter(prev => prev + 1);
  }, [nodeCounter, setNodes]);

  // Соединение узлов
  const onConnect = useCallback(
    (params: Connection) => setEdges((eds) => addEdge(params, eds)),
    [setEdges]
  );

  // Выбор узла
  const onNodeClick = useCallback((event: React.MouseEvent, node: Node) => {
    setSelectedNode(node);
    setDrawerVisible(true);
  }, []);

  // Сохранение настроек узла
  const saveNodeSettings = useCallback((values: any) => {
    if (!selectedNode) return;
    
    setNodes((nds) =>
      nds.map((node) =>
        node.id === selectedNode.id
          ? {
              ...node,
              data: {
                ...node.data,
                label: values.label || node.data.label,
                parameters: values.parameters || {},
              },
            }
          : node
      )
    );
    
    setDrawerVisible(false);
    message.success('Настройки узла сохранены');
  }, [selectedNode, setNodes]);

  // Экспорт в JSON
  const exportScenario = useCallback(() => {
    if (nodes.length === 0) {
      message.warning('Добавьте узлы для экспорта');
      return;
    }

    const startNode = nodes.find(n => n.data.type === 'start') || nodes[0];
    
    const scenario = {
      start_node: startNode.id,
      nodes: nodes.map(node => {
        const nodeEdges = edges.filter(edge => edge.source === node.id);
        
        // Для condition узла - создаем conditions объект
        if (node.data.type === 'condition') {
          const conditions: any = {};
          nodeEdges.forEach(edge => {
            const conditionKey = edge.sourceHandle || 'default';
            conditions[conditionKey] = edge.target;
          });
          
          return {
            id: node.id,
            type: node.data.type,
            parameters: node.data.parameters || {},
            conditions: conditions,
          };
        }
        
        // Для остальных узлов - обычный next_nodes
        return {
          id: node.id,
          type: node.data.type,
          parameters: node.data.parameters || {},
          next_nodes: nodeEdges.map(edge => edge.target),
        };
      }),
    };
    
    // Создаем blob и скачиваем файл
    const blob = new Blob([JSON.stringify(scenario, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'scenario.json';
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
    
    message.success('Сценарий экспортирован в файл scenario.json');
  }, [nodes, edges]);

  // Тест сценария
  const testScenario = useCallback(async () => {
    if (nodes.length === 0) {
      message.warning('Добавьте узлы для тестирования');
      return;
    }

    try {
      const startNode = nodes.find(n => n.data.type === 'start') || nodes[0];
      
      const scenario = {
        id: 'visual-test-scenario',
        name: 'Визуальный тест сценарий',
        version: '1.0',
        language: 'uk',
        scenario_data: {
          start_node: startNode.id,
          nodes: nodes.map(node => {
            const nodeEdges = edges.filter(edge => edge.source === node.id);
            
            if (node.data.type === 'condition') {
              const conditions: any = {};
              nodeEdges.forEach(edge => {
                const conditionKey = edge.sourceHandle || 'default';
                conditions[conditionKey] = edge.target;
              });
              
              return {
                id: node.id,
                type: node.data.type,
                parameters: node.data.parameters || {},
                conditions: conditions,
              };
            }
            
            return {
              id: node.id,
              type: node.data.type,
              parameters: node.data.parameters || {},
              next_nodes: nodeEdges.map(edge => edge.target),
            };
          }),
        }
      };

      // Отправляем тестовое сообщение
      const response = await fetch('/api/v1/orchestrator/process', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          session_id: 'visual-editor-test',
          message: 'тест',
          scenario_data: scenario.scenario_data
        })
      });

      if (response.ok) {
        const result = await response.json();
        message.success(`Тест успешен! Ответ: "${result.bot_response}"`);
      } else {
        message.error('Ошибка тестирования сценария');
      }
    } catch (error) {
      console.error('Test error:', error);
      message.error('Ошибка при тестировании');
    }
  }, [nodes, edges]);

  return (
    <div style={{ height: '100vh', display: 'flex' }}>
      {/* Левая панель - палитра узлов */}
      <Card
        title="Узлы"
        style={{ width: '250px', height: '100%', overflow: 'auto' }}
        bodyStyle={{ padding: '16px' }}
      >
        <Space direction="vertical" style={{ width: '100%' }}>
          {NODE_TYPES.map((nodeType) => (
            <Button
              key={nodeType.type}
              block
              icon={<PlusOutlined />}
              onClick={() => addNode(nodeType.type)}
              style={{
                borderColor: nodeType.color,
                color: nodeType.color,
              }}
            >
              {nodeType.label}
            </Button>
          ))}
        </Space>
      </Card>

      {/* Центральное полотно */}
      <div style={{ flex: 1, position: 'relative' }}>
        {/* Верхняя панель */}
        <div
          style={{
            position: 'absolute',
            top: '10px',
            left: '10px',
            zIndex: 10,
            display: 'flex',
            gap: '8px',
          }}
        >
          <Button
            type="primary"
            icon={<SaveOutlined />}
            onClick={exportScenario}
          >
            Экспорт
          </Button>
          <Button
            icon={<PlayCircleOutlined />}
            onClick={testScenario}
          >
            Тест
          </Button>
        </div>

        <ReactFlow
          nodes={nodes}
          edges={edges}
          onNodesChange={onNodesChange}
          onEdgesChange={onEdgesChange}
          onConnect={onConnect}
          onNodeClick={onNodeClick}
          nodeTypes={nodeTypes}
          fitView
        >
          <Controls />
          <MiniMap />
          <Background variant="dots" gap={12} size={1} />
        </ReactFlow>
      </div>

      {/* Правая панель - настройки узла */}
      <Drawer
        title={`Настройки: ${selectedNode?.data?.label || 'Узел'}`}
        placement="right"
        width={400}
        open={drawerVisible}
        onClose={() => setDrawerVisible(false)}
      >
        {selectedNode && (
          <Form
            layout="vertical"
            initialValues={{
              label: selectedNode.data.label,
              parameters: selectedNode.data.parameters,
            }}
            onFinish={saveNodeSettings}
          >
            <Form.Item
              label="Название узла"
              name="label"
              rules={[{ required: true, message: 'Введите название узла' }]}
            >
              <Input placeholder="Название узла" />
            </Form.Item>

            {selectedNode.data.type === 'announce' && (
              <Form.Item
                label="Сообщение"
                name={['parameters', 'message']}
                rules={[{ required: true, message: 'Введите сообщение' }]}
              >
                <Input.TextArea rows={3} placeholder="Текст сообщения" />
              </Form.Item>
            )}

            {selectedNode.data.type === 'ask' && (
              <>
                <Form.Item
                  label="Вопрос"
                  name={['parameters', 'question']}
                  rules={[{ required: true, message: 'Введите вопрос' }]}
                >
                  <Input.TextArea rows={3} placeholder="Текст вопроса" />
                </Form.Item>
                <Form.Item
                  label="Тип ввода"
                  name={['parameters', 'inputType']}
                >
                  <Select defaultValue="text">
                    <Option value="text">Текст</Option>
                    <Option value="number">Число</Option>
                    <Option value="email">Email</Option>
                  </Select>
                </Form.Item>
              </>
            )}

            {selectedNode.data.type === 'condition' && (
              <>
                <Form.Item
                  label="Условие"
                  name={['parameters', 'condition']}
                  rules={[{ required: true, message: 'Введите условие' }]}
                >
                  <Input placeholder="Например: user_input == 'да'" />
                </Form.Item>
                
                <Form.List name={['parameters', 'conditions']}>
                  {(fields, { add, remove }) => (
                    <>
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
                        <Text strong>Условия и выходы:</Text>
                        <Button type="dashed" onClick={() => add()} icon={<PlusOutlined />}>
                          Добавить условие
                        </Button>
                      </div>
                      {fields.map(({ key, name, ...restField }) => (
                        <Space key={key} style={{ display: 'flex', marginBottom: 8 }} align="baseline">
                          <Form.Item
                            {...restField}
                            name={[name, 'key']}
                            rules={[{ required: true, message: 'Введите ключ' }]}
                          >
                            <Input placeholder="true/false/custom" />
                          </Form.Item>
                          <Form.Item
                            {...restField}
                            name={[name, 'description']}
                          >
                            <Input placeholder="Описание условия" />
                          </Form.Item>
                          <Button type="link" onClick={() => remove(name)} danger>
                            Удалить
                          </Button>
                        </Space>
                      ))}
                      {fields.length === 0 && (
                        <div style={{ marginBottom: 16 }}>
                          <Button type="dashed" onClick={() => {
                            add({ key: 'true', description: 'Условие выполнено' });
                            add({ key: 'false', description: 'Условие не выполнено' });
                          }}>
                            Добавить стандартные условия (true/false)
                          </Button>
                        </div>
                      )}
                    </>
                  )}
                </Form.List>
              </>
            )}

            {selectedNode.data.type === 'api_call' && (
              <>
                <Form.Item
                  label="URL"
                  name={['parameters', 'url']}
                  rules={[{ required: true, message: 'Введите URL' }]}
                >
                  <Input placeholder="https://api.example.com/endpoint" />
                </Form.Item>
                <Form.Item
                  label="Метод"
                  name={['parameters', 'method']}
                >
                  <Select defaultValue="GET">
                    <Option value="GET">GET</Option>
                    <Option value="POST">POST</Option>
                    <Option value="PUT">PUT</Option>
                  </Select>
                </Form.Item>
              </>
            )}

            <Form.Item>
              <Button type="primary" htmlType="submit" block>
                Сохранить настройки
              </Button>
            </Form.Item>
          </Form>
        )}
      </Drawer>
    </div>
  );
};

export default VisualEditor;
