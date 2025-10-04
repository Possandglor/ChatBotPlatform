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
import { Card, Button, Space, Select, Input, message, Tag, Drawer, Form, Modal, List, Checkbox } from 'antd';
import { PlusOutlined, SaveOutlined, SettingOutlined, ExportOutlined, ImportOutlined, FolderOpenOutlined, DeleteOutlined } from '@ant-design/icons';
import { scenarioService } from '../services/scenarioService';
import { useBranchStore } from '../store/branchStore';

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
      end: '#8c8c8c',
      'context-edit': '#fa8c16'
    };
    return colors[type as keyof typeof colors] || '#d9d9d9';
  };

  const renderOutputHandles = () => {
    if (data.type === 'condition' || data.type === 'switch') {
      // ИСПРАВЛЕНО: Поддержка как строки, так и массива
      let conditions = [];
      if (typeof data.conditions === 'string') {
        conditions = data.conditions.split('\n').filter(line => {
          const trimmed = line.trim();
          return trimmed && !trimmed.startsWith('//') && !trimmed.startsWith('#');
        });
      } else if (Array.isArray(data.conditions)) {
        conditions = data.conditions;
      } else {
        conditions = ['true', 'false'];
      }
      
      // Добавляем ELSE выход
      const outputs = [...conditions, 'ELSE'];
      
      return outputs.map((condition: string, index: number) => (
        <Handle
          key={`${id}-${condition}-${index}`}
          type="source"
          position={Position.Right}
          id={`output-${index}`}
          style={{ 
            top: `${30 + (index * 20)}px`,
            right: '-6px',
            width: '12px',
            height: '12px',
            backgroundColor: index === outputs.length - 1 ? '#ff4d4f' : '#52c41a'
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
        {(data.type === 'condition' || data.type === 'switch') && (
          <div style={{ fontSize: 10 }}>
            {(() => {
              let conditions = [];
              if (typeof data.conditions === 'string') {
                conditions = data.conditions.split('\n').filter(line => {
                  const trimmed = line.trim();
                  return trimmed && !trimmed.startsWith('//') && !trimmed.startsWith('#');
                });
              } else if (Array.isArray(data.conditions)) {
                conditions = data.conditions;
              } else {
                conditions = ['true', 'false'];
              }
              
              return [...conditions, 'ELSE'].map((condition: string, index: number) => (
                <div key={`${condition}-${index}`} style={{ marginBottom: 2 }}>
                  → {condition.length > 20 ? condition.substring(0, 20) + '...' : condition}
                </div>
              ));
            })()}
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
  const [selectedNodeType, setSelectedNodeType] = useState<string>('announce');
  const [selectedNode, setSelectedNode] = useState<Node | null>(null);
  const [settingsVisible, setSettingsVisible] = useState(false);
  const [saveModalVisible, setSaveModalVisible] = useState(false);
  const [importModalVisible, setImportModalVisible] = useState(false);
  const [loadModalVisible, setLoadModalVisible] = useState(false);
  const [scenarioName, setScenarioName] = useState('');
  const [scenarioDescription, setScenarioDescription] = useState('');
  const [isEntryPoint, setIsEntryPoint] = useState(false);
  const [importJson, setImportJson] = useState('');
  const [availableScenarios, setAvailableScenarios] = useState<any[]>([]);
  const [editingScenarioId, setEditingScenarioId] = useState<string | null>(null);
  const [jsonEditorVisible, setJsonEditorVisible] = useState(false);
  const [jsonContent, setJsonContent] = useState('');
  const { currentBranch } = useBranchStore();

  useEffect(() => {
    loadAvailableScenarios();
  }, [currentBranch]); // Перезагружаем при смене ветки

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

  const updateNodeId = (oldId: string, newId: string) => {
    if (!newId || newId === oldId) return;
    
    // Обновляем ID узла
    setNodes((nds) =>
      nds.map((node) => {
        if (node.id === oldId) {
          return { ...node, id: newId };
        }
        return node;
      })
    );
    
    // Обновляем все ссылки в edges
    setEdges((eds) =>
      eds.map((edge) => ({
        ...edge,
        id: edge.id.replace(oldId, newId),
        source: edge.source === oldId ? newId : edge.source,
        target: edge.target === oldId ? newId : edge.target,
      }))
    );
    
    // Обновляем selectedNode
    if (selectedNode && selectedNode.id === oldId) {
      setSelectedNode({
        ...selectedNode,
        id: newId
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
      is_entry_point: isEntryPoint,
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
      setIsEntryPoint(scenarioData.is_entry_point || false);
      
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
          body: node.parameters?.body,
          headers: node.parameters?.headers,
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
      'sub-flow': 'Подсценарий',
      'transfer': 'Перевод на оператора',
      'end': 'Завершение/Возврат',
      'end_dialog': 'Конец диалога',
      'condition': 'Условие выбора',
      'switch': 'Множественное условие',
      'context-edit': 'Редактирование контекста'
    };

    const baseData = { 
      type: selectedNodeType,
      content: `Новый ${nodeTypeLabels[selectedNodeType as keyof typeof nodeTypeLabels] || selectedNodeType} узел`
    };
    
    // Add specific data for different node types
    let nodeData = baseData;
    if (selectedNodeType === 'condition') {
      nodeData = { ...baseData, conditions: 'intent == "check_balance"\nintent != "check_balance"', content: 'Условие выбора' };
    } else if (selectedNodeType === 'switch') {
      nodeData = { ...baseData, conditions: 'intent == "check_balance" || intent == "transfer_money"\nintent == "greeting"\n// ELSE для всех остальных', content: 'Множественное условие' };
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
    } else if (selectedNodeType === 'context-edit') {
      nodeData = { 
        ...baseData, 
        content: 'Редактирование контекста',
        operations: [
          { action: 'set', path: 'user.name', value: 'Новый пользователь' }
        ]
      };
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
      is_entry_point: isEntryPoint, // Сохраняем entry point статус
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
              body: node.data.body,
              headers: node.data.headers,
              prompt: node.data.prompt,
              target_scenario: node.data.target_scenario,
              operations: node.data.operations
            },
            next_nodes: (() => {
              // ИСПРАВЛЕНО: Сортируем выходы по sourceHandle для гарантированного порядка
              const nodeEdges = edges.filter(edge => edge.source === node.id);
              
              if (node.data.type === 'condition' || node.data.type === 'switch') {
                // Для condition/switch узлов сортируем по индексу в sourceHandle
                const sortedEdges = nodeEdges.sort((a, b) => {
                  const indexA = parseInt(a.sourceHandle?.replace('output-', '') || '0');
                  const indexB = parseInt(b.sourceHandle?.replace('output-', '') || '0');
                  return indexA - indexB;
                });
                return sortedEdges.map(edge => edge.target);
              } else {
                // Для остальных узлов - как было
                return nodeEdges.map(edge => edge.target);
              }
            })()
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
      setIsEntryPoint(false);
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
    setIsEntryPoint(false);
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
      is_entry_point: isEntryPoint,
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
          body: node.parameters?.body || node.body,
          headers: node.parameters?.headers || node.headers,
          prompt: node.parameters?.prompt || node.prompt,
          target_scenario: node.parameters?.target_scenario || node.target_scenario,
          operations: node.parameters?.operations || node.operations
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

  // Функции для работы с ветками
  const handleBranchChange = async (branchName: string) => {
    if (!editingScenarioId) return;
    
    try {
      setCurrentBranch(branchName);
      
      // Загружаем сценарий из выбранной ветки
      const scenarioFromBranch = await branchService.getScenarioFromBranch(editingScenarioId, branchName);
      if (scenarioFromBranch && scenarioFromBranch.scenarioData) {
        loadScenario(scenarioFromBranch);
        message.success(`Переключено на ветку: ${branchName}`);
      }
    } catch (error) {
      console.error('Error switching branch:', error);
      message.error('Ошибка переключения ветки');
    }
  };

  const handleBranchCreated = () => {
    // Обновляем список веток после создания новой
    message.info('Ветка создана успешно');
  };

  const saveToBranch = async () => {
    if (!editingScenarioId || !currentBranch) return;

    const scenarioData = {
      start_node: nodes.length > 0 ? nodes[0].id : 'start',
      nodes: nodes.map(node => {
        const baseNode = {
          id: node.id,
          type: node.data.type,
          content: node.data.content,
          position: node.position
        };

        return {
          ...baseNode,
          parameters: {
            message: node.data.content,
            question: node.data.content,
            conditions: node.data.conditions,
            url: node.data.url,
            method: node.data.method,
            body: node.data.body,
            headers: node.data.headers,
            prompt: node.data.prompt,
            target_scenario: node.data.target_scenario,
            operations: node.data.operations
          },
          next_nodes: (() => {
            const nodeEdges = edges.filter(edge => edge.source === node.id);
            
            if (node.data.type === 'condition' || node.data.type === 'switch') {
              const sortedEdges = nodeEdges.sort((a, b) => {
                const indexA = parseInt(a.sourceHandle?.replace('output-', '') || '0');
                const indexB = parseInt(b.sourceHandle?.replace('output-', '') || '0');
                return indexA - indexB;
              });
              return sortedEdges.map(edge => edge.target);
            } else {
              return nodeEdges.map(edge => edge.target);
            }
          })()
        };
      }),
      edges: edges.map(edge => ({
        source: edge.source,
        target: edge.target,
        sourceHandle: edge.sourceHandle,
        targetHandle: edge.targetHandle
      }))
    };

    try {
      // Всегда сохраняем через scenarioService - он автоматически учтет X-Branch header
      await scenarioService.updateScenario(editingScenarioId, {
        name: scenarioName,
        description: scenarioDescription,
        scenario_data: scenarioData
      });
      
      message.success(`Сценарий сохранен в ветку: ${currentBranch}`);
    } catch (error) {
      console.error('Error saving to branch:', error);
      message.error('Ошибка сохранения в ветку');
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
        setIsEntryPoint(scenario.is_entry_point || false);
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
            method: node.parameters?.method,
            body: node.parameters?.body,
            headers: node.parameters?.headers,
            target_scenario: node.parameters?.target_scenario,
            prompt: node.parameters?.prompt,
            operations: node.parameters?.operations
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
            node.next_nodes.forEach((targetId: string, index: number) => {
              // ИСПРАВЛЕНО: Добавляем sourceHandle с индексом для condition/switch узлов
              const sourceHandle = (node.type === 'condition' || node.type === 'switch') 
                ? `output-${index}` 
                : undefined;
                
              generatedEdges.push({
                id: `${node.id}-${targetId}`,
                source: node.id,
                target: targetId,
                sourceHandle: sourceHandle,
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
      setIsEntryPoint(scenario.is_entry_point || false);
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
        {/* ID узла - редактируемое поле */}
        <Form.Item label="ID узла">
          <Input
            value={selectedNode.id}
            onChange={(e) => updateNodeId(selectedNode.id, e.target.value)}
            placeholder="Уникальный ID узла"
            style={{ fontFamily: 'monospace' }}
          />
        </Form.Item>

        <Form.Item label="Содержимое">
          <TextArea
            value={data.content || ''}
            onChange={(e) => updateNodeData(selectedNode.id, { content: e.target.value })}
            rows={3}
            placeholder="Введите текст узла"
            disabled={false}
          />
        </Form.Item>

        {(data.type === 'condition' || data.type === 'switch') && (
          <Form.Item label={`${data.type === 'switch' ? 'Switch' : 'Условия'} (по одному на строку)`}>
            <TextArea
              value={Array.isArray(data.conditions) ? data.conditions.join('\n') : (data.conditions || '')}
              onChange={(e) => updateNodeData(selectedNode.id, { 
                conditions: e.target.value 
              })}
              rows={6}
              placeholder={`intent == "check_balance" || intent == "transfer_money"
intent == "greeting"
// Комментарий
intent != "unknown"
# Еще комментарий
// Последняя строка = ELSE (default)`}
              disabled={false}
            />
            <div style={{ marginTop: 8, fontSize: '12px', color: '#666' }}>
              💡 Поддерживается: OR (||), комментарии (//, #), последний выход = ELSE
            </div>
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
          <Form.Item label="Целевой сценарий">
            <Select
              value={data.target_scenario || ''}
              onChange={(value) => updateNodeData(selectedNode.id, { target_scenario: value })}
              placeholder="Выберите сценарий"
              showSearch
              filterOption={(input, option) => {
                const children = option?.children as string;
                return children?.toLowerCase().includes(input.toLowerCase()) || false;
              }}
              disabled={false}
            >
              {availableScenarios.map(scenario => (
                <Select.Option 
                  key={scenario.id} 
                  value={scenario.id}
                  title={`${scenario.name} (${scenario.id})`}
                >
                  {scenario.name}
                </Select.Option>
              ))}
            </Select>
            {data.target_scenario && (
              <div style={{ marginTop: 4, fontSize: '12px', color: '#666', fontFamily: 'monospace' }}>
                ID: {data.target_scenario}
              </div>
            )}
          </Form.Item>
        )}

        {data.type === 'sub-flow' && (
          <Form.Item label="Подсценарий">
            <Select
              value={data.target_scenario || ''}
              onChange={(value) => updateNodeData(selectedNode.id, { target_scenario: value })}
              placeholder="Выберите подсценарий"
              showSearch
              filterOption={(input, option) => {
                const children = option?.children as string;
                return children?.toLowerCase().includes(input.toLowerCase()) || false;
              }}
              disabled={false}
            >
              {availableScenarios.map(scenario => (
                <Select.Option 
                  key={scenario.id} 
                  value={scenario.id}
                  title={`${scenario.name} (${scenario.id})`}
                >
                  {scenario.name}
                </Select.Option>
              ))}
            </Select>
            {data.target_scenario && (
              <div style={{ marginTop: 4, fontSize: '12px', color: '#666', fontFamily: 'monospace' }}>
                ID: {data.target_scenario}
              </div>
            )}
            <div style={{ marginTop: 8, fontSize: '12px', color: '#999' }}>
              💡 После завершения подсценария вернется к следующему узлу
            </div>
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
            
            <Form.Item label="Тело запроса (JSON)">
              <Input.TextArea
                value={data.body || ''}
                onChange={(e) => updateNodeData(selectedNode.id, { body: e.target.value })}
                placeholder='{"key": "value", "user": "{context.user_input}"}'
                rows={3}
                disabled={false}
              />
            </Form.Item>
            
            <Form.Item label="Заголовки (JSON)">
              <Input.TextArea
                value={data.headers || ''}
                onChange={(e) => updateNodeData(selectedNode.id, { headers: e.target.value })}
                placeholder='{"Authorization": "Bearer token", "Content-Type": "application/json"}'
                rows={2}
                disabled={false}
              />
            </Form.Item>
          </>
        )}

        {data.type === 'context-edit' && (
          <>
            <Form.Item label="Операции с контекстом">
              <div style={{ marginBottom: 8, fontSize: '12px', color: '#666' }}>
                Поддерживаемые операции: set, delete, add, merge, clear
              </div>
              <Input.TextArea
                value={typeof data.operations === 'string' ? data.operations : JSON.stringify(data.operations || [], null, 2)}
                onChange={(e) => {
                  try {
                    const operations = JSON.parse(e.target.value);
                    updateNodeData(selectedNode.id, { operations });
                  } catch {
                    updateNodeData(selectedNode.id, { operations: e.target.value });
                  }
                }}
                placeholder={`[
  {
    "action": "set",
    "path": "user.name",
    "value": "Олександр Петренко"
  },
  {
    "action": "add", 
    "path": "user.permissions[]",
    "value": "admin"
  },
  {
    "action": "delete",
    "path": "temp_data"
  }
]`}
                rows={8}
                disabled={false}
              />
            </Form.Item>
            <div style={{ fontSize: '12px', color: '#999', marginTop: -16, marginBottom: 16 }}>
              💡 Примеры путей: user.name, user.profile.theme, users[0].name, api_response.data[0]<br/>
              💡 Динамические значения: "Привет, {`{context.user.name}`}!"
            </div>
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
            style={{ width: 220 }}
            dropdownStyle={{ minWidth: 220 }}
          >
            <Option value="announce">Анонс</Option>
            <Option value="ask">Вопрос</Option>
            <Option value="api_call">Запрос в API</Option>
            <Option value="llm_call">Запрос в LLM</Option>
            <Option value="nlu-request">Запрос в NLU</Option>
            <Option value="scenario_jump">Переход в сценарий</Option>
            <Option value="sub-flow">Подсценарий</Option>
            <Option value="transfer">Перевод на оператора</Option>
            <Option value="end">Завершение/Возврат</Option>
            <Option value="end_dialog">Конец диалога</Option>
            <Option value="condition">Условие выбора</Option>
            <Option value="switch">Множественное условие</Option>
            <Option value="context-edit">Редактирование контекста</Option>
          </Select>
          <Button type="primary" icon={<PlusOutlined />} onClick={addNode}>
            Добавить узел
          </Button>
          <Button type="default" icon={<SaveOutlined />} onClick={saveToBranch}>
            Сохранить в {currentBranch}
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
          <Form.Item>
            <Checkbox
              checked={isEntryPoint}
              onChange={(e) => setIsEntryPoint(e.target.checked)}
            >
              Точка входа (стартовый сценарий)
            </Checkbox>
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
