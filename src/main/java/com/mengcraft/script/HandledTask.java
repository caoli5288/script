package com.mengcraft.script;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitScheduler;

/**
 * Created on 16-10-17.
 */
public class HandledTask {

    private final ScriptPlugin plugin;
    private final int id;

    public HandledTask(ScriptPlugin plugin, int id) {
        this.plugin = plugin;
        this.id = id;
    }

    public void cancel() {
        Bukkit.getScheduler().cancelTask(id);
    }

    public boolean isCancelled() {
        BukkitScheduler scheduler = Bukkit.getScheduler();
        return !(scheduler.isCurrentlyRunning(id) || scheduler.isQueued(id));
    }

    public ScriptPlugin getPlugin() {
        return plugin;
    }

}
