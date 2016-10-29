package com.mengcraft.script;

import org.bukkit.command.CommandSender;

/**
 * Created on 16-10-17.
 */
public interface ScriptExecutor {

    void execute(CommandSender sender, Object input);

}
