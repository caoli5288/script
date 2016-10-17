package com.mengcraft.script.loader;

import com.mengcraft.script.ScriptPlugin;

import javax.script.ScriptException;

/**
 * Created on 16-10-17.
 */
public class ScriptPluginException extends ScriptException {

    private final ScriptPlugin plugin;

    public ScriptPluginException(ScriptPlugin plugin, String message) {
        super(message);
        this.plugin = plugin;
    }

    public ScriptPlugin getPlugin() {
        return plugin;
    }

}
