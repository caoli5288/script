package com.mengcraft.script.loader;

import com.mengcraft.script.EventMapping;
import com.mengcraft.script.Main;
import com.mengcraft.script.ScriptListener;
import com.mengcraft.script.ScriptPlugin;
import lombok.Builder;
import org.bukkit.command.CommandSender;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.Reader;
import java.util.Map;

import static com.mengcraft.script.Main.nil;

/**
 * Created on 16-10-17.
 */
public class ScriptLoader {

    private final Main main;

    public ScriptLoader(Main main) {
        this.main = main;
    }

    @SuppressWarnings("unchecked")
    public ScriptBinding load(ScriptInfo info) throws ScriptPluginException {
        ScriptEngine engine = new ScriptEngineManager().getEngineByName("js");
        ScriptPlugin plugin = new ScriptPlugin(main, info.id);
        engine.put("plugin", plugin);
        engine.put("arg", info.arg);
        engine.put("loader", info.loader);
        try {
            engine.eval(info.contend);
        } catch (ScriptException e) {
            ScriptPluginException.thr(plugin, e.getMessage());
        }
        Object description = engine.get("description");
        if (Map.class.isInstance(description)) {
            plugin.setDescription((Map) description);
            if (nil(plugin.getDescription("name"))) {
                plugin.setDescription("name", info.id);
            }
            loadListener(plugin, engine);
            main.getLogger().info(load(plugin));
        } else {
            plugin.setDescription("name", info.id);
            main.getLogger().info("Load script " + info.id);
        }
        return ScriptBinding.bind(plugin, engine);
    }

    private static String load(ScriptPlugin plugin) {
        StringBuilder b = new StringBuilder("Loaded script ");
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

    private static void loadListener(ScriptPlugin plugin, ScriptEngine engine) {
        String handle = plugin.getDescription("handle");
        if (!nil(handle) && EventMapping.INSTANCE.initialized(handle)) {
            ScriptListener listener = getInterface(engine, ScriptListener.class);
            if (!nil(listener)) {
                plugin.addListener(handle, listener);
            }
        }
    }

    private static <T> T getInterface(ScriptEngine engine, Class<T> i) {
        return Invocable.class.cast(engine).getInterface(i);
    }

    @Builder
    public final static class ScriptInfo {

        private CommandSender loader;
        private String id;
        private Reader contend;
        private Object arg;
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

        private static ScriptBinding bind(ScriptPlugin plugin, ScriptEngine engine) {
            return new ScriptBinding(plugin, engine);
        }

    }

}
