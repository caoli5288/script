package com.mengcraft.script.loader;

import com.mengcraft.script.ScriptBootstrap;
import com.mengcraft.script.ScriptPlugin;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Created on 16-10-18.
 */
public class ScriptLogger extends Logger {

    private final ScriptPlugin plugin;
    private String prefix;

    public ScriptLogger(ScriptBootstrap main, ScriptPlugin plugin) {
        super("SCRIPT|" + plugin.getId(), null);
        setParent(main.getLogger());
        setLevel(Level.ALL);
        this.plugin = plugin;
    }

    @Override
    public void log(LogRecord record) {
        record.setMessage(format(record.getMessage()));
        getParent().log(record);
    }

    private String format(String msg) {
        if (prefix == null) {
            String name = plugin.getDescription("name");
            if (name == null) {
                prefix =  "[" + plugin.getId() + "] ";
            } else {
                prefix = "[" + name + "] ";
            }
        }
        return prefix.concat(msg);
    }

}
