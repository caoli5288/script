package com.mengcraft.script.loader;

import com.mengcraft.script.ScriptBootstrap;
import com.mengcraft.script.ScriptPlugin;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static com.mengcraft.script.ScriptBootstrap.nil;

/**
 * Created on 16-10-18.
 */
public class ScriptLogger extends Logger {

    private final ScriptPlugin plugin;
    private String i;

    public ScriptLogger(ScriptBootstrap main, ScriptPlugin plugin) {
        super("SCRIPT|" + plugin.getId(), null);
        setParent(main.getLogger());
        setLevel(Level.ALL);
        this.plugin = plugin;
    }

    @Override
    public void log(LogRecord record) {
        record.setMessage(i() + record.getMessage());
        getParent().log(record);
    }

    String i() {
        if (nil(i)) {
            String name = plugin.getDescription("name");
            if (nil(name)) {
                return "[" + plugin.getId() + "] ";
            }
            i = "[" + name + "] ";
        }
        return i;
    }

}
