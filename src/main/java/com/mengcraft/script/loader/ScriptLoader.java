package com.mengcraft.script.loader;

import com.mengcraft.script.EventMapping;
import com.mengcraft.script.ScriptBootstrap;
import com.mengcraft.script.ScriptListener;
import com.mengcraft.script.ScriptPlugin;
import com.mengcraft.script.util.Named;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import lombok.Builder;
import lombok.experimental.Delegate;
import org.bukkit.command.CommandSender;

import javax.script.Bindings;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.io.Closeable;
import java.io.Reader;
import java.util.Map;

import static com.mengcraft.script.ScriptBootstrap.nil;

/**
 * Created on 16-10-17.
 */
public class ScriptLoader {

    @SuppressWarnings("unchecked")
    public ScriptBinding load(ScriptInfo info) throws ScriptPluginException {
        ScriptBootstrap bootstrap = ScriptBootstrap.get();
        ScriptPlugin plugin = new ScriptPlugin(bootstrap, info.id);
        ScriptEngine ctx = ScriptBootstrap.jsEngine();
        Bindings bindings = ctx.getBindings(ScriptContext.ENGINE_SCOPE);
        ctx.setBindings(ctx.createBindings(), ScriptContext.ENGINE_SCOPE);
        ctx.put("plugin", plugin);
        ctx.put("args", info.args);
        ctx.put("arg", info.args);// Compatible codes
        ctx.put("loader", info.loader);
        Object scriptObj = null;
        try {
            ctx.eval(info.contend);
            scriptObj = ctx.eval("this");
        } catch (ScriptException e) {
            ctx.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
            ScriptPluginException.thr(plugin, e.getMessage());
        }
        Object description = ctx.get("description");
        if (description instanceof Map) {
            plugin.setDescription((Map) description);
            if (nil(plugin.getDescription("name"))) {
                plugin.setDescription("name", info.id);
            }
            loadListener(plugin, ctx);
            bootstrap.getLogger().info(load(plugin));
        } else {
            plugin.setDescription("name", info.id);
            bootstrap.getLogger().info("Load script " + info.id);
        }
        ctx.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
        return ScriptBinding.bind(plugin, scriptObj);
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

    private static void loadListener(ScriptPlugin plugin, ScriptEngine ctx) {
        String handle = plugin.getDescription("handle");
        if (!nil(handle) && EventMapping.INSTANCE.initialized(handle)) {
            ScriptListener listener = ((Invocable) ctx).getInterface(ScriptListener.class);
            if (!nil(listener)) {
                plugin.addListener(handle, listener);
            }
        }
    }

    @Builder
    public final static class ScriptInfo {

        private CommandSender loader;
        private String id;
        private String contend;
        private Object args;
    }

    public final static class ScriptBinding implements Closeable, Named {

        @Delegate(types = Named.class)
        private final ScriptPlugin plugin;
        private final Object scriptObj;

        private ScriptBinding(ScriptPlugin plugin, Object scriptObj) {
            this.plugin = plugin;
            this.scriptObj = scriptObj;
        }

        public ScriptPlugin getPlugin() {
            return plugin;
        }

        public ScriptObjectMirror getScriptObj() {
            return (ScriptObjectMirror) scriptObj;
        }

        @Override
        public String toString() {
            return plugin.getId();
        }

        @Override
        public void close() {
            plugin.unload();
        }

        private static ScriptBinding bind(ScriptPlugin plugin, Object scriptObj) {
            return new ScriptBinding(plugin, scriptObj);
        }

    }

}
