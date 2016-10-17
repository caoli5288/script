package com.mengcraft.script.loader;

import com.mengcraft.script.EventMapping;
import com.mengcraft.script.Main;
import com.mengcraft.script.ScriptListener;
import com.mengcraft.script.ScriptPlugin;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Map;

import static com.mengcraft.script.Main.nil;

/**
 * Created on 16-10-17.
 */
public class ScriptLoader {

    @SuppressWarnings("unchecked")
    public static ScriptPlugin load(Main main, File file) throws ScriptPluginException {
        ScriptEngine engine = new ScriptEngineManager().getEngineByName("js");
        ScriptPlugin plugin = new ScriptPlugin(main);
        try {
            engine.put("plugin", plugin);
            engine.eval(new FileReader(file));
            Object description = engine.get("description");
            if (!nil(description) && description instanceof Map) {
                Map map = Map.class.cast(description);
                plugin.setDescription(map);
                if (nil(plugin.getDescription("name"))) {
                    plugin.setDescription("name", file.getName());
                }
                main.getLogger().info(load(plugin));
                main.getLogger().info(loadHandler(plugin, engine));
            } else {
                main.getLogger().info("Load script " + file.getName());
            }
        } catch (ScriptException | FileNotFoundException e) {
            throw new ScriptPluginException(plugin, e.getMessage());
        }
        return plugin;
    }

    private static String load(ScriptPlugin plugin) {
        StringBuilder b = new StringBuilder("Load script ");
        b.append(plugin.getDescription("name"));
        Object version = plugin.getDescription("version");
        if (!nil(version)) {
            b.append(" v");
            b.append(version);
        }
        Object author = plugin.getDescription("author");
        if (!nil(author)) {
            b.append(" authored by ");
            b.append(author);
        }
        return b.toString();
    }

    private static String loadHandler(ScriptPlugin plugin, ScriptEngine engine) {
        String handle = plugin.getDescription("handle");
        if (!nil(handle) && EventMapping.INSTANCE.initialized(handle)) {
            ScriptListener listener = getInterface(engine, ScriptListener.class);
            if (nil(listener)) {
                return "Error while try to handle " + handle;
            } else {
                plugin.addListener(handle, listener);
            }
            return "Handled event " + handle;
        }
        return "No handle declare in description";
    }

    private static <T> T getInterface(ScriptEngine engine, Class<T> i) {
        return Invocable.class.cast(engine).getInterface(i);
    }

}
