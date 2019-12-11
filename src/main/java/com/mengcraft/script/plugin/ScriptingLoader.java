package com.mengcraft.script.plugin;

import com.google.common.base.Preconditions;
import com.mengcraft.script.EventMapping;
import com.mengcraft.script.Formatter;
import com.mengcraft.script.ScriptBootstrap;
import com.mengcraft.script.util.Named;
import com.mengcraft.script.util.Utils;
import lombok.Data;
import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginBase;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.PluginLogger;
import org.bukkit.plugin.RegisteredListener;
import org.bukkit.plugin.UnknownDependencyException;
import org.yaml.snakeyaml.Yaml;

import javax.script.Bindings;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.StringReader;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

@Getter
public class ScriptingLoader extends PluginBase implements PluginLoader, Named, Closeable, Listener {

    public ScriptingLoader(File dataFolder) {
        this.dataFolder = dataFolder;
        description = getPluginDescription(new File(dataFolder, "plugin.yml"));
        jsFile = new File(dataFolder, "plugin.js");
    }

    private final File dataFolder;

    @Override
    public File getDataFolder() {
        return dataFolder;
    }

    private final PluginDescriptionFile description;

    @Override
    public PluginDescriptionFile getDescription() {
        return description;
    }

    private FileConfiguration config;

    @Override
    public FileConfiguration getConfig() {
        if (config == null) {
            reloadConfig();
        }
        return config;
    }

    @Override
    @SneakyThrows
    public InputStream getResource(String name) {
        return new FileInputStream(new File(dataFolder, name));
    }

    @Override
    @SneakyThrows
    public void saveConfig() {
        getConfig().save(new File(dataFolder, "config.yml"));
    }

    @Override
    public void saveDefaultConfig() {

    }

    @Override
    public void saveResource(String name, boolean b) {

    }

    @Override
    @SneakyThrows
    public void reloadConfig() {
        if (config == null) {
            config = YamlConfiguration.loadConfiguration(new File(dataFolder, "config.yml"));
            return;
        }
        config.load(new File(dataFolder, "config.yml"));
    }

    @Override
    public PluginLoader getPluginLoader() {
        return this;
    }

    @Override
    public Server getServer() {
        return Bukkit.getServer();
    }

    private boolean enabled;

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean setter) {
        if (enabled != setter) {
            enabled = setter;
            if (setter) {
                onEnable();
            } else {
                onDisable();
            }
        }
    }

    @Data
    public static class Lifecycle {

        private Runnable unload;
    }

    private final Lifecycle lifecycle = new Lifecycle();

    @Override
    public void onDisable() {
        if (lifecycle.unload != null) {
            lifecycle.unload.run();
        }
    }

    @Override
    public void onLoad() {

    }

    private final File jsFile;

    @Override
    @SneakyThrows
    public void onEnable() {
        ScriptEngine ctx = ScriptBootstrap.jsEngine();
        Bindings bindings = ctx.createBindings();
        Object global = ctx.eval("this", bindings);
        Object jsObject = ctx.eval("Object", bindings);
        ((Invocable) ctx).invokeMethod(jsObject, "bindProperties", global, this);
        ctx.eval(new FileReader(jsFile), bindings);
    }

    private boolean naggable;

    @Override
    public boolean isNaggable() {
        return naggable;
    }

    @Override
    public void setNaggable(boolean b) {
        naggable = b;
    }

    @Override
    public ChunkGenerator getDefaultWorldGenerator(String s, String s1) {
        return null;
    }

    private final Logger logger = new PluginLogger(this);

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public boolean onCommand(CommandSender _0, Command _1, String _2, String[] _3) {
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender _0, Command _1, String _2, String[] _3) {
        return null;
    }

    //===== Scripting Plugin Loader

    @Override
    public Plugin loadPlugin(File js) throws UnknownDependencyException {
        if (jsFile.equals(js)) {
            return this;
        }
        return null;
    }

    @Override
    @SneakyThrows
    public PluginDescriptionFile getPluginDescription(File ymlFile) {
        if (ymlFile.isFile()) {
            Yaml yaml = Utils.getYaml();
            Map<String, Object> obj = yaml.load(new FileInputStream(ymlFile));
            obj.put("main", "plugin.js");
            return new PluginDescriptionFile(new StringReader(yaml.dump(obj)));
        }
        return new PluginDescriptionFile(dataFolder.getName(), "0.1.0", "plugin.js");
    }

    static Pattern[] pluginFileFilters = new Pattern[]{Pattern.compile("plugin\\.js")};

    @Override
    public Pattern[] getPluginFileFilters() {
        return pluginFileFilters;
    }

    @Override
    public Map<Class<? extends Event>, Set<RegisteredListener>> createRegisteredListeners(Listener listener, Plugin plugin) {
        return ScriptBootstrap.get().getPluginLoader().createRegisteredListeners(listener, plugin);
    }

    @Override
    public void enablePlugin(Plugin plugin) {
        Validate.isTrue(plugin instanceof ScriptingLoader, "Plugin is not associated with this PluginLoader");
        if (!plugin.isEnabled()) {
            plugin.getLogger().info("Enabling " + plugin.getDescription().getFullName());
            try {
                ((ScriptingLoader) plugin).setEnabled(true);
            } catch (Exception e) {
                Bukkit.getLogger().log(Level.SEVERE, "Error occurred while enabling " + plugin.getDescription().getFullName() + " (Is it up to date?)", e);
            }
            Bukkit.getPluginManager().callEvent(new PluginEnableEvent(plugin));
        }
    }

    @Override
    public void disablePlugin(Plugin plugin) {
        Validate.isTrue(plugin instanceof ScriptingLoader, "Plugin is not associated with this PluginLoader");
        if (plugin.isEnabled()) {
            String message = String.format("Disabling %s", plugin.getDescription().getFullName());
            plugin.getLogger().info(message);
            Bukkit.getPluginManager().callEvent(new PluginDisableEvent(plugin));
            try {
                ((ScriptingLoader) plugin).setEnabled(false);
            } catch (Exception e) {
                Bukkit.getLogger().log(Level.SEVERE, "Error occurred while disabling " + plugin.getDescription().getFullName() + " (Is it up to date?)", e);
            }
        }
    }

    //===== Internal

    @Override
    public String getId() {
        return "plugin:" + jsFile;
    }

    @Override
    public void close() {
        unload();
    }

    public Object require(String path) {
        return ScriptBootstrap.require(new File(dataFolder, path));
    }

    //===== Scripting Logic

    public void unload() {
        ScriptBootstrap.get().unload(this);
    }

    public void depend(String plugname, boolean ignore) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin(plugname);
        if (plugin == null || !plugin.isEnabled()) {
            Preconditions.checkState(ignore, "Plugin " + plugname + " not found or inactive");
            return;
        }
        EventMapping.get().init(plugin);
    }

    public void depend(String plugin) {
        depend(plugin, false);
    }

    @SneakyThrows
    public Class loadType(String clazz) {
        return ScriptBootstrap.class.getClassLoader().loadClass(clazz);
    }

    public String format(Player p, String input) {
        return Formatter.format(p, input);
    }

    public void broadcast(String... input) {
        for (String line : input) {
            Bukkit.getServer().broadcastMessage(ChatColor.translateAlternateColorCodes('&', line));
        }
    }

    public void runCommand(String command) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), ChatColor.translateAlternateColorCodes('&', command));
    }

    public void runCommand(Player p, String command) {
        p.chat("/" + Formatter.format(p, command));
    }

    public void setMetadata(Player player, String key, Object value) {
        player.setMetadata(key, new FixedMetadataValue(this, value));
    }

    public void removeMetadata(Player player, String metadataKey) {
        player.removeMetadata(metadataKey, this);
    }

    public Object getMetadata(Player player, String key) {
        for (MetadataValue metadataValue : player.getMetadata(key)) {
            if (metadataValue.getOwningPlugin() == this) {
                return metadataValue.value();
            }
        }
        return null;
    }

    public void addListener(String eventName, Consumer<Event> executor, int priority) {
        HandlerList handlerList = EventMapping.get().getListener(eventName).getHandlerList();
        RegisteredListener registered = new RegisteredListener(this, (_i, event) -> executor.accept(event), Utils.getEventPriority(priority), this, false);
        handlerList.register(registered);

    }

    public void addListener(String eventName, Consumer<Event> executor) {
        addListener(eventName, executor, -1);
    }

}
