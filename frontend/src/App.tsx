import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { ConfigProvider, App as AntApp } from 'antd';
import Layout from './components/Layout/Layout';
import Dashboard from './pages/Dashboard/Dashboard';
import Scenarios from './pages/Scenarios/Scenarios';
import Testing from './pages/Testing/Testing';
import Dialogs from './pages/Dialogs/Dialogs';
import NLU from './pages/NLU/NLU';
import Users from './pages/Users/Users';
import Logs from './pages/Logs/Logs';

const App: React.FC = () => {
  return (
    <ConfigProvider
      theme={{
        token: {
          colorPrimary: '#52c41a',
        },
      }}
    >
      <AntApp>
        <Router>
          <Layout>
            <Routes>
              <Route path="/" element={<Navigate to="/dashboard" replace />} />
              <Route path="/dashboard" element={<Dashboard />} />
              <Route path="/scenarios" element={<Scenarios />} />
              <Route path="/testing" element={<Testing />} />
              <Route path="/dialogs" element={<Dialogs />} />
              <Route path="/nlu" element={<NLU />} />
              <Route path="/users" element={<Users />} />
              <Route path="/logs" element={<Logs />} />
            </Routes>
          </Layout>
        </Router>
      </AntApp>
    </ConfigProvider>
  );
};

export default App;
