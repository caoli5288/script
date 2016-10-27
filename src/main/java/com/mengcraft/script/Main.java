package com.mengcraft.script;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.mengcraft.script.loader.ScriptLoader;
import com.mengcraft.script.loader.ScriptPluginException;
import org.bukkit.ChatColor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.plugin.SimplePluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Level;

/**
 * Created on 16-10-17.
 */
public final class Main extends JavaPlugin {

    private final Map<String, HandledExecutor> executor = new HashMap<>();
    private Map<String, ScriptLoader.ScriptBinding> plugin;

    @Override
    public void onEnable() {
        String[] i = {
                ChatColor.GREEN + "梦梦家高性能服务器出租店",
                ChatColor.GREEN + "shop105595113.taobao.com"
        };
        getServer().getConsoleSender().sendMessage(i);

        saveDefaultConfig();

        EventMapping.INSTANCE.init();// Register build-in event

        load();

        getCommand("script").setExecutor(new MainCommand(this, executor));
    }

    @Override
    public void onDisable() {
        for (Map.Entry<String, ScriptLoader.ScriptBinding> i : new HashMap<>(plugin).entrySet()) {
            i.getValue().getPlugin().unload();
        }
        plugin = null;
    }

    protected void reload() {
        onDisable();
        load();
    }

    private void load() {
        plugin = new HashMap<>();
        List<String> list = getConfig().getStringList("script");
        for (String i : list) {
            try {
                load(new File(getDataFolder(), i));
            } catch (ScriptPluginException e) {
                getLogger().log(Level.WARNING, e.getMessage());
            }
        }
    }

    protected void load(File file) throws ScriptPluginException {
        Preconditions.checkArgument(file.isFile(), "file not exist");
        Preconditions.checkArgument(!isLoaded(file), "file is loaded");

        ScriptLoader.ScriptBinding binding = ScriptLoader.load(this, file);
        ScriptPlugin loaded = binding.getPlugin();
        String name = loaded.getDescription("name");
        ScriptLoader.ScriptBinding i = plugin.get(name);
        if (!nil(i)) {
            ScriptPluginException.thr(loaded, "Name conflict with " + i.getPlugin());
        }

        plugin.put(name, binding);
    }

    private boolean isLoaded(File file) {
        String id = "file:" + file.getName();
        return !nil(getScriptById(id));
    }

    public ImmutableList<String> list() {
        return ImmutableList.copyOf(plugin.keySet());
    }

    public int execute(Runnable runnable, int delay, int repeat) {
        return getServer().getScheduler().runTaskTimerAsynchronously(this, runnable, delay, repeat).getTaskId();
    }

    public int process(Runnable runnable, int delay, int repeat) {
        return getServer().getScheduler().runTaskTimer(this, runnable, delay, repeat).getTaskId();
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

    protected boolean remove(HandledExecutor handled) {
        String label = handled.getLabel();
        if (executor.containsKey(label)) {
            try {
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
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e.getMessage());
            }
            return true;
        }
        return false;
    }

    protected boolean unload(ScriptPlugin i) {
        String id = i.getDescription("name");
        ScriptLoader.ScriptBinding binding = plugin.get(id);
        return !nil(binding) && binding.getPlugin() == i && plugin.remove(id, binding);
    }

    protected boolean unload(String id) {
        ScriptLoader.ScriptBinding binding = plugin.get(id);
        if (nil(binding) && id.startsWith("file:")) {
            binding = getScriptById(id);
        }
        return !nil(binding) && binding.getPlugin().unload();
    }

    protected ScriptLoader.ScriptBinding getScriptById(String id) {
        for (ScriptLoader.ScriptBinding i : plugin.values()) {
            if (i.toString().equals(id)) return i;
        }
        return null;
    }

    public ScriptLoader.ScriptBinding getScript(String name) {
        return plugin.get(name);
    }

    @SuppressWarnings("all")
    public static <T, E> T[] collect(Class<T> type, List<E> in, Function<E, T> func) {
        List<T> handle = new ArrayList<>(in.size());
        for (E i : in) {
            T j = func.apply(i);
            if (!nil(j)) handle.add(j);
        }
        return handle.toArray((T[]) Array.newInstance(type, handle.size()));
    }

    public static boolean nil(Object i) {
        return i == null;
    }

}
