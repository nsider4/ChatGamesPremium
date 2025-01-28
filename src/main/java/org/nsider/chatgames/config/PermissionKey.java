package org.nsider.chatgames.config;

/**
 * Enum representing plugin permissions with their descriptions and default values.
 */
public enum PermissionKey {
    GLOBAL_ELYTRA_BLOCK_OVERRIDE(
            "globalelytrablock.override",
            "Overrides the Elytra block when it is in use. Only works when Elytra block is enabled.",
            "op"
    ),
    REGION_MANAGE(
            "elytratweaks.region.manage",
            "Allows players to select and manage regions.",
            "op"
    );

    private final String permission;
    private final String description;
    private final String defaultValue;

    PermissionKey(String permission, String description, String defaultValue) {
        this.permission = permission;
        this.description = description;
        this.defaultValue = defaultValue;
    }

    /**
     * @return The permission string.
     */
    public String getPermission() {
        return permission;
    }

    /**
     * @return The description of the permission.
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return The default value for the permission.
     */
    public String getDefaultValue() {
        return defaultValue;
    }
}
