package com.swiftevents;

import com.swiftevents.api.EventAPI;
import com.swiftevents.api.SwiftEventsAPI;
import com.swiftevents.api.hooks.HookManager;
import com.swiftevents.commands.EventCommand;
import com.swiftevents.commands.EventAdminCommand;
import com.swiftevents.commands.EventGUICommand;
import com.swiftevents.config.ConfigManager;
import com.swiftevents.database.DatabaseManager;
import com.swiftevents.events.EventManager;
import com.swiftevents.gui.GUIManager;
import com.swiftevents.hud.HUDManager;
import com.swiftevents.listeners.PlayerListener;
import com.swiftevents.tasker.EventTasker;
import com.swiftevents.chat.ChatManager;
import org.bukkit.plugin.java.JavaPlugin;

public class SwiftEventsPlugin extends JavaPlugin {
    
    private static SwiftEventsPlugin instance;
    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private EventManager eventManager;
    private GUIManager guiManager;
    private HUDManager hudManager;
    private EventAPI eventAPI;
    private EventTasker eventTasker;
    private HookManager hookManager;
    private ChatManager chatManager;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // Initialize components
        initializePlugin();
        
        // Register commands
        registerCommands();
        
        // Register listeners
        registerListeners();
        
        // Initialize static API
        SwiftEventsAPI.initialize(this);
        
        getLogger().info("SwiftEvents plugin has been enabled!");
    }
    
    @Override
    public void onDisable() {
        // Save any pending data
        if (eventManager != null) {
            eventManager.saveAllEvents();
        }
        
        // Stop event tasker
        if (eventTasker != null) {
            eventTasker.stop();
        }
        
        // Notify hooks of plugin disable
        if (hookManager != null) {
            hookManager.callPluginDisable();
        }
        
        // Close database connections
        if (databaseManager != null) {
            databaseManager.closeConnections();
        }
        
        // Shutdown chat manager
        if (chatManager != null) {
            chatManager.shutdown();
        }
        
        getLogger().info("SwiftEvents plugin has been disabled!");
    }
    
    private void initializePlugin() {
        // Initialize configuration
        configManager = new ConfigManager(this);
        configManager.loadConfig();
        
        // Load and validate messages
        if (!configManager.loadMessages()) {
            getLogger().warning("Some message keys may be missing. Plugin will continue with defaults.");
        }
        
        // Initialize database
        databaseManager = new DatabaseManager(this);
        databaseManager.initialize();
        
        // Initialize hook manager first (other components may need it)
        hookManager = new HookManager(this);
        
        // Initialize managers
        eventManager = new EventManager(this);
        guiManager = new GUIManager(this);
        hudManager = new HUDManager(this);
        
        // Initialize API
        eventAPI = new EventAPI(this);
        
        // Initialize Event Tasker
        eventTasker = new EventTasker(this);
        if (configManager.isEventTaskerEnabled()) {
            eventTasker.start();
        }
        
        // Initialize Chat Manager
        chatManager = new ChatManager(this);
    }
    
    private void registerCommands() {
        getCommand("event").setExecutor(new EventCommand(this));
        getCommand("eventadmin").setExecutor(new EventAdminCommand(this));
        getCommand("eventgui").setExecutor(new EventGUICommand(this));
    }
    
    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
    }
    
    // Getters for other classes to access managers
    public static SwiftEventsPlugin getInstance() {
        return instance;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
    
    public EventManager getEventManager() {
        return eventManager;
    }
    
    public GUIManager getGUIManager() {
        return guiManager;
    }
    
    public HUDManager getHUDManager() {
        return hudManager;
    }
    
    public EventAPI getEventAPI() {
        return eventAPI;
    }
    
    public EventTasker getEventTasker() {
        return eventTasker;
    }
    
    public HookManager getHookManager() {
        return hookManager;
    }
    
    public ChatManager getChatManager() {
        return chatManager;
    }
} 