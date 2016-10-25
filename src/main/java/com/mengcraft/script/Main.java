package com.mengcraft.script;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.mengcraft.script.loader.ScriptLoader;
import com.mengcraft.script.loader.ScriptPluginException;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.lang.reflect.Array;
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

    private Map<String, ScriptLoader.ScriptBinding> plugin;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        EventMapping.INSTANCE.init();// Register build-in event

        load();
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
        for (ScriptLoader.ScriptBinding i : plugin.values()) {
            if (i.toString().equals(id)) return true;
        }
        return false;
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

    protected boolean unload(ScriptPlugin i) {
        return plugin.remove(i.getDescription("name"), i);
    }

    protected boolean unload(String id) {
        ScriptLoader.ScriptBinding binding = plugin.get(id);
        if (!nil(binding)) {
            return binding.getPlugin().unload();
        }
        return false;
    }

    public ScriptLoader.ScriptBinding getScript(String id) {
        return plugin.get(id);
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
