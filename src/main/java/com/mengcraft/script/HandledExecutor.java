package com.mengcraft.script;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import static com.mengcraft.script.ScriptBootstrap.nil;

/**
 * Created on 16-10-17.
 */
public class HandledExecutor {

    private final String label;
    private final ScriptExecutor executor;
    private final String permission;
    private final ScriptPlugin plugin;

    public HandledExecutor(ScriptPlugin plugin, ScriptPlugin.Executor i) {
        this.plugin = plugin;
        label = i.getLabel();
        executor = i.getExecutor();
        permission = i.getPermission();
    }

    public boolean execute(CommandSender sender, Object input) {
        if (nil(permission) || sender.hasPermission(permission)) {
            executor.execute(sender, input);
            return true;
        } else {
            sender.sendMessage(ChatColor.RED + "你没有执行权限");
        }
        return false;
    }


    public boolean remove() {
        return plugin.remove(this);
    }

    public String getLabel() {
        return label;
    }

}
