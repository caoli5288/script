package com.mengcraft.script.loader;

import com.mengcraft.script.ScriptPlugin;

import javax.script.ScriptException;

import static com.mengcraft.script.ScriptBootstrap.nil;

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

    public static void thr(ScriptPlugin i, String str) throws ScriptPluginException {
        if (!nil(i)) {
            i.unload();
        }
        throw new ScriptPluginException(i, str);
    }

}
