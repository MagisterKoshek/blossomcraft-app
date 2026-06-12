package com.blossomcraft.core.model;

import java.util.List;

/** A role/rank with display styling and permission list. */
public class Role {
    public String id;
    public String name;
    public String displayName;
    public String color;
    public List<String> permissions;
    public boolean isSystem;
    public int priority;

    /** Builds the display role from a user record, mirroring the website's buildRole(). */
    public static Role fromUser(User user) {
        if (user == null || user.roleId == null) {
            return null;
        }
        Role role = new Role();
        role.id = user.roleId;
        role.name = user.roleDisplay != null ? user.roleDisplay : user.roleId;
        role.displayName = role.name;
        role.color = user.roleBgColor != null ? user.roleBgColor : "#a855f7";
        role.permissions = user.permissions == null ? List.of() : List.of(user.permissions);
        role.isSystem = true;
        role.priority = 50;
        return role;
    }
}
