package com.swiftevents;

import com.swiftevents.api.GuiAPI;
import com.swiftevents.api.GuiAPIImpl;
import com.swiftevents.api.ChatAPI;
import com.swiftevents.api.ChatAPIImpl;
import com.swiftevents.api.LocationAPI;
import com.swiftevents.api.LocationAPIImpl;
import com.swiftevents.api.HudAPI;
import com.swiftevents.api.HudAPIImpl;
import com.swiftevents.api.TaskerAPI;
import com.swiftevents.api.TaskerAPIImpl;
import com.swiftevents.api.SwiftEventsAPI;
import com.swiftevents.api.hooks.HookManager;
import com.swiftevents.commands.SwiftEventCommand;
import com.swiftevents.config.ConfigManager;
import com.swiftevents.database.DatabaseManager;
import com.swiftevents.events.EventManager;
import com.swiftevents.gui.GUIManager;
import com.swiftevents.gui.AdminGUIManager;
import com.swiftevents.gui.StatisticsGUIManager;
import com.swiftevents.hud.HUDManager;
import com.swiftevents.listeners.PlayerListener;
import com.swiftevents.locations.LocationManager;
import com.swiftevents.tasker.EventTasker;
import com.swiftevents.chat.ChatManager;
import org.bukkit.plugin.java.JavaPlugin;

public class SwiftEventsPlugin extends JavaPlugin {
    
    private static SwiftEventsPlugin instance;
    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private EventManager eventManager;
    private GUIManager guiManager;
    private AdminGUIManager adminGUIManager;
    private StatisticsGUIManager statisticsGUIManager;
    private HUDManager hudManager;
    private GuiAPI guiAPI;
    private ChatAPI chatAPI;
    private LocationAPI locationAPI;
    private HudAPI hudAPI;
    private TaskerAPI taskerAPI;
    private EventTasker eventTasker;
    private HookManager hookManager;
    private ChatManager chatManager;
    private LocationManager locationManager;
    
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
        
        // Initialize location manager
        locationManager = new LocationManager(this);
        
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
        adminGUIManager = new AdminGUIManager(this, guiManager);
        statisticsGUIManager = new StatisticsGUIManager(this);
        hudManager = new HUDManager(this);
        
        // Initialize API
        guiAPI = new GuiAPIImpl(this);
        chatAPI = new ChatAPIImpl(this);
        locationAPI = new LocationAPIImpl(this);
        hudAPI = new HudAPIImpl(this);
        taskerAPI = new TaskerAPIImpl(this);
        
        // Initialize Event Tasker
        eventTasker = new EventTasker(this);
        if (configManager.isEventTaskerEnabled()) {
            eventTasker.start();
        }
        
        // Initialize Chat Manager
        chatManager = new ChatManager(this);
    }
    
    private void registerCommands() {
        SwiftEventCommand swiftEventCommand = new SwiftEventCommand(this);
        getCommand("swiftevent").setExecutor(swiftEventCommand);
        getCommand("swiftevent").setTabCompleter(swiftEventCommand);
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
    
    public AdminGUIManager getAdminGUIManager() {
        return adminGUIManager;
    }
    
    public StatisticsGUIManager getStatisticsGUIManager() {
        return statisticsGUIManager;
    }
    
    public HUDManager getHUDManager() {
        return hudManager;
    }
    
    public GuiAPI getGuiAPI() {
        return guiAPI;
    }
    
    public ChatAPI getChatAPI() {
        return chatAPI;
    }
    
    public LocationAPI getLocationAPI() {
        return locationAPI;
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

    public LocationManager getLocationManager() {
        return locationManager;
    }

    public HudAPI getHudAPI() {
        return hudAPI;
    }

    public TaskerAPI getTaskerAPI() {
        return taskerAPI;
    }
} 