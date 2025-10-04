import React from 'react';
import { Layout as AntLayout, Menu, Space } from 'antd';
import { useNavigate, useLocation } from 'react-router-dom';
import GlobalBranchSelector from '../GlobalBranchSelector';

const { Header, Sider, Content } = AntLayout;

interface LayoutProps {
  children: React.ReactNode;
}

const Layout: React.FC<LayoutProps> = ({ children }) => {
  const navigate = useNavigate();
  const location = useLocation();

  const menuItems = [
    { key: '/dashboard', label: 'Дашборд' },
    { key: '/testing', label: 'Тестирование' },
    { key: '/scenarios', label: 'Сценарии' },
    { key: '/dialogs', label: 'Диалоги' },
    { key: '/nlu', label: 'NLU' },
    { key: '/logs', label: 'Логи' },
  ];

  return (
    <AntLayout style={{ minHeight: '100vh' }}>
      <Sider>
        <div style={{ 
          height: 32, 
          margin: 16, 
          background: '#52c41a',
          borderRadius: 6,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          color: 'white',
          fontWeight: 'bold',
        }}>
          ChatBot Platform
        </div>
        
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={[location.pathname]}
          items={menuItems}
          onClick={({ key }) => navigate(key)}
        />
      </Sider>
      
      <AntLayout>
        <Header style={{ background: '#fff', padding: '0 16px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <h2 style={{ margin: 0 }}>ChatBot Platform</h2>
          <GlobalBranchSelector />
        </Header>
        
        <Content style={{ margin: '16px', padding: '16px', background: '#fff' }}>
          {children}
        </Content>
      </AntLayout>
    </AntLayout>
  );
};

export default Layout;
