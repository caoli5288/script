package com.mengcraft.script;

import com.mengcraft.script.loader.ScriptPluginException;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

import static com.mengcraft.script.Main.nil;

/**
 * Created on 16-10-25.
 */
public class MainCommand implements CommandExecutor {

    private final Map<String, HandledExecutor> executor;
    private final Main main;

    public MainCommand(Main main, Map<String, HandledExecutor> executor) {
        this.main = main;
        this.executor = executor;
    }

    @Override
    public boolean onCommand(CommandSender who, Command i, String label, String[] j) {
        if (label.equals("script") || label.equals("script:script")) {
            if (who.hasPermission("script.admin")) {
                return admin(who, Arrays.asList(j).iterator());
            } else {
                who.sendMessage(ChatColor.RED + "你没有执行权限");
            }
        } else {
            HandledExecutor handled = executor.get(label);
            if (nil(handled)) {
                throw new IllegalStateException("喵");
            }
            return handled.execute(who, Arrays.asList(j));
        }
        return false;
    }

    private boolean admin(CommandSender who, Iterator<String> it) {
        if (it.hasNext()) {
            String label = it.next();
            if (label.equals("list")) {
                return list(who);
            }
            if (label.equals("load")) {
                return it.hasNext() && load(who, it.next());
            }
            if (label.equals("unload")) {
                return it.hasNext() && unload(who, it.next());
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
        main.reload();
        who.sendMessage(ChatColor.GREEN + "O-kay!");
        return true;
    }

    private boolean unload(CommandSender who, String i) {
        if (main.unload(i)) {
            who.sendMessage(ChatColor.GREEN + "O-kay!");
            return true;
        }
        return false;
    }

    private boolean list(CommandSender who) {
        who.sendMessage(ChatColor.GREEN + "> Loaded script");
        for (String l : main.list()) {
            who.sendMessage(ChatColor.GREEN + "- " + l);
        }
        return true;
    }

    private boolean load(CommandSender who, String i) {
        File file = new File(main.getDataFolder(), i);
        try {
            main.load(file);
            who.sendMessage(ChatColor.GREEN + "O-kay!");
            return true;
        } catch (IllegalArgumentException | ScriptPluginException e) {
            who.sendMessage(ChatColor.RED + e.getMessage());
        }
        return false;
    }

}
