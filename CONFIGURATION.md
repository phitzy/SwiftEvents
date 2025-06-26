# SwiftEvents Configuration Guide

This guide covers all configuration options and new features available in SwiftEvents v1.0.0+.

## Table of Contents

- [Overview](#overview)
- [Configuration Files](#configuration-files)
- [Database Configuration](#database-configuration)
- [Event System](#event-system)
- [Permission System](#permission-system)
- [Localization](#localization)
- [Advanced Features](#advanced-features)
- [Runtime Configuration](#runtime-configuration)
- [Performance Tuning](#performance-tuning)
- [Integration Settings](#integration-settings)
- [Troubleshooting](#troubleshooting)

## Overview

SwiftEvents offers extensive configurability to meet the needs of any server. All settings are validated on startup and can be modified at runtime through commands.

## Configuration Files

### Main Configuration (`config.yml`)

The primary configuration file containing all plugin settings:

```yaml
# Core systems
database:           # Database connection settings
json:              # JSON storage settings (fallback)
gui:               # GUI interface settings
hud:               # HUD notification settings  
chat:              # Chat system settings
events:            # Event system limits and behavior
event_tasker:      # Automatic event scheduling
permission_groups: # Group-based permissions and limits
localization:      # Language and localization
advanced:          # Performance and debugging
integrations:      # Third-party plugin integration
messages:          # User-facing messages
```

### Language Files (`messages_*.yml`)

Separate language files for localization support:
- `messages_en.yml` - English (default)
- `messages_es.yml` - Spanish
- `messages_fr.yml` - French
- `messages_de.yml` - German

## Database Configuration

### MySQL Database

For high-performance servers with many events:

```yaml
database:
  enabled: true
  type: mysql
  host: localhost
  port: 3306
  name: swiftevents
  username: root
  password: password
  # Advanced settings
  connection_timeout: 30        # Connection timeout in seconds
  max_connections: 10           # Maximum connection pool size
  connection_validation_timeout: 5
```

### JSON Storage

For smaller servers or easy setup:

```yaml
database:
  enabled: false

json:
  folder: events
  auto_backup: true            # Enable automatic backups
  backup_interval: 3600        # Backup every hour
  max_backups: 5              # Keep 5 most recent backups
```

## Event System

### Core Event Settings

```yaml
events:
  max_concurrent: 5           # Maximum simultaneous events
  auto_save_interval: 300     # Auto-save every 5 minutes
  player_cooldown: 300        # Cooldown between joins (seconds)
  max_events_per_player: 3    # Max events per player
  track_statistics: true      # Enable player statistics
  auto_cancel_empty_after: 10 # Cancel empty events after 10 minutes
```

### Event Tasker (Automatic Events)

```yaml
event_tasker:
  enabled: false
  check_interval: 60          # Check every minute
  min_event_interval: 30      # Minimum 30 minutes between events
  max_event_interval: 120     # Maximum 2 hours between events
  announce_upcoming: true
  announce_time: 5            # Announce 5 minutes before start
  
  presets:
    pvp_tournament:
      name: "PvP Tournament #{number}"
      description: "Automated PvP tournament"
      type: PVP
      duration: 1800           # 30 minutes
      max_participants: 20
      min_participants: 4
      rewards:
        - "give {winner} diamond_sword 1"
        - "give {winner} golden_apple 5"
      enabled: true
      weight: 10               # Selection probability
      required_permission: "swiftevents.event.pvp"
```

## Permission System

### Granular Permissions

SwiftEvents uses a hierarchical permission system:

```yaml
# Root permissions
swiftevents.*                 # All permissions
swiftevents.admin.*          # All admin permissions
swiftevents.user.*           # All user permissions
swiftevents.event.*          # All event type permissions
swiftevents.bypass.*         # All bypass permissions

# User permissions
swiftevents.user.join        # Join events
swiftevents.user.leave       # Leave events
swiftevents.user.list        # List events
swiftevents.user.info        # View event info
swiftevents.user.teleport    # Teleport to events
swiftevents.user.gui         # Use GUI interface

# Event-specific permissions
swiftevents.event.pvp        # Join PvP events
swiftevents.event.building   # Join building events
swiftevents.event.racing     # Join racing events
# ... etc for all event types

# Bypass permissions
swiftevents.bypass.cooldown  # Bypass join cooldowns
swiftevents.bypass.limits    # Bypass participation limits
swiftevents.bypass.full      # Join full events
```

### Permission Groups

Configure different limits for different player groups:

```yaml
permission_groups:
  auto_join_groups: ["vip", "premium", "staff"]
  event_creator_groups: ["admin", "moderator"]
  
  limits:
    default:
      max_events_per_player: 2
      event_cooldown: 300
    vip:
      max_events_per_player: 4
      event_cooldown: 180
    premium:
      max_events_per_player: 6
      event_cooldown: 120
    staff:
      max_events_per_player: -1  # Unlimited
      event_cooldown: 0
```

## Localization

### Language Configuration

```yaml
localization:
  default_language: "en"
  per_player_language: false  # Individual player languages
  languages:
    en: "English"
    es: "Español" 
    fr: "Français"
    de: "Deutsch"
```

### Message Placeholders

Common placeholders used in messages:
- `{event_name}` - Event name
- `{player}` - Player name
- `{time}` - Formatted time
- `{winner}` - Event winner
- `{participants}` - Participant count
- `{key}` - Configuration key
- `{value}` - Configuration value

## Advanced Features

### Performance Settings

```yaml
advanced:
  debug_mode: false
  metrics_enabled: true
  backup_on_shutdown: true
  
  performance:
    enable_caching: true
    cache_duration: 300      # 5 minutes
    async_operations: true   # Use async database operations
    batch_operations: true   # Batch database writes
    batch_size: 50
```

### GUI Customization

```yaml
gui:
  enabled: true
  title: "§6SwiftEvents"
  size: 54                   # Inventory size (9, 18, 27, 36, 45, 54)
  update_interval: 5         # Update every 5 seconds
  animations_enabled: true
  auto_refresh: true
```

### HUD System

```yaml
hud:
  enabled: true
  position: ACTION_BAR       # ACTION_BAR, BOSS_BAR, or TITLE
  notification_duration: 5
  animations_enabled: true
  
  colors:                    # Custom colors per event type
    PVP: "#FF5555"
    PVE: "#FFAA00"
    BUILDING: "#55FF55"
    RACING: "#5555FF"
    TREASURE_HUNT: "#FFFF55"
    MINI_GAME: "#FF55FF"
    CUSTOM: "#AAAAAA"
```

## Runtime Configuration

### Admin Commands

Use these commands to modify configuration without restarting:

```bash
# View configuration values
/swiftevent admin config get events.max_concurrent
/swiftevent admin config list

# Modify configuration
/swiftevent admin config set events.max_concurrent 10
/swiftevent admin config set hud.enabled false

# Validate configuration
/swiftevent admin config validate

# Reload configuration
/swiftevent admin reload
```

### Configuration Management

```bash
# Create backup
/swiftevent admin backup create

# View backup information
/swiftevent admin backup info

# Event tasker management
/swiftevent admin tasker start
/swiftevent admin tasker stop
/swiftevent admin tasker status
/swiftevent admin tasker force        # Force next event
/swiftevent admin tasker presets      # List presets
```

## Performance Tuning

### Database Optimization

For high-traffic servers:

```yaml
database:
  max_connections: 20
  connection_timeout: 60
  
advanced:
  performance:
    batch_size: 100           # Larger batches
    cache_duration: 600       # Longer cache time
```

### Memory Optimization

```yaml
advanced:
  performance:
    enable_caching: true      # Cache frequently accessed data
    async_operations: true    # Prevent main thread blocking
    
events:
  auto_save_interval: 600     # Less frequent saves
```

### Network Optimization

```yaml
chat:
  interactive_messages: false # Disable for lower bandwidth
  hover_tooltips: false

gui:
  animations_enabled: false   # Disable animations
  auto_refresh: false         # Manual refresh only
```

## Integration Settings

### Third-Party Plugin Support

```yaml
integrations:
  placeholderapi: true        # PlaceholderAPI integration
  vault: true                 # Vault economy integration
  
  discord:
    enabled: false
    webhook_url: ""
    announce_events: true
    
  worldguard:
    enabled: true
    respect_regions: true     # Respect WorldGuard regions
```

### Available Placeholders

When PlaceholderAPI is enabled:
- `%swiftevents_player_events%` - Events player is in
- `%swiftevents_active_events%` - Total active events
- `%swiftevents_total_events%` - Total events
- `%swiftevents_next_event%` - Next automatic event

## Troubleshooting

### Common Issues

**Configuration validation errors:**
1. Check the server console for specific error messages
2. Use `/swiftevent admin config validate` to check current settings
3. Ensure all required values are within valid ranges

**Database connection issues:**
1. Verify database credentials and host accessibility
2. Check firewall settings
3. Monitor connection pool usage with debug mode

**Performance issues:**
1. Enable metrics to monitor performance
2. Increase cache duration for better performance
3. Use database storage for large servers
4. Consider disabling animations and interactive features

### Debug Mode

Enable debug mode for detailed logging:

```yaml
advanced:
  debug_mode: true
```

This will log:
- Configuration changes
- Database operations
- Cache operations
- Performance metrics

### Configuration Validation

The plugin validates all configuration values on startup:

- Numeric values must be within specified ranges
- Required strings cannot be empty
- Database connections are tested
- Permission group settings are validated
- Language files are checked for existence

Invalid configurations will:
1. Log detailed error messages
2. Use safe default values
3. Continue plugin operation
4. Allow runtime correction

### Best Practices

1. **Start with default configuration** and modify gradually
2. **Test configuration changes** on a development server first
3. **Enable validation** to catch configuration errors early
4. **Use permission groups** to customize player experience
5. **Monitor performance** with metrics enabled
6. **Regular backups** of both configuration and event data
7. **Update gradually** - test new features in isolation

### Support

For additional help:
1. Check the GitHub repository for issues and documentation
2. Join the Discord server for community support
3. Review server logs for specific error messages
4. Use debug mode for detailed troubleshooting information

---

*This configuration guide covers SwiftEvents v1.0.0+. Features and settings may vary in different versions.* 