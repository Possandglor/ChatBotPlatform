import { create } from 'zustand';

interface BranchState {
  currentBranch: string;
  branches: string[];
  setCurrentBranch: (branch: string) => void;
  setBranches: (branches: string[]) => void;
}

export const useBranchStore = create<BranchState>((set) => ({
  currentBranch: 'main',
  branches: ['main'],
  setCurrentBranch: (branch: string) => set({ currentBranch: branch }),
  setBranches: (branches: string[]) => set({ branches }),
}));
