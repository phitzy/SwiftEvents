package com.swiftevents.api.hooks;

import com.swiftevents.SwiftEventsPlugin;
import com.swiftevents.events.Event;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages integration hooks for other plugins
 */
public class HookManager {
    
    private final SwiftEventsPlugin plugin;
    private final Map<String, SwiftEventsHook> hooks;
    private final List<SwiftEventsHook> sortedHooks;
    
    public HookManager(SwiftEventsPlugin plugin) {
        this.plugin = plugin;
        this.hooks = new ConcurrentHashMap<>();
        this.sortedHooks = new ArrayList<>();
    }
    
    /**
     * Register a new integration hook
     * @param hook The hook to register
     * @return True if registered successfully, false if a hook with this name already exists
     */
    public boolean registerHook(SwiftEventsHook hook) {
        if (hooks.containsKey(hook.getHookName())) {
            plugin.getLogger().warning("Hook with name '" + hook.getHookName() + "' is already registered!");
            return false;
        }
        
        hooks.put(hook.getHookName(), hook);
        rebuildSortedHooks();
        
        plugin.getLogger().info("Registered integration hook: " + hook.getHookName());
        return true;
    }
    
    /**
     * Unregister an integration hook
     * @param hookName The name of the hook to unregister
     * @return True if unregistered successfully, false if hook doesn't exist
     */
    public boolean unregisterHook(String hookName) {
        SwiftEventsHook removed = hooks.remove(hookName);
        if (removed != null) {
            rebuildSortedHooks();
            plugin.getLogger().info("Unregistered integration hook: " + hookName);
            return true;
        }
        return false;
    }
    
    /**
     * Get a hook by name
     * @param hookName The hook name
     * @return The hook or null if not found
     */
    public SwiftEventsHook getHook(String hookName) {
        return hooks.get(hookName);
    }
    
    /**
     * Get all registered hooks
     * @return A copy of all registered hooks
     */
    public Collection<SwiftEventsHook> getAllHooks() {
        return new ArrayList<>(hooks.values());
    }
    
    /**
     * Get all hook names
     * @return Set of all hook names
     */
    public Set<String> getHookNames() {
        return new HashSet<>(hooks.keySet());
    }
    
    private void rebuildSortedHooks() {
        sortedHooks.clear();
        sortedHooks.addAll(hooks.values());
        sortedHooks.sort(Comparator.comparingInt(SwiftEventsHook::getPriority));
    }
    
    // Hook callback methods
    
    public boolean callEventPreCreate(Event event) {
        for (SwiftEventsHook hook : sortedHooks) {
            try {
                if (!hook.onEventPreCreate(event)) {
                    return false;
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Error in hook " + hook.getHookName() + " onEventPreCreate: " + e.getMessage());
            }
        }
        return true;
    }
    
    public void callEventCreated(Event event) {
        for (SwiftEventsHook hook : sortedHooks) {
            try {
                hook.onEventCreated(event);
            } catch (Exception e) {
                plugin.getLogger().severe("Error in hook " + hook.getHookName() + " onEventCreated: " + e.getMessage());
            }
        }
    }
    
    public boolean callEventPreStart(Event event) {
        for (SwiftEventsHook hook : sortedHooks) {
            try {
                if (!hook.onEventPreStart(event)) {
                    return false;
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Error in hook " + hook.getHookName() + " onEventPreStart: " + e.getMessage());
            }
        }
        return true;
    }
    
    public void callEventStarted(Event event) {
        for (SwiftEventsHook hook : sortedHooks) {
            try {
                hook.onEventStarted(event);
            } catch (Exception e) {
                plugin.getLogger().severe("Error in hook " + hook.getHookName() + " onEventStarted: " + e.getMessage());
            }
        }
    }
    
    public void callEventEnded(Event event, String reason) {
        for (SwiftEventsHook hook : sortedHooks) {
            try {
                hook.onEventEnded(event, reason);
            } catch (Exception e) {
                plugin.getLogger().severe("Error in hook " + hook.getHookName() + " onEventEnded: " + e.getMessage());
            }
        }
    }
    
    public boolean callPlayerPreJoin(Player player, Event event) {
        for (SwiftEventsHook hook : sortedHooks) {
            try {
                if (!hook.onPlayerPreJoin(player, event)) {
                    return false;
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Error in hook " + hook.getHookName() + " onPlayerPreJoin: " + e.getMessage());
            }
        }
        return true;
    }
    
    public void callPlayerJoined(Player player, Event event) {
        for (SwiftEventsHook hook : sortedHooks) {
            try {
                hook.onPlayerJoined(player, event);
            } catch (Exception e) {
                plugin.getLogger().severe("Error in hook " + hook.getHookName() + " onPlayerJoined: " + e.getMessage());
            }
        }
    }
    
    public void callPlayerLeft(Player player, UUID playerId, Event event, String reason) {
        for (SwiftEventsHook hook : sortedHooks) {
            try {
                hook.onPlayerLeft(player, playerId, event, reason);
            } catch (Exception e) {
                plugin.getLogger().severe("Error in hook " + hook.getHookName() + " onPlayerLeft: " + e.getMessage());
            }
        }
    }
    
    public void callEventUpdate(Event event) {
        for (SwiftEventsHook hook : sortedHooks) {
            try {
                hook.onEventUpdate(event);
            } catch (Exception e) {
                plugin.getLogger().severe("Error in hook " + hook.getHookName() + " onEventUpdate: " + e.getMessage());
            }
        }
    }
    
    public void callPluginDisable() {
        for (SwiftEventsHook hook : sortedHooks) {
            try {
                hook.onPluginDisable();
            } catch (Exception e) {
                plugin.getLogger().severe("Error in hook " + hook.getHookName() + " onPluginDisable: " + e.getMessage());
            }
        }
    }
} 