package com.pb.chatbot.scenario.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

public class WorkspaceWithBranches {
    
    @JsonProperty("branches")
    public Map<String, WorkspaceBranch> branches = new HashMap<>();
    
    @JsonProperty("history")
    public List<BranchHistoryEntry> history = new ArrayList<>();
    
    public WorkspaceWithBranches() {}
    
    public WorkspaceBranch getBranch(String branchName) {
        return branches.get(branchName != null ? branchName : "main");
    }
    
    public void addBranch(String branchName, WorkspaceBranch branch) {
        branches.put(branchName, branch);
        addHistoryEntry("CREATE_BRANCH", branchName, branch.author, "Created branch: " + branchName);
    }
    
    public void updateBranch(String branchName, WorkspaceBranch branch) {
        branches.put(branchName, branch);
        addHistoryEntry("UPDATE_BRANCH", branchName, branch.author, "Updated branch: " + branchName);
    }
    
    public void mergeBranch(String sourceBranch, String targetBranch, String author) {
        addHistoryEntry("MERGE_BRANCH", sourceBranch + " -> " + targetBranch, author, 
                       "Merged " + sourceBranch + " into " + targetBranch);
    }
    
    public void addHistoryEntry(String action, String branch, String author, String message) {
        history.add(new BranchHistoryEntry(action, branch, author, message));
    }
}
