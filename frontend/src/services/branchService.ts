import { apiService } from './api';

export interface Branch {
  branchName: string;
  scenarioData: any;
  baseCommit: string;
  lastModified: string;
  author: string;
  isDeleted: boolean;
  commitMessage?: string;
}

export interface MergeResult {
  success: boolean;
  message: string;
  conflicts?: string[];
}

export interface HistoryEntry {
  action: string;
  branch: string;
  author: string;
  message: string;
  timestamp: string;
}

class BranchService {
  
  /**
   * Создать новую ветку
   */
  async createBranch(branchName: string, sourceBranch = 'main', author = 'anonymous'): Promise<Branch> {
    const encodedBranchName = encodeURIComponent(branchName);
    const response = await apiService.post(
      `/branches/${encodedBranchName}?from=${sourceBranch}&author=${author}`,
      {}
    );
    return response.data;
  }

  /**
   * Получить список веток
   */
  async getBranches(): Promise<string[]> {
    const response = await apiService.get(`/branches`);
    return response.data?.branches || response.branches || [];
  }

  /**
   * Получить конкретную ветку
   */
  async getBranch(branchName: string): Promise<Branch | null> {
    try {
      const encodedBranchName = encodeURIComponent(branchName);
      const response = await apiService.get(`/branches/${encodedBranchName}`);
      return response.data;
    } catch (error) {
      console.error('Error getting branch:', error);
      return null;
    }
  }

  /**
   * Слить ветку
   */
  async mergeBranch(sourceBranch: string, targetBranch = 'main', author = 'anonymous'): Promise<MergeResult> {
    const encodedSourceBranch = encodeURIComponent(sourceBranch);
    const response = await apiService.post(
      `/branches/${encodedSourceBranch}/merge?target=${targetBranch}&author=${author}`,
      {}
    );
    return response.data;
  }

  /**
   * Удалить ветку
   */
  async deleteBranch(branchName: string): Promise<void> {
    const encodedBranchName = encodeURIComponent(branchName);
    await apiService.delete(`/branches/${encodedBranchName}`);
  }

  /**
   * Получить историю изменений
   */
  async getHistory(): Promise<HistoryEntry[]> {
    const response = await apiService.get(`/branches/history`);
    return response.data.history || [];
  }
}

export default new BranchService();
