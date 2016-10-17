package com.mengcraft.script.loader;

import com.mengcraft.script.Main;
import com.mengcraft.script.ScriptPlugin;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static com.mengcraft.script.Main.nil;

/**
 * Created on 16-10-18.
 */
public class ScriptLogger extends Logger {

    private final ScriptPlugin plugin;
    private String i;

    public ScriptLogger(Main main, ScriptPlugin plugin) {
        super("Script|" + plugin.toString(), null);
        setParent(main.getLogger());
        setLevel(Level.ALL);
        this.plugin = plugin;
    }

    @Override
    public void log(LogRecord record) {
        record.setMessage(i() + record.getMessage());
        super.log(record);
    }

    private String i() {
        if (nil(i)) {
            String name = plugin.getDescription("name");
            if (nil(name)) {
                return "[Script|" + plugin.toString() + "] ";
            }
            i = "[Script|" + name + "] ";
        }
        return i;
    }

}
