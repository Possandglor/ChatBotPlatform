import { apiService } from './api';

export interface ScenarioNode {
  id: string;
  type: 'message' | 'input' | 'condition' | 'api_call' | 'transfer' | 'end' | 'scenario_jump' | 'menu';
  content?: string;
  variable?: string;
  condition?: string;
  api_url?: string;
  api_method?: string;
  target_scenario?: string;
  options?: Array<{ text: string; value: string; next_node?: string }>;
  next_node?: string;
  true_node?: string;
  false_node?: string;
}

export interface Scenario {
  id?: string;
  name: string;
  description: string;
  trigger_intents: string[];
  nodes: ScenarioNode[];
  created_at?: string;
  updated_at?: string;
}

class ScenarioService {
  async getScenarios(): Promise<Scenario[]> {
    try {
      const response = await apiService.get('/scenarios');
      return response.scenarios || [];
    } catch (error) {
      console.error('Error fetching scenarios:', error);
      return [];
    }
  }

  async getScenario(id: string): Promise<Scenario | null> {
    try {
      const response = await apiService.get(`/scenarios/${id}`);
      return response;
    } catch (error) {
      console.error('Error fetching scenario:', error);
      return null;
    }
  }

  async createScenario(scenario: Scenario): Promise<Scenario> {
    const response = await apiService.post('/scenarios', scenario);
    return response;
  }

  async updateScenario(id: string, scenario: Scenario): Promise<Scenario> {
    const response = await apiService.put(`/scenarios/${id}`, scenario);
    return response;
  }

  async deleteScenario(id: string): Promise<void> {
    await apiService.delete(`/scenarios/${id}`);
  }
}

export const scenarioService = new ScenarioService();
