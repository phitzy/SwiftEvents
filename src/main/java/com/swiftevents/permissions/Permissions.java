package com.swiftevents.permissions;

/**
 * Utility class containing all permission nodes for the SwiftEvents plugin.
 */
public final class Permissions {

    private Permissions() {
        // Private constructor to prevent instantiation
    }

    // General Permissions
    public static final String RELOAD = "swiftevents.reload";
    public static final String UPDATE_CHECK = "swiftevents.updatecheck";

    // User Permissions
    public static final String USER_BASE = "swiftevents.user";
    public static final String USER_HELP = USER_BASE + ".help";
    public static final String USER_LIST = USER_BASE + ".list";
    public static final String USER_JOIN = USER_BASE + ".join";
    public static final String USER_LEAVE = USER_BASE + ".leave";
    public static final String USER_TP = USER_BASE + ".tp";
    public static final String USER_STATS = USER_BASE + ".stats";

    // Admin Permissions
    public static final String ADMIN_BASE = "swiftevents.admin";
    public static final String ADMIN_CREATE = ADMIN_BASE + ".create";
    public static final String ADMIN_DELETE = ADMIN_BASE + ".delete";
    public static final String ADMIN_EDIT = ADMIN_BASE + ".edit";
    public static final String ADMIN_START = ADMIN_BASE + ".start";
    public static final String ADMIN_STOP = ADMIN_BASE + ".stop";
    public static final String ADMIN_CLONE = ADMIN_BASE + ".clone";
    public static final String ADMIN_SETPOS = ADMIN_BASE + ".setpos";
    public static final String ADMIN_TOGGLE = ADMIN_BASE + ".toggle";
    public static final String ADMIN_PARTICIPANTS = ADMIN_BASE + ".participants";

    // Tasker Permissions
    public static final String TASKER_BASE = ADMIN_BASE + ".tasker";
    public static final String TASKER_START = TASKER_BASE + ".start";
    public static final String TASKER_STOP = TASKER_BASE + ".stop";
    public static final String TASKER_RESTART = TASKER_BASE + ".restart";
    public static final String TASKER_NEXT = TASKER_BASE + ".next";
    public static final String TASKER_STATUS = TASKER_BASE + ".status";

    // Event Type Permissions
    public static final String EVENT_TYPE_BASE = "swiftevents.event";
    public static final String EVENT_TYPE_PVP = EVENT_TYPE_BASE + ".pvp";
    public static final String EVENT_TYPE_TREASURE = EVENT_TYPE_BASE + ".treasure";
    public static final String EVENT_TYPE_BUILDING = EVENT_TYPE_BASE + ".building";
    public static final String EVENT_TYPE_RACING = EVENT_TYPE_BASE + ".racing";
    public static final String EVENT_TYPE_CUSTOM = EVENT_TYPE_BASE + ".custom";

    // Other Permissions
    public static final String BYPASS_COOLDOWN = "swiftevents.bypass.cooldown";
    public static final String BYPASS_MAX_EVENTS = "swiftevents.bypass.maxevents";

} 