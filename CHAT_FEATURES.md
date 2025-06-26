# SwiftEvents Chat System Documentation

## Overview

The SwiftEvents plugin now includes a comprehensive chat system that provides beautiful, interactive event announcements using the modern Adventure API. Players can click on chat messages to instantly interact with events, including teleporting to event locations and joining events.

## Features

### 🎨 Beautiful Chat Announcements

Events now appear in chat with:
- **Color-coded messages** based on event type
- **Rich formatting** with icons and separators
- **Professional layout** with clear information hierarchy
- **Event type indicators** with custom colors:
  - 🔴 PVP Events (Red)
  - 🟠 PVE Events (Orange)  
  - 🟢 Building Events (Green)
  - 🔵 Racing Events (Blue)
  - 🟡 Treasure Hunt Events (Yellow)
  - 🟣 Mini Game Events (Magenta)
  - ⚪ Custom Events (Gray)

### ⚡ Interactive Clickable Elements

Each event announcement includes clickable buttons:

1. **[⚡ TELEPORT]** - Instantly teleport to the event location
   - Only shown if the event has a set location
   - 5-second cooldown to prevent spam
   - Smooth teleportation with visual and audio effects

2. **[📋 JOIN]** - Join the event directly from chat
   - Automatically switches to **[📋 INFO]** if event can't be joined
   - Provides immediate feedback

3. **[📤 SHARE]** - Share event with other players
   - Pre-fills a message template for easy sharing
   - Includes event join command

### 🔔 Event Announcement Types

The system announces different event states:

- **🎉 NEW EVENT CREATED** - When events are created manually or automatically
- **▶️ EVENT STARTING** - When events begin (includes title animation)
- **🏁 EVENT ENDED** - When events complete or are cancelled
- **⏰ EVENT REMINDER** - Automatic reminders before events start

### 🎵 Audio Enhancement

- **Sound effects** accompany announcements (configurable)
- **Different sounds** for different announcement types:
  - Creation: `ui.toast.challenge_complete`
  - Starting: `block.note_block.chime` 
  - Ending: `entity.experience_orb.pickup`
  - Teleporting: `entity.enderman.teleport`

### 📱 Title Notifications

Event start announcements include:
- **Bold title** saying "Event Starting!"
- **Colored subtitle** with the event name
- **Smooth animations** with fade in/out effects

## Commands

### Enhanced `/event` Command

The event command now includes teleportation:

```
/event teleport <event_id>  - Teleport to an event location
```

**Example:**
```
/event teleport abc123-def456-ghi789
```

### Full Command List

- `/event list` - View all available events
- `/event info <name>` - Get detailed event information  
- `/event join <name>` - Join an event
- `/event leave <name>` - Leave an event
- `/event teleport <id>` - Teleport to an event location
- `/eventgui` - Open the events GUI

## Configuration

### Chat Settings in `config.yml`

```yaml
# Chat Configuration
chat:
  # Enable/disable chat announcements
  enabled: true
  # Play sound effects with announcements
  sound_effects: true
  # Announce to all players or only participants
  announce_to_all: true
  # Time before event starts to send reminders (in minutes)
  reminder_time: 5
  # Enable automatic reminders
  reminders_enabled: true
```

## Technical Implementation

### Architecture

- **ChatManager** - Core chat functionality and message formatting
- **Adventure API Integration** - Modern Minecraft chat components
- **EventManager Integration** - Automatic announcements for event lifecycle
- **EventTasker Integration** - Enhanced automatic event announcements
- **Caching System** - Performance optimization for frequent messages

### Performance Features

- **Message caching** (30-second duration) for identical announcements
- **Teleport cooldowns** (5-second) to prevent spam
- **Background cache cleanup** every 5 minutes
- **Thread-safe operations** using concurrent collections

### Safety Features

- **World validation** before teleportation
- **Permission checks** for command execution
- **Cooldown enforcement** to prevent abuse
- **Graceful error handling** with user feedback

## Example Chat Output

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🎉 NEW EVENT CREATED PvP Tournament #5 [PVP]
  An intense PvP tournament for skilled fighters!
  [⚡ TELEPORT] [📋 JOIN] [📤 SHARE]
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

## Integration with Existing Systems

### Automatic Events (EventTasker)

- Automatic events now use the new chat system instead of basic broadcasts
- Maintains backward compatibility with HUD notifications
- Falls back to basic announcements if chat is disabled

### Manual Events

- All manually created events automatically announce in chat
- Event lifecycle (start/end/cancel) triggers appropriate announcements
- Seamless integration with existing event management

### API Integration

- Chat announcements work alongside existing API hooks
- Compatible with all existing event types and statuses
- Maintains all existing functionality while adding chat enhancements

## Benefits

1. **Enhanced User Experience** - Beautiful, interactive chat messages
2. **Reduced Command Typing** - Click-to-interact functionality
3. **Better Event Visibility** - Eye-catching announcements
4. **Instant Access** - One-click teleportation and joining
5. **Professional Appearance** - Modern UI design principles
6. **Performance Optimized** - Caching and efficient operations
7. **Highly Configurable** - Full control over chat behavior

The chat system seamlessly integrates with your existing SwiftEvents infrastructure while providing a modern, interactive experience for your players! 