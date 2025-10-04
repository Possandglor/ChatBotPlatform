import React, { useState, useEffect } from 'react';
import { Select, Button, Modal, Input, message, Space, Popconfirm, Tag } from 'antd';
import { BranchesOutlined, PlusOutlined, MergeOutlined, DeleteOutlined, HistoryOutlined } from '@ant-design/icons';
import branchService from '../services/branchService';

interface BranchSelectorProps {
  currentBranch: string;
  onBranchChange: (branchName: string) => void;
  onBranchCreated?: () => void;
}

const BranchSelector: React.FC<BranchSelectorProps> = ({
  currentBranch,
  onBranchChange,
  onBranchCreated
}) => {
  const [branches, setBranches] = useState<string[]>([]);
  const [loading, setLoading] = useState(false);
  const [createModalVisible, setCreateModalVisible] = useState(false);
  const [historyModalVisible, setHistoryModalVisible] = useState(false);
  const [newBranchName, setNewBranchName] = useState('');
  const [sourceBranch, setSourceBranch] = useState('main');
  const [history, setHistory] = useState<any[]>([]);

  useEffect(() => {
    loadBranches();
  }, []);

  const loadBranches = async () => {
    try {
      setLoading(true);
      const branchList = await branchService.getBranches();
      setBranches(branchList);
    } catch (error) {
      console.error('Error loading branches:', error);
      message.error('Ошибка загрузки веток');
    } finally {
      setLoading(false);
    }
  };

  const handleCreateBranch = async () => {
    if (!newBranchName.trim()) {
      message.error('Введите название ветки');
      return;
    }

    try {
      setLoading(true);
      await branchService.createBranch(newBranchName, sourceBranch, 'developer');
      message.success(`Ветка "${newBranchName}" создана`);
      
      setCreateModalVisible(false);
      setNewBranchName('');
      await loadBranches();
      onBranchCreated?.();
      
      // Переключаемся на новую ветку
      onBranchChange(newBranchName);
    } catch (error) {
      console.error('Error creating branch:', error);
      message.error('Ошибка создания ветки');
    } finally {
      setLoading(false);
    }
  };

  const handleMergeBranch = async () => {
    if (currentBranch === 'main') {
      message.warning('Нельзя слить main ветку');
      return;
    }

    try {
      setLoading(true);
      const result = await branchService.mergeBranch(currentBranch, 'main', 'developer');
      
      if (result.success) {
        message.success(`Ветка "${currentBranch}" слита с main`);
        onBranchChange('main');
        await loadBranches();
      } else {
        message.error(`Ошибка слияния: ${result.message}`);
      }
    } catch (error) {
      console.error('Error merging branch:', error);
      message.error('Ошибка слияния ветки');
    } finally {
      setLoading(false);
    }
  };

  const handleDeleteBranch = async () => {
    if (currentBranch === 'main') {
      message.warning('Нельзя удалить main ветку');
      return;
    }

    try {
      setLoading(true);
      await branchService.deleteBranch(currentBranch);
      message.success(`Ветка "${currentBranch}" удалена`);
      
      onBranchChange('main');
      await loadBranches();
    } catch (error) {
      console.error('Error deleting branch:', error);
      message.error('Ошибка удаления ветки');
    } finally {
      setLoading(false);
    }
  };

  const showHistory = async () => {
    try {
      setLoading(true);
      const historyData = await branchService.getHistory();
      setHistory(historyData);
      setHistoryModalVisible(true);
    } catch (error) {
      console.error('Error loading history:', error);
      message.error('Ошибка загрузки истории');
    } finally {
      setLoading(false);
    }
  };

  const getBranchColor = (branch: string) => {
    if (branch === 'main') return 'blue';
    if (branch.startsWith('feature/')) return 'green';
    if (branch.startsWith('fix/')) return 'orange';
    if (branch.startsWith('hotfix/')) return 'red';
    return 'default';
  };

  return (
    <Space>
      <BranchesOutlined />
      
      <Select
        value={currentBranch}
        onChange={onBranchChange}
        loading={loading}
        style={{ minWidth: 150 }}
        placeholder="Выберите ветку"
      >
        {branches.map(branch => (
          <Select.Option key={branch} value={branch}>
            <Tag color={getBranchColor(branch)} style={{ margin: 0 }}>
              {branch}
            </Tag>
          </Select.Option>
        ))}
      </Select>

      <Button
        type="primary"
        icon={<PlusOutlined />}
        onClick={() => setCreateModalVisible(true)}
        size="small"
      >
        Новая ветка
      </Button>

      {currentBranch !== 'main' && (
        <>
          <Button
            icon={<MergeOutlined />}
            onClick={handleMergeBranch}
            loading={loading}
            size="small"
          >
            Слить с main
          </Button>

          <Popconfirm
            title="Удалить ветку?"
            description={`Вы уверены, что хотите удалить ветку "${currentBranch}"?`}
            onConfirm={handleDeleteBranch}
            okText="Да"
            cancelText="Нет"
          >
            <Button
              danger
              icon={<DeleteOutlined />}
              size="small"
            >
              Удалить
            </Button>
          </Popconfirm>
        </>
      )}

      <Button
        icon={<HistoryOutlined />}
        onClick={showHistory}
        size="small"
      >
        История
      </Button>

      {/* Модальное окно создания ветки */}
      <Modal
        title="Создать новую ветку"
        open={createModalVisible}
        onOk={handleCreateBranch}
        onCancel={() => setCreateModalVisible(false)}
        confirmLoading={loading}
      >
        <Space direction="vertical" style={{ width: '100%' }}>
          <div>
            <label>Название ветки:</label>
            <Input
              value={newBranchName}
              onChange={(e) => setNewBranchName(e.target.value)}
              placeholder="feature/new-feature"
              onPressEnter={handleCreateBranch}
            />
          </div>
          
          <div>
            <label>Создать из ветки:</label>
            <Select
              value={sourceBranch}
              onChange={setSourceBranch}
              style={{ width: '100%' }}
            >
              {branches.map(branch => (
                <Select.Option key={branch} value={branch}>
                  {branch}
                </Select.Option>
              ))}
            </Select>
          </div>
        </Space>
      </Modal>

      {/* Модальное окно истории */}
      <Modal
        title="История изменений"
        open={historyModalVisible}
        onCancel={() => setHistoryModalVisible(false)}
        footer={null}
        width={600}
      >
        <div style={{ maxHeight: 400, overflowY: 'auto' }}>
          {history.length === 0 ? (
            <div style={{ textAlign: 'center', color: '#999' }}>
              История пуста
            </div>
          ) : (
            history.map((entry, index) => (
              <div key={index} style={{ marginBottom: 12, padding: 8, border: '1px solid #f0f0f0', borderRadius: 4 }}>
                <div style={{ fontWeight: 'bold' }}>
                  <Tag color={entry.action === 'CREATE_BRANCH' ? 'green' : entry.action === 'MERGE_BRANCH' ? 'blue' : 'default'}>
                    {entry.action}
                  </Tag>
                  {entry.branch}
                </div>
                <div style={{ color: '#666', fontSize: '12px' }}>
                  {entry.author} • {new Date(entry.timestamp).toLocaleString()}
                </div>
                <div>{entry.message}</div>
              </div>
            ))
          )}
        </div>
      </Modal>
    </Space>
  );
};

export default BranchSelector;
