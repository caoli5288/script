package com.mengcraft.script;

import com.mengcraft.script.util.Utils;
import lombok.RequiredArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import javax.script.Bindings;
import java.util.function.BiConsumer;

/**
 * Created on 16-10-17.
 */
@RequiredArgsConstructor
public class HandledCommand implements CommandExecutor {

    private final ScriptPlugin script;
    private final String label;
    private final BiConsumer<CommandSender, Bindings> executor;
    private final String permission;

    public boolean execute(CommandSender sender, Bindings params) {
        if (permission == null || sender.hasPermission(permission)) {
            executor.accept(sender, params);
            return true;
        } else {
            sender.sendMessage(ChatColor.RED + "你没有执行权限");
        }
        return false;
    }


    public boolean remove() {
        return script.remove(this);
    }

    public String getLabel() {
        return label;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] params) {
        return execute(sender, Utils.fromJava(params));
    }
}
