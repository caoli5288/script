package com.mengcraft.script;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mengcraft.script.loader.ScriptPluginException;
import com.mengcraft.script.util.Utils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

/**
 * Created on 16-10-25.
 */
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class ScriptCommand implements CommandExecutor {

    private final Map<String, HandledCommand> commands = Maps.newHashMap();

    public boolean containsKey(Object key) {
        return commands.containsKey(key);
    }

    public HandledCommand get(Object key) {
        return commands.get(key);
    }

    public HandledCommand put(String key, HandledCommand value) {
        return commands.put(key, value);
    }

    public HandledCommand remove(Object key) {
        return commands.remove(key);
    }

    @Override
    public boolean onCommand(CommandSender who, Command _command, String label, String[] params) {
        if (label.equals("script") || label.equals("script:script")) {
            if (who.hasPermission("script.admin")) {
                return admin(who, Arrays.asList(params).iterator());
            } else {
                who.sendMessage(ChatColor.RED + "你没有执行权限");
            }
        } else {
            HandledCommand executor = commands.get(label);
            if (executor == null) {
                throw new IllegalStateException("喵");
            }
            return executor.onCommand(who, _command, label, params);
        }
        return false;
    }

    private boolean admin(CommandSender who, Iterator<String> i) {
        if (i.hasNext()) {
            String label = i.next();
            if (label.equals("list")) {
                return list(who);
            }
            if (label.equals("load")) {
                return i.hasNext() && load(who, i.next(), i.hasNext() ? Utils.fromJava(Lists.newArrayList(i)) : null);
            }
            if (label.equals("unload")) {
                return i.hasNext() && unload(who, i.next());
            }
            if (label.equals("reload")) {
                return reload(who);
            }
        } else {
            who.sendMessage(ChatColor.RED + "/script load <name>");
            who.sendMessage(ChatColor.RED + "/script list");
            who.sendMessage(ChatColor.RED + "/script unload <name>");
            who.sendMessage(ChatColor.RED + "/script reload");
        }
        return false;
    }

    private boolean reload(CommandSender who) {
        ScriptBootstrap.get().reload();
        who.sendMessage(ChatColor.GREEN + "O-kay!");
        return true;
    }

    private boolean unload(CommandSender who, String i) {
        if (ScriptBootstrap.get().unload(i)) {
            who.sendMessage(ChatColor.GREEN + "O-kay!");
            return true;
        }
        return false;
    }

    private boolean list(CommandSender who) {
        who.sendMessage(ChatColor.GREEN + "> Loaded script(s)");
        for (String l : ScriptBootstrap.get().list()) {
            who.sendMessage(ChatColor.GREEN + "- " + l);
        }
        return true;
    }

    private boolean load(CommandSender who, String load, Object arg) {
        try {
            ScriptBootstrap.get().load(who, new File(ScriptBootstrap.get().getDataFolder(), load), arg);
            who.sendMessage(ChatColor.GREEN + "O-kay!");
            return true;
        } catch (IllegalArgumentException | ScriptPluginException e) {
            who.sendMessage(ChatColor.RED + e.getMessage());
        }
        return false;
    }

}
