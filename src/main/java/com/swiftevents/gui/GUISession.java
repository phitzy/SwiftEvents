package com.swiftevents.gui;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GUISession {
    private final UUID playerId;
    private EventFilter currentFilter;
    private EventSort currentSort;
    private int currentPage;
    private long lastRefresh;
    
    // For event creation wizard
    private EventCreationStep creationStep;
    private Map<String, Object> creationData;
    
    public GUISession(UUID playerId) {
        this.playerId = playerId;
        this.currentFilter = EventFilter.ALL;
        this.currentSort = EventSort.NAME;
        this.currentPage = 0;
        this.lastRefresh = System.currentTimeMillis();
        this.creationStep = EventCreationStep.TYPE_SELECTION;
        this.creationData = new HashMap<>();
    }
    
    // Getters and setters
    public UUID getPlayerId() {
        return playerId;
    }
    
    public EventFilter getCurrentFilter() {
        return currentFilter;
    }
    
    public void setCurrentFilter(EventFilter currentFilter) {
        this.currentFilter = currentFilter;
        this.currentPage = 0; // Reset to first page when filter changes
        updateRefreshTime();
    }
    
    public EventSort getCurrentSort() {
        return currentSort;
    }
    
    public void setCurrentSort(EventSort currentSort) {
        this.currentSort = currentSort;
        updateRefreshTime();
    }
    
    public int getCurrentPage() {
        return currentPage;
    }
    
    public void setCurrentPage(int currentPage) {
        this.currentPage = Math.max(0, currentPage);
        updateRefreshTime();
    }
    
    public long getLastRefresh() {
        return lastRefresh;
    }
    
    private void updateRefreshTime() {
        this.lastRefresh = System.currentTimeMillis();
    }
    
    // Event creation wizard state
    public EventCreationStep getCreationStep() {
        return creationStep;
    }
    
    public void setCreationStep(EventCreationStep creationStep) {
        this.creationStep = creationStep;
        updateRefreshTime();
    }
    
    public Map<String, Object> getCreationData() {
        return creationData;
    }
    
    public void setCreationData(String key, Object value) {
        this.creationData.put(key, value);
        updateRefreshTime();
    }
    
    public Object getCreationData(String key) {
        return creationData.get(key);
    }
    
    public void clearCreationData() {
        this.creationData.clear();
        this.creationStep = EventCreationStep.TYPE_SELECTION;
        updateRefreshTime();
    }
    
    // Helper methods
    public boolean needsRefresh(long interval) {
        return System.currentTimeMillis() - lastRefresh > interval;
    }
    
    public void refresh() {
        updateRefreshTime();
    }
    
    // Enums for GUI navigation
    public enum EventCreationStep {
        TYPE_SELECTION, BASIC_INFO, SETTINGS, LOCATION, REWARDS, CONFIRMATION
    }
} 