import React, { useState, useEffect } from 'react';
import { Card, Table, Button, Space, message, Modal, Tabs } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, EyeOutlined, NodeIndexOutlined } from '@ant-design/icons';
import { scenarioService } from '../../services/scenarioService';
import VisualScenarioEditor from '../../components/VisualScenarioEditor';

interface Scenario {
  id: string;
  name: string;
  description: string;
  trigger_intents: string[];
  nodes: any[];
  created_at: string;
  updated_at: string;
}

const Scenarios: React.FC = () => {
  const [scenarios, setScenarios] = useState<Scenario[]>([]);
  const [loading, setLoading] = useState(false);
  const [viewModalVisible, setViewModalVisible] = useState(false);
  const [selectedScenario, setSelectedScenario] = useState<Scenario | null>(null);
  const [editingScenario, setEditingScenario] = useState<Scenario | null>(null);
  const [activeTab, setActiveTab] = useState('list');

  useEffect(() => {
    loadScenarios();
  }, []);

  const loadScenarios = async () => {
    setLoading(true);
    try {
      const data = await scenarioService.getScenarios();
      setScenarios(data);
    } catch (error) {
      message.error('Ошибка загрузки сценариев');
    } finally {
      setLoading(false);
    }
  };

  const deleteScenario = async (id: string, name: string) => {
    Modal.confirm({
      title: 'Удалить сценарий?',
      content: `Вы уверены, что хотите удалить сценарий "${name}"? Это действие нельзя отменить.`,
      okText: 'Удалить',
      okType: 'danger',
      cancelText: 'Отмена',
      onOk: async () => {
        try {
          await scenarioService.deleteScenario(id);
          message.success('Сценарий удален');
          loadScenarios();
        } catch (error) {
          message.error('Ошибка удаления сценария');
        }
      }
    });
  };

  const viewScenario = (scenario: Scenario) => {
    setSelectedScenario(scenario);
    setViewModalVisible(true);
  };

  const editScenario = (scenario: Scenario) => {
    setEditingScenario(scenario);
    setActiveTab('visual'); // Переключаемся на визуальный редактор
  };

  const columns = [
    {
      title: 'ID сценария',
      dataIndex: 'id',
      key: 'id',
      width: 320,
      render: (id: string) => (
        <span style={{ fontFamily: 'monospace', fontSize: '11px', wordBreak: 'break-all' }}>
          {id}
        </span>
      ),
    },
    {
      title: 'Название',
      dataIndex: 'name',
      key: 'name',
      width: 200,
    },
    {
      title: 'Описание',
      dataIndex: 'description',
      key: 'description',
      width: 300,
      render: (text: string) => (
        <span style={{ wordBreak: 'break-word' }}>{text}</span>
      ),
    },
    {
      title: 'Создан',
      dataIndex: 'created_at',
      key: 'created_at',
      width: 150,
      render: (date: string) => new Date(date).toLocaleDateString('ru-RU'),
    },
    {
      title: 'Узлов',
      dataIndex: 'scenario_data',
      key: 'nodes_count',
      width: 80,
      render: (scenarioData: any) => scenarioData?.nodes?.length || 0,
    },
    {
      title: 'Обновлен',
      dataIndex: 'updated_at',
      key: 'updated_at',
      render: (date: string) => date ? new Date(date).toLocaleString('ru-RU') : '-',
    },
    {
      title: 'Действия',
      key: 'actions',
      width: 150,
      render: (_: any, record: Scenario) => (
        <Space>
          <Button
            type="text"
            icon={<EyeOutlined />}
            onClick={() => viewScenario(record)}
            title="Просмотр"
          />
          <Button
            type="text"
            icon={<EditOutlined />}
            onClick={() => editScenario(record)}
            title="Редактировать"
          />
          <Button
            type="text"
            danger
            icon={<DeleteOutlined />}
            onClick={() => deleteScenario(record.id, record.name)}
            title="Удалить"
          />
        </Space>
      ),
    },
  ];

  const tabItems = [
    {
      key: 'visual',
      label: (
        <span>
          <NodeIndexOutlined />
          Визуальный редактор
        </span>
      ),
      children: <VisualScenarioEditor editingScenario={editingScenario} onScenarioSaved={() => { setEditingScenario(null); loadScenarios(); }} />
    },
    {
      key: 'list',
      label: (
        <span>
          <EditOutlined />
          Список сценариев
        </span>
      ),
      children: (
        <Card 
          title="Управление сценариями"
          extra={
            <Button 
              type="primary" 
              icon={<PlusOutlined />}
              onClick={loadScenarios}
            >
              Обновить
            </Button>
          }
        >
          <Table
            columns={columns}
            dataSource={scenarios}
            rowKey="id"
            loading={loading}
            pagination={{
              pageSize: 10,
              showSizeChanger: true,
              showQuickJumper: true,
            }}
          />
        </Card>
      )
    }
  ];

  return (
    <div style={{ padding: 24 }}>
      <Tabs 
        activeKey={activeTab} 
        onChange={setActiveTab}
        items={tabItems} 
      />

      <Modal
        title="Просмотр сценария"
        open={viewModalVisible}
        onCancel={() => setViewModalVisible(false)}
        footer={null}
        width={800}
      >
        {selectedScenario && (
          <div>
            <h3>{selectedScenario.name}</h3>
            <p><strong>Описание:</strong> {selectedScenario.description}</p>
            <p><strong>Интенты:</strong> {selectedScenario.trigger_intents?.join(', ') || 'Нет'}</p>
            <p><strong>Узлов:</strong> {selectedScenario.nodes?.length || 0}</p>
            
            <h4>JSON структура:</h4>
            <pre style={{ 
              background: '#f5f5f5', 
              padding: 16, 
              borderRadius: 4,
              maxHeight: 400,
              overflow: 'auto'
            }}>
              {JSON.stringify(selectedScenario, null, 2)}
            </pre>
          </div>
        )}
      </Modal>
    </div>
  );
};

export default Scenarios;
