package com.mengcraft.script;

import com.github.caoli5288.bukkitgroovy.BukkitGroovy;
import com.github.caoli5288.bukkitgroovy.Listeners;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class GroovyIntegration {

    private static BukkitGroovy groovy;
    private static boolean integrated;
    private static Map<String, EventListener> listeners;

    static void integrate() {
        groovy = JavaPlugin.getPlugin(BukkitGroovy.class);
        listeners = new HashMap<>();
        integrated = true;
    }

    public static boolean isIntegrated() {
        return integrated;
    }

    public static EventListener getListener(String name) {
        Listeners.Classes classes = groovy.getListeners().getClasses(name.replace(':', '_'));
        Objects.requireNonNull(classes, "Cannot found event " + name);
        Class<?> cls = classes.getCls();
        return listeners.computeIfAbsent(cls.getName(), s -> new EventListener(cls, cls.getSimpleName(), classes.getHandlers()));
    }

    public static void loadClasses(Plugin plugin) {
        groovy.getListeners().loadClasses(plugin.getName(), plugin.getClass(), s -> true);
    }
}
