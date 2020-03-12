package com.mengcraft.script.loader;

import com.mengcraft.script.EventMapping;
import com.mengcraft.script.ScriptBootstrap;
import com.mengcraft.script.ScriptPlugin;
import com.mengcraft.script.util.Named;
import lombok.Builder;
import lombok.experimental.Delegate;
import lombok.experimental.var;
import org.bukkit.command.CommandSender;

import javax.script.Bindings;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.io.Closeable;
import java.util.Map;
import java.util.function.Consumer;

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
            ctx.eval("load(\"nashorn:mozilla_compat.js\"); importPackage(java.lang, java.util, org.bukkit);");
            ctx.eval(info.contend);
            scriptObj = ctx.eval("this");
        } catch (ScriptException e) {
            ctx.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
            ScriptPluginException.thr(plugin, e.getMessage());
        }
        Object description = ctx.get("description");
        if (description instanceof Map) {
            plugin.setDescription((Map) description);
            if (plugin.getDescription("name") == null) {
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
        var version = plugin.getDescription("version");
        if (version != null) {
            b.append(" v");
            b.append(version);
        }
        var author = plugin.getDescription("author");
        if (author != null) {
            b.append(" authored by ");
            b.append(author);
        }
        return b.toString();
    }

    private static void loadListener(ScriptPlugin plugin, ScriptEngine ctx) {
        String handle = plugin.getDescription("handle");
        if (handle != null && EventMapping.INSTANCE.initialized(handle)) {
            var obj = ctx.get("handle");
            if (obj != null) {
                plugin.addListener(handle, ((Invocable) ctx).getInterface(obj, Consumer.class));
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

        public Bindings getScriptObj() {
            return (Bindings) scriptObj;
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
