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
import java.io.Reader;
import java.util.Map;

import static com.mengcraft.script.Main.nil;

/**
 * Created on 16-10-17.
 */
public class ScriptLoader {

    public static ScriptBinding load(Main main, File file) throws ScriptPluginException {
        FileReader reader = null;
        try {
            reader = new FileReader(file);
        } catch (FileNotFoundException e) {
            ScriptPluginException.thr(null, e.getMessage());
        }
        return load(main, "file:" + file.getName(), reader);
    }

    @SuppressWarnings("unchecked")
    public static ScriptBinding load(Main main, String id, Reader reader) throws ScriptPluginException {
        ScriptEngine engine = new ScriptEngineManager().getEngineByName("js");
        ScriptPlugin plugin = new ScriptPlugin(main, id);
        try {
            engine.put("plugin", plugin);
            engine.eval(reader);
            Object description = engine.get("description");
            if (Map.class.isInstance(description)) {
                plugin.setDescription((Map) description);
                if (nil(plugin.getDescription("name"))) {
                    plugin.setDescription("name", id);
                }
                main.getLogger().info(load(plugin));
                main.getLogger().info(loadListener(plugin, engine));
            } else {
                main.getLogger().info("Load script " + id);
            }
        } catch (ScriptException e) {
            ScriptPluginException.thr(plugin, e.getMessage());
        }
        return ScriptBinding.bind(plugin, engine);
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

    private static String loadListener(ScriptPlugin plugin, ScriptEngine engine) {
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

    public final static class ScriptBinding {

        private final ScriptPlugin plugin;
        private final ScriptEngine engine;

        private ScriptBinding(ScriptPlugin plugin, ScriptEngine engine) {
            this.plugin = plugin;
            this.engine = engine;
        }

        public ScriptPlugin getPlugin() {
            return plugin;
        }

        public ScriptEngine getEngine() {
            return engine;
        }

        @Override
        public String toString() {
            return plugin.toString();
        }

        public static ScriptBinding bind(ScriptPlugin plugin, ScriptEngine engine) {
            return new ScriptBinding(plugin, engine);
        }

    }

}
