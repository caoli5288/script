package com.mengcraft.script.loader;

import com.mengcraft.script.ScriptPlugin;

import javax.script.ScriptException;

/**
 * Created on 16-10-17.
 */
public class ScriptPluginException extends ScriptException {

    private final ScriptPlugin plugin;

    private ScriptPluginException(ScriptPlugin plugin, String message) {
        super(message);
        this.plugin = plugin;
    }

    public ScriptPlugin getPlugin() {
        return plugin;
    }

    public static void thr(ScriptPlugin script, String str) throws ScriptPluginException {
        if (script != null) {
            script.unload();
        }
        throw new ScriptPluginException(script, str);
    }

}
