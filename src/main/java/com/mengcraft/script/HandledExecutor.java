package com.mengcraft.script;

import org.bukkit.command.CommandSender;

import java.util.List;

import static com.mengcraft.script.Main.nil;

/**
 * Created on 16-10-17.
 */
public class HandledExecutor {

    private final String label;
    private final ScriptExecutor executor;
    private final String permission;

    public HandledExecutor(String label, String permission, ScriptExecutor executor) {
        this.label = label;
        this.executor = executor;
        this.permission = permission;
    }

    public void execute(CommandSender sender, List<String> list) {
        if (nil(permission) || sender.hasPermission(permission)) {
            executor.execute(sender, list);
        }
    }

    public String getLabel() {
        return label;
    }

}
