import React, { useState, useEffect } from 'react';
import { Select, Button, Modal, Input, message, Space, Popconfirm, Tag } from 'antd';
import { BranchesOutlined, PlusOutlined, MergeOutlined, DeleteOutlined } from '@ant-design/icons';
import branchService from '../services/branchService';
import { useBranchStore } from '../store/branchStore';

const GlobalBranchSelector: React.FC = () => {
  const { currentBranch, branches, setCurrentBranch, setBranches } = useBranchStore();
  const [loading, setLoading] = useState(false);
  const [createModalVisible, setCreateModalVisible] = useState(false);
  const [newBranchName, setNewBranchName] = useState('');
  const [sourceBranch, setSourceBranch] = useState('main');

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

  const handleBranchChange = (branchName: string) => {
    setCurrentBranch(branchName);
    message.success(`Переключено на ветку: ${branchName}`);
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
      
      // Переключаемся на новую ветку
      setCurrentBranch(newBranchName);
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
        setCurrentBranch('main');
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
      
      setCurrentBranch('main');
      await loadBranches();
    } catch (error) {
      console.error('Error deleting branch:', error);
      message.error('Ошибка удаления ветки');
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
        onChange={handleBranchChange}
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
    </Space>
  );
};

export default GlobalBranchSelector;
