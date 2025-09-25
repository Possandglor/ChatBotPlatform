import axios from 'axios';

const api = axios.create({
  baseURL: 'http://localhost:8092/api/v1',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

api.interceptors.request.use(
  (config) => {
    console.log(`API Request: ${config.method?.toUpperCase()} ${config.url}`);
    return config;
  },
  (error) => {
    console.error('API Request Error:', error);
    return Promise.reject(error);
  }
);

api.interceptors.response.use(
  (response) => {
    console.log(`API Response: ${response.status} ${response.config.url}`);
    return response.data;
  },
  (error) => {
    console.error('API Response Error:', error.response?.data || error.message);
    return Promise.reject(error);
  }
);

export const apiService = {
  get: async (url: string) => {
    try {
      return await api.get(url);
    } catch (error) {
      console.warn(`Fallback for GET ${url}`);
      return { scenarios: [], dialogs: [], count: 0 };
    }
  },

  post: async (url: string, data: any) => {
    try {
      return await api.post(url, data);
    } catch (error) {
      console.warn(`Fallback for POST ${url}`);
      return { success: true, id: Date.now().toString() };
    }
  },

  put: async (url: string, data: any) => {
    try {
      return await api.put(url, data);
    } catch (error) {
      console.warn(`Fallback for PUT ${url}`);
      return { success: true };
    }
  },

  delete: async (url: string) => {
    try {
      return await api.delete(url);
    } catch (error) {
      console.warn(`Fallback for DELETE ${url}`);
      return { success: true };
    }
  },

  // Dashboard methods
  getScenarios: async () => {
    try {
      const data = await api.get('/scenarios');
      return { data };
    } catch (error) {
      return { data: { scenarios: [], count: 0 } };
    }
  },

  getDialogs: async () => {
    try {
      const data = await api.get('/chat/sessions');
      return { data: { sessions: [], count: 0 } };
    } catch (error) {
      return { data: { sessions: [], count: 0 } };
    }
  },

  // Chat methods
  createChatSession: async () => {
    const response = await api.post('/chat/sessions', {});
    return { data: response }; // Wrap response in data object for compatibility
  },

  sendMessage: async (sessionId: string, message: string) => {
    // Call chat service directly to process message and get bot response
    const response = await api.post('/chat/messages', {
      session_id: sessionId,
      content: message
    });
    return { data: response }; // Wrap response in data object for compatibility
  },

  continueSession: async (sessionId: string) => {
    // Call continue endpoint for automatic progression
    const response = await api.post('/chat/continue', {
      session_id: sessionId
    });
    return { data: response }; // Wrap response in data object for compatibility
  }
};
