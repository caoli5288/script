package com.mengcraft.script;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;
import com.mengcraft.script.loader.ScriptLoader;
import com.mengcraft.script.loader.ScriptPluginException;
import com.mengcraft.script.plugin.ScriptingLoader;
import com.mengcraft.script.util.ArrayHelper;
import com.mengcraft.script.util.BossBarWrapper;
import com.mengcraft.script.util.Named;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.SimplePluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.simple.JSONValue;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.io.Closeable;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import static org.bukkit.util.NumberConversions.toInt;

/**
 * Created on 16-10-17.
 */
public final class ScriptBootstrap extends JavaPlugin implements IScriptSpi {

    private static ScriptBootstrap plugin;
    private Map<String, Object> scripts;
    private ScriptEngine jsEngine;
    private final Map<String, HandledExecutor> executor = new HashMap<>();
    private ScriptLoader scriptLoader;
    private Unsafe unsafe;

    public static ScriptBootstrap get() {
        return plugin;
    }

    public static IScriptSpi getSpi() {
        return plugin;
    }

    @SneakyThrows
    public static Object require(File required) {
        switch (Files.getFileExtension(required.getName())) {
            case "js":
                ScriptEngine ctx = jsEngine();
                Bindings bindings = ctx.createBindings();
                ctx.eval("exports = {}", bindings);
                ctx.eval(Files.newReader(required, StandardCharsets.UTF_8), bindings);
                return ctx.eval("exports", bindings);
            case "json":
                return JSONValue.parse(Files.newReader(required, StandardCharsets.UTF_8));
            default:
                return required;
        }
    }

    public static ScriptEngine jsEngine() {
        return plugin.jsEngine;
    }

    @Override
    public void onLoad() {
        plugin = this;
        jsEngine = new ScriptEngineManager(getClassLoader()).getEngineByExtension("js");
    }

    @Override
    public void onEnable() {
        scriptLoader = new ScriptLoader();
        getServer().getConsoleSender().sendMessage(ArrayHelper.toArray(
                ChatColor.GREEN + "梦梦家高性能服务器出租店",
                ChatColor.GREEN + "shop105595113.taobao.com"));

        EventMapping.INSTANCE.loadClasses();// Register build-in event
        getLogger().info("Initialized " + (EventMapping.INSTANCE.getKnownClasses().size() / 2) + " build-in event(s)");

        Plugin plugin = Bukkit.getPluginManager().getPlugin("PlaceholderAPI");
        if (!nil(plugin)) {
            Formatter.setReplacePlaceholder(true);
        }

        getCommand("script").setExecutor(new MainCommand(this, executor));

        saveDefaultConfig();
        unsafe = new Unsafe(this);

        getServer().getScheduler().runTask(this, this::loadAll);
    }

    protected void reload() {
        onDisable();
        loadAll();
    }

    @Override
    @SneakyThrows
    public void onDisable() {
        for (Map.Entry<String, Object> i : new HashMap<>(scripts).entrySet()) {
            ((Closeable) i.getValue()).close();
        }
        scripts = null;
    }

    private void loadAll() {
        scripts = new HashMap<>();
        for (File obj : getDataFolder().listFiles()) {
            if (obj.isFile() && obj.getName().matches(".+\\.js")) {
                try {
                    load(getServer().getConsoleSender(), obj, null);
                } catch (ScriptPluginException e) {
                    getLogger().log(Level.WARNING, e.getMessage());
                }
            } else if (obj.isDirectory() && new File(obj, "plugin.js").isFile()) {
                loadEx(obj);
            }
        }
    }

    private void loadEx(File obj) {
        ScriptingLoader scripting = new ScriptingLoader(obj);
        if (scripts.containsKey(scripting.getName())) {
            getLogger().warning(String.format("!!! name conflict between %s and %s", scripting.getId(), ((Named) scripts.get(scripting.getName())).getId()));
        } else {
            scripts.put(scripting.getName(), scripting);
            Bukkit.getPluginManager().enablePlugin(scripting);
        }
    }

    @SneakyThrows
    protected void load(CommandSender loader, File obj, Object args) throws ScriptPluginException {
        if (obj.isDirectory() && new File(obj, "plugin.js").isFile()) {
            loadEx(obj);
        } else {
            thr(!obj.isFile() || isLoaded(obj), "path not loadable");
            loadScript(ScriptLoader.ScriptInfo.builder()
                    .loader(loader)
                    .id("file:" + obj)
                    .contend(Files.toString(obj, StandardCharsets.UTF_8))
                    .args(args).build());
        }
    }

    public static void thr(boolean b, String message) {
        if (b) throw new IllegalStateException(message);
    }

    private boolean isLoaded(File file) {
        String id = "file:" + file.getName();
        return !nil(lookById(id));
    }

    @Override
    public ScriptLoader.ScriptBinding loadScript(ScriptLoader.ScriptInfo info) throws ScriptPluginException {
        ScriptLoader.ScriptBinding binding = scriptLoader.load(info);
        ScriptPlugin loaded = binding.getPlugin();
        if (loaded.isHandled() && !loaded.isIdled()) {
            String name = loaded.getDescription("name");
            Named i = (Named) scripts.get(name);
            if (!nil(i)) {
                ScriptPluginException.thr(loaded, "Name conflict with " + i.getId());
            }
            scripts.put(name, binding);
        }
        return binding;
    }

    public static boolean nil(Object i) {
        return i == null;
    }

    Named lookById(String id) {
        for (Object obj : scripts.values()) {
            Named named = (Named) obj;
            if (named.getId().equals(id)) {
                return named;
            }
        }
        return null;
    }

    public ImmutableList<String> list() {
        return ImmutableList.copyOf(scripts.keySet());
    }

    @SuppressWarnings("unchecked")
    protected void addExecutor(HandledExecutor handled) {
        String label = handled.getLabel();
        Preconditions.checkState(!executor.containsKey(label));
        try {
            Field field = SimplePluginManager.class.getDeclaredField("commandMap");
            field.setAccessible(true);
            SimpleCommandMap i = SimpleCommandMap.class.cast(field.get(getServer().getPluginManager()));
            Field f = SimpleCommandMap.class.getDeclaredField("knownCommands");
            f.setAccessible(true);
            Map handler = Map.class.cast(f.get(i));
            PluginCommand command = getCommand("script");
            handler.putIfAbsent(label, command);
            handler.putIfAbsent("script:" + label, command);
            executor.put(label, handled);
            executor.put("script:" + label, handled);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @SneakyThrows
    protected boolean remove(HandledExecutor handled) {
        boolean b = executor.containsKey(handled.getLabel());
        if (b) {
            String label = handled.getLabel();
            Field field = SimplePluginManager.class.getDeclaredField("commandMap");
            field.setAccessible(true);
            SimpleCommandMap i = SimpleCommandMap.class.cast(field.get(getServer().getPluginManager()));
            Field f = SimpleCommandMap.class.getDeclaredField("knownCommands");
            f.setAccessible(true);
            Map handler = Map.class.cast(f.get(i));
            PluginCommand command = getCommand("script");
            handler.remove(label, command);
            handler.remove("script:" + label, command);
            executor.remove(label);
            executor.remove("script:" + label);
        }
        return b;
    }

    boolean unload(ScriptPlugin i) {
        String id = i.getDescription("name");
        if (nil(id)) return false;
        Object obj = scripts.get(id);
        ScriptLoader.ScriptBinding binding = obj instanceof ScriptLoader.ScriptBinding ? (ScriptLoader.ScriptBinding) obj : null;
        return !nil(binding) && binding.getPlugin() == i && scripts.remove(id, binding);
    }

    public void unload(ScriptingLoader scripting) {
        if (scripts.remove(scripting.getName(), scripting)) {
            Bukkit.getPluginManager().disablePlugin(scripting);
        }
    }

    @SneakyThrows
    boolean unload(String id) {
        if (scripts.containsKey(id)) {
            ((Closeable) scripts.get(id)).close();
            return true;
        }
        val binding = getScript(id);
        return !nil(binding) && binding.getPlugin().unload();
    }

    public ScriptLoader.ScriptBinding getScript(String name) {
        Object binding = scripts.get(name);
        if (nil(binding) && name.startsWith("file:")) {
            binding = lookById(name);
        }
        return binding instanceof ScriptLoader.ScriptBinding ? (ScriptLoader.ScriptBinding) binding : null;
    }

    public Unsafe getUnsafe() {
        return unsafe;
    }

    @RequiredArgsConstructor
    public static class Unsafe {

        private final ScriptBootstrap main;

        public Server getServer() {
            return main.getServer();
        }

        public Plugin getPlugin(String id) {
            return main.getServer().getPluginManager().getPlugin(id);
        }

        public Object getScript(String id) {
            ScriptLoader.ScriptBinding binding = main.getScript(id);
            return !nil(binding) ? binding.getScriptObj() : null;
        }

        public BossBarWrapper createBossBar(String text) {
            return new BossBarWrapper(Bukkit.createBossBar(text, BarColor.WHITE, BarStyle.SOLID));
        }

        public BossBarWrapper createBossBar(String text, Map<String, Object> attribute) {
            BarColor color = attribute.containsKey("color") ? BarColor.valueOf(String.valueOf(attribute.get("color")).toUpperCase()) : BarColor.WHITE;
            BarStyle style = attribute.containsKey("style") ? BarStyle.values()[toInt(attribute.get("style"))] : BarStyle.SOLID;
            BarFlag[] allFlag = attribute.containsKey("flag") ? ((List<String>) attribute.get("flag")).stream().map(name -> BarFlag.valueOf(name.toUpperCase())).toArray(BarFlag[]::new) : new BarFlag[0];
            return new BossBarWrapper(Bukkit.createBossBar(text, color, style, allFlag));
        }

    }

}
