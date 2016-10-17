package com.mengcraft.script;

import com.google.common.base.Preconditions;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import static com.mengcraft.script.Main.nil;

/**
 * Created on 16-10-17.
 */
public class EventMapping {

    private final Map<String, Mapping> mapping = new HashMap<>();

    public final static class Mapping {
        private final Class<?> clz;
        private final String name;

        private EventListener listener;

        private Mapping(Class<?> clz) {
            this.clz = clz;
            name = clz.getSimpleName().toLowerCase();
        }

        public String getName() {
            return name;
        }

        private EventListener getListener() {
            if (nil(listener)) {
                listener = new EventListener(this);
            }
            return listener;
        }
    }

    public boolean initialized(String name) {
        return mapping.containsKey(name.toLowerCase());
    }

    public EventListener getListener(String name) {
        String id = name.toLowerCase();
        if (!mapping.containsKey(id)) {
            throw new IllegalArgumentException("Not initialized");
        }
        return mapping.get(id).getListener();
    }

    public void init(Plugin plugin) {
        init(plugin.getClass().getClassLoader());
    }

    @SuppressWarnings("unchecked")
    public void init(ClassLoader loader) {
        try {
            Field field = ClassLoader.class.getDeclaredField("classes");
            field.setAccessible(true);
            List<Class<?>> list = (List) field.get(loader);
            for (Class<?> clz : list) {
                if (valid(clz)) {
                    try {
                        init(clz);
                    } catch (Exception e) {
                        Bukkit.getLogger().log(Level.WARNING, "[Script] " + e.getMessage());
                    }
                }
            }
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e.toString());
        }
    }

    @SuppressWarnings("unchecked")
    public void init(String plugin) {
        init(Bukkit.getPluginManager().getPlugin(plugin));
    }

    public void init(Class<?> clz) {
        Preconditions.checkArgument(valid(clz), clz.getName() + " not valid");
        String name = clz.getSimpleName().toLowerCase();
        if (mapping.containsKey(name)) {
            throw new IllegalArgumentException("Already initialized");
        }
        mapping.put(name, new Mapping(clz));
        Bukkit.getLogger().log(Level.INFO, "[Script] Initialized " + clz.getSimpleName());
    }

    private boolean valid(Class<?> clz) {
        return Event.class.isAssignableFrom(clz) && Modifier.isPublic(clz.getModifiers()) && !Modifier.isAbstract(clz.getModifiers());
    }

    protected static HandlerList getHandler(Mapping mapping) {
        try {
            Method e = mapping.clz.getDeclaredMethod("getHandlerList");
            e.setAccessible(true);
            return (HandlerList) e.invoke(null);
        } catch (Exception e) {
            throw new RuntimeException(e.toString());
        }
    }

    public static final EventMapping INSTANCE = new EventMapping();

}
