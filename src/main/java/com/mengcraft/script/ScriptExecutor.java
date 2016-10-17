package com.mengcraft.script;

import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * Created on 16-10-17.
 */
public interface ScriptExecutor {

    void execute(CommandSender sender, List<String> list);

}
