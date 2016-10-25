package com.mengcraft.script;

import com.mengcraft.script.loader.ScriptPluginException;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;

/**
 * Created on 16-10-25.
 */
public class MainCommand implements CommandExecutor {

    private final Main main;

    public MainCommand(Main main) {
        this.main = main;
    }

    @Override
    public boolean onCommand(CommandSender who, Command i, String label, String[] j) {
        Iterator<String> it = Arrays.asList(j).iterator();
        if (it.hasNext()) {
            String n = it.next();
            if (eq(n, "list")) {
                return list(who);
            }
            if (eq(n, "load")) {
                return load(who, it.next());
            }
            if (eq(n, "unload")) {
                return unload(who, it.next());
            }
            if (eq(n, "reload")) {
                return reload(who);
            }
        } else {
            who.sendMessage(ChatColor.RED + "/script list");
            who.sendMessage(ChatColor.RED + "/script load <file_name>");
            who.sendMessage(ChatColor.RED + "/script unload <script_name>");
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

    private static boolean eq(Object i, Object j) {
        return i == j || i.equals(j);
    }

}
