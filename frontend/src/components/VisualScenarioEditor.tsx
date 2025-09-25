import React, { useCallback, useState, useEffect } from 'react';
import ReactFlow, {
  Node,
  Edge,
  addEdge,
  Connection,
  useNodesState,
  useEdgesState,
  Controls,
  Background,
  NodeTypes,
  Handle,
  Position,
  EdgeRemoveChange,
} from 'reactflow';
import 'reactflow/dist/style.css';
import { Card, Button, Space, Select, Input, message, Tag, Drawer, Form, Modal, List } from 'antd';
import { PlusOutlined, SaveOutlined, SettingOutlined, ExportOutlined, ImportOutlined, FolderOpenOutlined, DeleteOutlined } from '@ant-design/icons';
import { scenarioService } from '../services/scenarioService';

const { Option } = Select;
const { TextArea } = Input;

// Custom node component with proper handles
const CustomNode = ({ data, id, selected }: { data: any; id: string; selected: boolean }) => {
  const getNodeColor = (type: string) => {
    const colors = {
      message: '#52c41a',
      input: '#1890ff', 
      condition: '#faad14',
      api_call: '#722ed1',
      menu: '#13c2c2',
      scenario_jump: '#eb2f96',
      transfer: '#f5222d',
      end: '#8c8c8c'
    };
    return colors[type as keyof typeof colors] || '#d9d9d9';
  };

  const renderOutputHandles = () => {
    if (data.type === 'condition') {
      const conditions = data.conditions || ['true', 'false'];
      return conditions.map((condition: string, index: number) => (
        <Handle
          key={`${id}-${condition}`}
          type="source"
          position={Position.Right}
          id={condition}
          style={{ 
            top: `${30 + (index * 25)}px`,
            background: getNodeColor(data.type)
          }}
        />
      ));
    } else if (data.type === 'menu') {
      const options = data.options || ['option1', 'option2'];
      return options.map((option: string, index: number) => (
        <Handle
          key={`${id}-${option}`}
          type="source"
          position={Position.Right}
          id={option}
          style={{ 
            top: `${30 + (index * 25)}px`,
            background: getNodeColor(data.type)
          }}
        />
      ));
    } else if (data.type !== 'end') {
      return (
        <Handle
          type="source"
          position={Position.Right}
          style={{ background: getNodeColor(data.type) }}
        />
      );
    }
    return null;
  };

  return (
    <div style={{ position: 'relative' }}>
      {/* Input handle (left side) */}
      {data.type !== 'message' && (
        <Handle
          type="target"
          position={Position.Left}
          style={{ background: getNodeColor(data.type) }}
        />
      )}
      
      <Card 
        size="small"
        style={{ 
          minWidth: 180,
          minHeight: data.type === 'condition' || data.type === 'menu' ? 120 : 80,
          border: selected ? `3px solid #1890ff` : `2px solid ${getNodeColor(data.type)}`,
          borderRadius: 8,
          boxShadow: selected ? '0 0 10px rgba(24, 144, 255, 0.3)' : 'none'
        }}
        title={
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <Tag color={getNodeColor(data.type)} style={{ margin: 0 }}>
              {data.type.toUpperCase()}
            </Tag>
            {selected && <SettingOutlined style={{ color: '#1890ff' }} />}
          </div>
        }
      >
        <div style={{ fontSize: 12, marginBottom: 8 }}>
          {data.content || data.message || 'Настройте узел'}
        </div>
        
        {/* Show conditions/options */}
        {data.type === 'condition' && (
          <div style={{ fontSize: 10 }}>
            {(data.conditions || ['true', 'false']).map((condition: string, index: number) => (
              <div key={condition} style={{ marginBottom: 2 }}>
                → {condition}
              </div>
            ))}
          </div>
        )}
        
        {data.type === 'menu' && (
          <div style={{ fontSize: 10 }}>
            {(data.options || ['option1', 'option2']).map((option: string, index: number) => (
              <div key={option} style={{ marginBottom: 2 }}>
                → {option}
              </div>
            ))}
          </div>
        )}
      </Card>
      
      {/* Output handles (right side) */}
      {renderOutputHandles()}
    </div>
  );
};

const nodeTypes: NodeTypes = {
  custom: CustomNode,
};

interface VisualScenarioEditorProps {
  editingScenario?: any;
  onScenarioSaved?: () => void;
}

const VisualScenarioEditor: React.FC<VisualScenarioEditorProps> = ({ editingScenario, onScenarioSaved }) => {
  const [nodes, setNodes, onNodesChange] = useNodesState([]);
  const [edges, setEdges, onEdgesChange] = useEdgesState([]);
  const [selectedNodeType, setSelectedNodeType] = useState<string>('message');
  const [selectedNode, setSelectedNode] = useState<Node | null>(null);
  const [settingsVisible, setSettingsVisible] = useState(false);
  const [saveModalVisible, setSaveModalVisible] = useState(false);
  const [importModalVisible, setImportModalVisible] = useState(false);
  const [loadModalVisible, setLoadModalVisible] = useState(false);
  const [scenarioName, setScenarioName] = useState('');
  const [scenarioDescription, setScenarioDescription] = useState('');
  const [importJson, setImportJson] = useState('');
  const [availableScenarios, setAvailableScenarios] = useState<any[]>([]);
  const [editingScenarioId, setEditingScenarioId] = useState<string | null>(null);
  const [jsonEditorVisible, setJsonEditorVisible] = useState(false);
  const [jsonContent, setJsonContent] = useState('');

  useEffect(() => {
    loadAvailableScenarios();
  }, []);

  // Синхронизация selectedNode с обновленными nodes
  useEffect(() => {
    if (selectedNode) {
      const updatedNode = nodes.find(node => node.id === selectedNode.id);
      if (updatedNode && updatedNode !== selectedNode) {
        setSelectedNode(updatedNode);
      }
    }
  }, [nodes, selectedNode]);

  // Загрузка переданного сценария для редактирования
  useEffect(() => {
    if (editingScenario) {
      loadScenario(editingScenario);
      setEditingScenarioId(editingScenario.id);
      message.info(`Загружен сценарий: ${editingScenario.name}`);
    }
  }, [editingScenario]);

  const loadAvailableScenarios = async () => {
    try {
      const scenarios = await scenarioService.getScenarios();
      console.log('Loaded scenarios:', scenarios);
      setAvailableScenarios(scenarios || []);
    } catch (error) {
      console.error('Error loading scenarios:', error);
      message.error('Ошибка загрузки списка сценариев');
    }
  };

  const onConnect = useCallback(
    (params: Connection) => setEdges((eds) => addEdge(params, eds)),
    [setEdges]
  );

  const onNodeClick = useCallback((event: React.MouseEvent, node: Node) => {
    setSelectedNode(node);
    setSettingsVisible(true);
  }, []);

  const updateNodeData = (nodeId: string, newData: any) => {
    setNodes((nds) =>
      nds.map((node) =>
        node.id === nodeId ? { ...node, data: { ...node.data, ...newData } } : node
      )
    );
    
    // Синхронизируем selectedNode с обновленными данными
    if (selectedNode && selectedNode.id === nodeId) {
      setSelectedNode({
        ...selectedNode,
        data: { ...selectedNode.data, ...newData }
      });
    }
  };

  const deleteNode = (nodeId: string) => {
    setNodes((nds) => nds.filter((node) => node.id !== nodeId));
    setEdges((eds) => eds.filter((edge) => edge.source !== nodeId && edge.target !== nodeId));
    setSelectedNode(null);
    setSettingsVisible(false);
    message.success('Узел удален');
  };

  const openJsonEditor = () => {
    const scenarioJson = {
      name: scenarioName,
      description: scenarioDescription,
      version: "1.0",
      language: "uk",
      category: "general",
      tags: [],
      is_active: true,
      is_entry_point: editingScenarioId === 'greeting-001',
      scenario_data: {
        start_node: nodes.length > 0 ? nodes[0].id : "start",
        nodes: nodes.map(node => ({
          id: node.id,
          type: node.data.type,
          parameters: {
            message: node.data.content,
            question: node.data.content,
            conditions: node.data.conditions,
            url: node.data.url,
            method: node.data.method,
            prompt: node.data.prompt,
            target_scenario: node.data.target_scenario
          },
          next_nodes: edges.filter(edge => edge.source === node.id).map(edge => edge.target),
          position: node.position
        })),
        edges: edges.map(edge => ({
          source: edge.source,
          target: edge.target,
          sourceHandle: edge.sourceHandle,
          targetHandle: edge.targetHandle
        }))
      }
    };
    setJsonContent(JSON.stringify(scenarioJson, null, 2));
    setJsonEditorVisible(true);
  };

  const saveFromJson = async () => {
    try {
      const scenarioData = JSON.parse(jsonContent);
      
      // Обновляем визуальный редактор
      setScenarioName(scenarioData.name);
      setScenarioDescription(scenarioData.description);
      
      // Конвертируем узлы
      const loadedNodes = scenarioData.scenario_data.nodes.map((node: any) => ({
        id: node.id,
        type: 'custom',
        position: node.position || { x: Math.random() * 300, y: Math.random() * 300 },
        data: {
          type: node.type,
          content: node.parameters?.message || node.parameters?.question || 'Узел',
          conditions: node.parameters?.conditions,
          url: node.parameters?.url,
          method: node.parameters?.method,
          prompt: node.parameters?.prompt,
          target_scenario: node.parameters?.target_scenario
        }
      }));
      
      // Конвертируем связи
      const loadedEdges = scenarioData.scenario_data.edges?.map((edge: any) => ({
        id: `${edge.source}-${edge.target}`,
        source: edge.source,
        target: edge.target,
        type: 'smoothstep',
        animated: true
      })) || [];
      
      setNodes(loadedNodes);
      setEdges(loadedEdges);
      
      // Сохраняем сценарий
      if (editingScenarioId) {
        await scenarioService.updateScenario(editingScenarioId, scenarioData);
        message.success('Сценарий обновлен из JSON');
      } else {
        await scenarioService.createScenario(scenarioData);
        message.success('Сценарий создан из JSON');
      }
      
      setJsonEditorVisible(false);
      onScenarioSaved?.();
    } catch (error) {
      message.error('Ошибка в JSON формате');
      console.error('JSON parse error:', error);
    }
  };

  const addNode = () => {
    const nodeTypeLabels = {
      'announce': 'Анонс',
      'ask': 'Вопрос', 
      'api_call': 'API запрос',
      'llm_call': 'LLM запрос',
      'nlu-request': 'NLU запрос',
      'scenario_jump': 'Переход в сценарий',
      'transfer': 'Перевод на оператора',
      'end': 'Завершение диалога',
      'condition': 'Условие выбора'
    };

    const baseData = { 
      type: selectedNodeType,
      content: `Новый ${nodeTypeLabels[selectedNodeType] || selectedNodeType} узел`
    };
    
    // Add specific data for different node types
    let nodeData = baseData;
    if (selectedNodeType === 'condition') {
      nodeData = { ...baseData, conditions: ['условие 1', 'условие 2'], content: 'Условие выбора' };
    } else if (selectedNodeType === 'api_call') {
      nodeData = { ...baseData, url: 'https://api.example.com', method: 'GET', content: 'API запрос' };
    } else if (selectedNodeType === 'llm_call') {
      nodeData = { ...baseData, content: 'Запрос в LLM модель' };
    } else if (selectedNodeType === 'nlu-request') {
      nodeData = { ...baseData, content: 'Анализ через NLU' };
    } else if (selectedNodeType === 'scenario_jump') {
      nodeData = { ...baseData, content: 'Переход в другой сценарий' };
    } else if (selectedNodeType === 'transfer') {
      nodeData = { ...baseData, content: 'Перевод на оператора' };
    } else if (selectedNodeType === 'end') {
      nodeData = { ...baseData, content: 'Завершение диалога' };
    } else if (selectedNodeType === 'announce') {
      nodeData = { ...baseData, content: 'Сообщение пользователю' };
    } else if (selectedNodeType === 'ask') {
      nodeData = { ...baseData, content: 'Вопрос пользователю' };
    }

    const newNode: Node = {
      id: `node_${Date.now()}`,
      type: 'custom',
      position: { x: Math.random() * 300 + 50, y: Math.random() * 300 + 50 },
      data: nodeData,
    };
    setNodes((nds) => nds.concat(newNode));
  };

  const saveScenarioToList = async () => {
    if (!scenarioName.trim()) {
      message.error('Введите название сценария');
      return;
    }

    const scenarioData = {
      name: scenarioName,
      description: scenarioDescription,
      version: '1.0',
      language: 'uk',
      category: 'custom',
      tags: ['визуальный'],
      is_active: true,
      is_entry_point: editingScenarioId === 'greeting-001', // Сохраняем entry point статус
      scenario_data: {
        start_node: nodes.length > 0 ? nodes[0].id : 'start',
        nodes: nodes.map(node => {
          const baseNode = {
            id: node.id,
            type: node.data.type,
            content: node.data.content,
            position: node.position
          };

          // Для NLU узлов добавляем специальные параметры
          if (node.data.type === 'nlu-request') {
            return {
              ...baseNode,
              service: node.data.service || 'nlu-service',
              endpoint: node.data.endpoint || '/api/v1/nlu/analyze',
              conditions: {
                success: edges.find(edge => edge.source === node.id && edge.sourceHandle === 'success')?.target || 
                         edges.find(edge => edge.source === node.id)?.target,
                error: edges.find(edge => edge.source === node.id && edge.sourceHandle === 'error')?.target
              }
            };
          }

          // Для остальных узлов используем старый формат
          return {
            ...baseNode,
            parameters: {
              message: node.data.content,
              question: node.data.content,
              conditions: node.data.conditions,
              url: node.data.url,
              method: node.data.method,
              prompt: node.data.prompt,
              target_scenario: node.data.target_scenario
            },
            next_nodes: edges
              .filter(edge => edge.source === node.id)
              .map(edge => edge.target)
          };
        }),
        edges: edges.map(edge => ({
          source: edge.source,
          target: edge.target,
          sourceHandle: edge.sourceHandle,
          targetHandle: edge.targetHandle
        }))
      }
    };

    try {
      if (editingScenarioId) {
        // Обновляем существующий сценарий
        await scenarioService.updateScenario(editingScenarioId, scenarioData);
        message.success('Сценарий обновлен');
      } else {
        // Создаем новый сценарий
        await scenarioService.createScenario(scenarioData);
        message.success('Сценарий создан');
      }
      setSaveModalVisible(false);
      setScenarioName('');
      setScenarioDescription('');
      onScenarioSaved?.(); // Вызываем callback для обновления списка
    } catch (error) {
      message.error('Ошибка сохранения сценария');
    }
  };

  const createNewScenario = () => {
    setNodes([]);
    setEdges([]);
    setScenarioName('');
    setScenarioDescription('');
    setSelectedNode(null);
    setEditingScenarioId(null); // Очищаем ID редактируемого сценария
    message.info('Создание нового сценария');
  };

  const exportScenario = () => {
    const exportData = {
      name: scenarioName || 'Визуальный сценарий',
      description: scenarioDescription || 'Экспортированный сценарий',
      version: "1.0",
      language: "uk",
      category: "general",
      tags: [],
      is_active: true,
      is_entry_point: editingScenarioId === 'greeting-001',
      scenario_data: {
        start_node: nodes.length > 0 ? nodes[0].id : "start",
        nodes: nodes.map(node => ({
          id: node.id,
          type: node.data.type,
          parameters: {
            message: node.data.content,
            question: node.data.content,
            conditions: node.data.conditions,
            url: node.data.url,
            method: node.data.method,
            prompt: node.data.prompt,
            target_scenario: node.data.target_scenario
          },
          next_nodes: edges.filter(edge => edge.source === node.id).map(edge => edge.target),
          position: node.position
        })),
        edges: edges.map(edge => ({
          source: edge.source,
          target: edge.target,
          sourceHandle: edge.sourceHandle,
          targetHandle: edge.targetHandle
        }))
      }
    };

    const blob = new Blob([JSON.stringify(exportData, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `${scenarioName || 'scenario'}.json`;
    a.click();
    URL.revokeObjectURL(url);
    message.success('Сценарий экспортирован');
  };

  const importScenario = () => {
    try {
      const imported = JSON.parse(importJson);
      
      // Поддержка как старого, так и нового формата
      let nodes, edges, name, description;
      
      if (imported.scenario_data) {
        // Новый формат (полный сценарий)
        nodes = imported.scenario_data.nodes;
        edges = imported.scenario_data.edges;
        name = imported.name;
        description = imported.description;
      } else {
        // Старый формат (только узлы и связи)
        nodes = imported.nodes;
        edges = imported.edges;
        name = imported.name;
        description = imported.description;
      }

      // Convert imported nodes to ReactFlow format
      const importedNodes = nodes?.map((node: any, index: number) => ({
        id: node.id,
        type: 'custom',
        position: node.position || { 
          x: 100 + (index % 3) * 200, 
          y: 100 + Math.floor(index / 3) * 150 
        },
        data: {
          type: node.type,
          content: node.parameters?.message || node.parameters?.question || node.content || 'Узел',
          conditions: node.parameters?.conditions || node.conditions,
          url: node.parameters?.url || node.url,
          method: node.parameters?.method || node.method,
          prompt: node.parameters?.prompt || node.prompt,
          target_scenario: node.parameters?.target_scenario || node.target_scenario
        }
      })) || [];

      const importedEdges = edges?.map((edge: any) => ({
        id: `${edge.source}-${edge.target}`,
        source: edge.source,
        target: edge.target,
        sourceHandle: edge.sourceHandle,
        targetHandle: edge.targetHandle,
        type: 'smoothstep',
        animated: true
      })) || [];

      setNodes(importedNodes);
      setEdges(importedEdges);
      setScenarioName(name || '');
      setScenarioDescription(description || '');
      setImportModalVisible(false);
      message.success('Сценарий импортирован');
    } catch (error) {
      message.error('Ошибка импорта: неверный JSON');
    }
  };

  const loadScenario = (scenario: any) => {
    try {
      const scenarioData = scenario.scenario_data;
      if (!scenarioData || !scenarioData.nodes) {
        // Создаем пустой сценарий для редактирования
        setNodes([]);
        setEdges([]);
        setScenarioName(scenario.name);
        setScenarioDescription(scenario.description);
        setEditingScenarioId(scenario.id);
        setLoadModalVisible(false);
        message.warning(`Сценарий "${scenario.name}" загружен как пустой для редактирования`);
        return;
      }

      // Convert scenario nodes to ReactFlow format
      const loadedNodes = scenarioData.nodes.map((node: any, index: number) => {
        const baseNode = {
          id: node.id,
          type: 'custom',
          position: node.position || { 
            x: 100 + (index % 3) * 200, 
            y: 100 + Math.floor(index / 3) * 150 
          }
        };

        // Специальная обработка для NLU узлов
        if (node.type === 'nlu-request' || node.type === 'nlu_call') {
          return {
            ...baseNode,
            data: {
              type: 'nlu-request', // Нормализуем тип
              content: node.content || 'Анализ через NLU',
              service: node.service || 'nlu-service',
              endpoint: node.endpoint || '/api/v1/nlu/analyze',
              conditions: node.conditions
            }
          };
        }

        // Обычные узлы
        return {
          ...baseNode,
          data: {
            type: node.type,
            content: node.parameters?.message || node.parameters?.question || node.content || 'Узел без содержимого',
            conditions: node.parameters?.conditions || node.conditions,
            options: node.parameters?.options,
            url: node.parameters?.url,
            method: node.parameters?.method
          }
        };
      });

      // Convert edges if available
      const loadedEdges = scenarioData.edges?.map((edge: any) => ({
        id: `${edge.source}-${edge.target}`,
        source: edge.source,
        target: edge.target,
        sourceHandle: edge.sourceHandle,
        targetHandle: edge.targetHandle,
        type: 'smoothstep',
        animated: true
      })) || [];

      // If no edges, create them from next_nodes
      if (loadedEdges.length === 0) {
        const generatedEdges: any[] = [];
        scenarioData.nodes.forEach((node: any) => {
          if (node.next_nodes && node.next_nodes.length > 0) {
            node.next_nodes.forEach((targetId: string) => {
              generatedEdges.push({
                id: `${node.id}-${targetId}`,
                source: node.id,
                target: targetId,
                type: 'smoothstep',
                animated: true
              });
            });
          }
        });
        setEdges(generatedEdges);
      } else {
        setEdges(loadedEdges);
      }

      setNodes(loadedNodes);
      setScenarioName(scenario.name);
      setScenarioDescription(scenario.description);
      setEditingScenarioId(scenario.id); // Устанавливаем ID для редактирования
      setLoadModalVisible(false);
      message.success(`Сценарий "${scenario.name}" загружен`);
    } catch (error) {
      message.error('Ошибка загрузки сценария');
      console.error('Load scenario error:', error);
    }
  };

  const renderNodeSettings = () => {
    if (!selectedNode) return null;

    const { data } = selectedNode;

    return (
      <Form layout="vertical">
        <Form.Item label="Содержимое">
          <TextArea
            value={data.content || ''}
            onChange={(e) => updateNodeData(selectedNode.id, { content: e.target.value })}
            rows={3}
            placeholder="Введите текст узла"
            disabled={false}
          />
        </Form.Item>

        {data.type === 'condition' && (
          <Form.Item label="Условия (по одному на строку)">
            <TextArea
              value={(data.conditions || []).join('\n')}
              onChange={(e) => updateNodeData(selectedNode.id, { 
                conditions: e.target.value.split('\n').filter(Boolean) 
              })}
              rows={4}
              placeholder="условие 1&#10;условие 2&#10;комплексное условие"
              disabled={false}
            />
          </Form.Item>
        )}

        {data.type === 'llm_call' && (
          <Form.Item label="Промпт для LLM">
            <TextArea
              value={data.prompt || ''}
              onChange={(e) => updateNodeData(selectedNode.id, { prompt: e.target.value })}
              rows={4}
              placeholder="Введите промпт для LLM модели"
              disabled={false}
            />
          </Form.Item>
        )}

        {data.type === 'nlu-request' && (
          <>
            <Form.Item label="NLU сервис">
              <Input
                value={data.service || 'nlu-service'}
                onChange={(e) => updateNodeData(selectedNode.id, { service: e.target.value })}
                placeholder="nlu-service"
                disabled={false}
              />
            </Form.Item>
            <Form.Item label="Endpoint">
              <Input
                value={data.endpoint || '/api/v1/nlu/analyze'}
                onChange={(e) => updateNodeData(selectedNode.id, { endpoint: e.target.value })}
                placeholder="/api/v1/nlu/analyze"
                disabled={false}
              />
            </Form.Item>
          </>
        )}

        {data.type === 'scenario_jump' && (
          <Form.Item label="ID целевого сценария">
            <Input
              value={data.target_scenario || ''}
              onChange={(e) => updateNodeData(selectedNode.id, { target_scenario: e.target.value })}
              placeholder="scenario-id"
              disabled={false}
            />
          </Form.Item>
        )}

        {data.type === 'api_call' && (
          <>
            <Form.Item label="URL">
              <Input
                value={data.url || ''}
                onChange={(e) => updateNodeData(selectedNode.id, { url: e.target.value })}
                placeholder="https://api.example.com/endpoint"
                disabled={false}
              />
            </Form.Item>
            <Form.Item label="Метод">
              <Select
                value={data.method || 'GET'}
                onChange={(value) => updateNodeData(selectedNode.id, { method: value })}
                disabled={false}
              >
                <Option value="GET">GET</Option>
                <Option value="POST">POST</Option>
                <Option value="PUT">PUT</Option>
                <Option value="DELETE">DELETE</Option>
              </Select>
            </Form.Item>
          </>
        )}
        
        <Form.Item>
          <Button 
            type="primary" 
            danger 
            icon={<DeleteOutlined />}
            onClick={() => deleteNode(selectedNode.id)}
            style={{ marginTop: 16 }}
          >
            Удалить узел
          </Button>
        </Form.Item>
      </Form>
    );
  };

  return (
    <div style={{ height: '600px', border: '1px solid #d9d9d9', borderRadius: 8 }}>
      <div style={{ padding: 16, borderBottom: '1px solid #d9d9d9', background: '#fafafa' }}>
        <Space>
          <Select 
            value={selectedNodeType} 
            onChange={setSelectedNodeType}
            style={{ width: 150 }}
          >
            <Option value="announce">Анонс</Option>
            <Option value="ask">Вопрос</Option>
            <Option value="api_call">Запрос в API</Option>
            <Option value="llm_call">Запрос в LLM</Option>
            <Option value="nlu-request">Запрос в NLU</Option>
            <Option value="scenario_jump">Переход в сценарий</Option>
            <Option value="transfer">Перевод на оператора</Option>
            <Option value="end">Завершение диалога</Option>
            <Option value="condition">Условие выбора</Option>
          </Select>
          <Button type="primary" icon={<PlusOutlined />} onClick={addNode}>
            Добавить узел
          </Button>
          <Button type="default" icon={<SaveOutlined />} onClick={() => setSaveModalVisible(true)}>
            {editingScenarioId ? 'Обновить' : 'Сохранить'}
          </Button>
          <Button type="default" icon={<PlusOutlined />} onClick={createNewScenario}>
            Новый
          </Button>
          <Button type="default" icon={<FolderOpenOutlined />} onClick={() => setLoadModalVisible(true)}>
            Загрузить
          </Button>
          <Button type="default" onClick={openJsonEditor}>
            JSON
          </Button>
          <Button type="default" icon={<ExportOutlined />} onClick={exportScenario}>
            Экспорт
          </Button>
          <Button type="default" icon={<ImportOutlined />} onClick={() => setImportModalVisible(true)}>
            Импорт
          </Button>
        </Space>
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
        connectionLineType="smoothstep"
        defaultEdgeOptions={{
          type: 'smoothstep',
          animated: true,
          style: { strokeWidth: 2 }
        }}
        deleteKeyCode={['Delete', 'Backspace']}
        multiSelectionKeyCode={['Meta', 'Ctrl']}
      >
        <Controls />
        <Background />
      </ReactFlow>

      <Drawer
        title={`Настройки узла: ${selectedNode?.data.type.toUpperCase()}`}
        placement="right"
        onClose={() => setSettingsVisible(false)}
        open={settingsVisible}
        width={400}
      >
        {renderNodeSettings()}
      </Drawer>

      <Modal
        title="Сохранить сценарий"
        open={saveModalVisible}
        onOk={saveScenarioToList}
        onCancel={() => setSaveModalVisible(false)}
      >
        <Form layout="vertical">
          <Form.Item label="Название сценария" required>
            <Input
              value={scenarioName}
              onChange={(e) => setScenarioName(e.target.value)}
              placeholder="Например: Проверка баланса"
            />
          </Form.Item>
          <Form.Item label="Описание">
            <TextArea
              value={scenarioDescription}
              onChange={(e) => setScenarioDescription(e.target.value)}
              placeholder="Описание сценария"
              rows={3}
            />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="Импорт сценария"
        open={importModalVisible}
        onOk={importScenario}
        onCancel={() => setImportModalVisible(false)}
        width={600}
      >
        <TextArea
          value={importJson}
          onChange={(e) => setImportJson(e.target.value)}
          placeholder="Вставьте JSON сценария..."
          rows={15}
        />
      </Modal>

      <Modal
        title="Загрузить существующий сценарий"
        open={loadModalVisible}
        onCancel={() => setLoadModalVisible(false)}
        footer={[
          <Button key="refresh" onClick={loadAvailableScenarios}>
            Обновить список
          </Button>,
          <Button key="cancel" onClick={() => setLoadModalVisible(false)}>
            Отмена
          </Button>
        ]}
        width={800}
      >
        <List
          dataSource={availableScenarios}
          renderItem={(scenario) => (
            <List.Item
              actions={[
                <Button 
                  type="primary" 
                  onClick={() => loadScenario(scenario)}
                >
                  Загрузить
                </Button>
              ]}
            >
              <List.Item.Meta
                title={scenario.name}
                description={
                  <div>
                    <div>{scenario.description}</div>
                    <div style={{ fontSize: 12, color: '#666', marginTop: 4 }}>
                      Категория: {scenario.category} | Теги: {scenario.tags?.join(', ')}
                    </div>
                  </div>
                }
              />
            </List.Item>
          )}
        />
      </Modal>

      <Modal
        title="JSON редактор сценария"
        open={jsonEditorVisible}
        onOk={saveFromJson}
        onCancel={() => setJsonEditorVisible(false)}
        width={800}
        okText="Применить"
        cancelText="Отмена"
      >
        <TextArea
          value={jsonContent}
          onChange={(e) => setJsonContent(e.target.value)}
          rows={20}
          style={{ fontFamily: 'monospace', fontSize: '12px' }}
          placeholder="JSON структура сценария"
        />
      </Modal>
    </div>
  );
};

export default VisualScenarioEditor;
