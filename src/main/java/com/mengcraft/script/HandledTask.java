package com.mengcraft.script;

/**
 * Created on 16-10-17.
 */
public class HandledTask {

    private final ScriptPlugin plugin;
    private int id;

    public HandledTask(ScriptPlugin plugin) {
        this.plugin = plugin;
    }

    protected void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public boolean cancel() {
        return id > 0 && plugin.cancel(this);
    }

    public ScriptPlugin getPlugin() {
        return plugin;
    }

}
