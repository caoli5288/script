package com.mengcraft.script;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
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

    private final Map<String, ScriptPlugin> plugin = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();

        EventMapping.INSTANCE.init(getServer().getClass().getClassLoader());// Register build-in event

        List<String> list = getConfig().getStringList("script");
        for (String i : list) {
            File file = new File(getDataFolder(), i);
            if (file.isFile()) {
                try {
                    plugin.put(i, ScriptLoader.load(this, file));
                } catch (ScriptPluginException e) {
                    e.getPlugin().unload();
                    getLogger().log(Level.WARNING, e.getMessage() + " while load " + i);
                }
            }
        }
    }

    @Override
    public void onDisable() {
        for (Map.Entry<String, ScriptPlugin> i : new HashMap<>(plugin).entrySet()) {
            i.getValue().unload();
        }
    }


    public int execute(Runnable runnable, int delay, int repeat) {
        return getServer().getScheduler().runTaskTimerAsynchronously(this, runnable, delay, repeat).getTaskId();
    }

    public int process(Runnable runnable, int delay, int repeat) {
        return getServer().getScheduler().runTaskTimer(this, runnable, delay, repeat).getTaskId();
    }

    public void unload(ScriptPlugin i) {
        Preconditions.checkArgument(!(i == null));
        ScriptPlugin get = plugin.get(i.toString());
        if (get == i) {
            plugin.remove(i.toString());
        }
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
