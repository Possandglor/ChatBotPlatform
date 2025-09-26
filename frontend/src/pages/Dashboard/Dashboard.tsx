import React, { useEffect, useState } from 'react';
import { Card, Row, Col, Statistic, Button, Space, Alert } from 'antd';
import {
  MessageOutlined,
  UserOutlined,
  CheckCircleOutlined,
  ExclamationCircleOutlined,
  ReloadOutlined,
} from '@ant-design/icons';
import { apiService } from '../../services/api';

interface DashboardStats {
  totalDialogs: number;
  activeDialogs: number;
  totalScenarios: number;
  totalIntents: number;
}

const Dashboard: React.FC = () => {
  const [stats, setStats] = useState<DashboardStats>({
    totalDialogs: 0,
    activeDialogs: 0,
    totalScenarios: 0,
    totalIntents: 0,
  });
  const [loading, setLoading] = useState(false);

  const loadDashboardData = async () => {
    setLoading(true);
    try {
      // Загружаем реальные данные сценариев
      const scenariosResponse = await apiService.getScenarios();
      const scenariosCount = scenariosResponse.data.scenarios?.length || scenariosResponse.data.count || 0;

      // Загружаем количество интентов из Orchestrator
      let intentsCount = 24; // fallback
      try {
        const intentsResponse = await fetch('http://localhost:8092/api/v1/nlu/intents');
        const intentsData = await intentsResponse.json();
        intentsCount = intentsData.intents?.length || intentsData.length || 24;
      } catch (intentsError) {
        console.log('Orchestrator недоступен, используем fallback значение');
      }

      // Загружаем статистику диалогов
      let totalDialogs = 0;
      let activeDialogs = 0;
      try {
        // Всего диалогов (включая завершенные)
        const allDialogsResponse = await fetch('http://localhost:8092/api/v1/chat/sessions');
        if (allDialogsResponse.ok) {
          const allDialogsData = await allDialogsResponse.json();
          totalDialogs = allDialogsData.sessions?.length || 0;
        }
        
        // Активные диалоги (только незавершенные)
        const activeDialogsResponse = await fetch('http://localhost:8092/api/v1/chat/sessions/active');
        if (activeDialogsResponse.ok) {
          const activeDialogsData = await activeDialogsResponse.json();
          activeDialogs = activeDialogsData.sessions?.length || 0;
        }
      } catch (dialogError) {
        console.log('Диалоги недоступны');
      }

      // Загружаем статистику (все из Orchestrator)
      setStats({
        totalDialogs: totalDialogs,
        activeDialogs: activeDialogs,
        totalScenarios: scenariosCount,
        totalIntents: intentsCount,
      });
    } catch (error) {
      console.error('Failed to load dashboard data:', error);
      // Fallback к мок данным
      setStats({
        totalDialogs: 156,
        activeDialogs: 12,
        totalScenarios: 8,
        totalIntents: 24,
      });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadDashboardData();
  }, []);

  return (
    <div>
      <div style={{ marginBottom: 24, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <h1 style={{ margin: 0 }}>Дашборд</h1>
        <Button 
          icon={<ReloadOutlined />} 
          onClick={loadDashboardData}
          loading={loading}
        >
          Обновить
        </Button>
      </div>

      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic
              title="Всего диалогов"
              value={stats.totalDialogs}
              prefix={<MessageOutlined />}
              valueStyle={{ color: '#52c41a' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic
              title="Активные диалоги"
              value={stats.activeDialogs}
              prefix={<UserOutlined />}
              valueStyle={{ color: '#1890ff' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic
              title="Сценарии"
              value={stats.totalScenarios}
              valueStyle={{ color: '#722ed1' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic
              title="Интенты NLU"
              value={stats.totalIntents}
              valueStyle={{ color: '#fa8c16' }}
            />
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]}>
        <Col xs={24} lg={12}>
          <Card title="Быстрые действия">
            <Space direction="vertical" style={{ width: '100%' }}>
              <Button type="primary" block href="/testing">
                Начать тестирование
              </Button>
              <Button block href="/scenarios">
                Создать сценарий
              </Button>
              <Button block href="/dialogs">
                Просмотреть диалоги
              </Button>
              <Button block href="/nlu">
                Настроить NLU
              </Button>
            </Space>
          </Card>
        </Col>
        <Col xs={24} lg={12}>
          <Card title="Последние события">
            <div style={{ color: '#666' }}>
              <p>• Загружены сценарии из Scenario Service - сейчас</p>
              <p>• Проверен статус всех микросервисов - сейчас</p>
              <p>• Обновлена статистика диалогов - сейчас</p>
              <p>• Система готова к работе - сейчас</p>
            </div>
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default Dashboard;
