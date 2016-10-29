package com.mengcraft.script;

import com.google.common.base.Preconditions;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

import static com.mengcraft.script.Main.nil;

/**
 * Created on 16-10-17.
 */
public final class EventMapping {

    private final Map<String, Mapping> mapping = new HashMap<>();

    public final static class Mapping {
        private final Class<?> clz;
        private final String name;

        private EventListener listener;

        private Mapping(String name, Class<?> clz) {
            this.clz = clz;
            this.name = name;
        }

        public boolean valid(Event event) {
            return event.getClass() == clz;
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
        Preconditions.checkState(mapping.containsKey(id));
        return mapping.get(id).getListener();
    }

    public int init(Plugin plugin) {
        Class<?> clz = plugin.getClass();
        URL path = clz.getProtectionDomain().getCodeSource().getLocation();
        return init(clz.getClassLoader(), path, "(.*)\\.class");
    }

    protected int init() {
        URL path = Bukkit.class.getProtectionDomain().getCodeSource().getLocation();
        return init(Bukkit.class.getClassLoader(), path, "org/bukkit/event/(.*)/(.*)\\.class");
    }

    private int init(ClassLoader loader, URL path, String regex) {
        Preconditions.checkArgument(path.getProtocol().equals("file"));
        Pattern pattern = Pattern.compile(regex);
        int i = mapping.size();
        try {
            JarFile ball = new JarFile(path.getFile());
            Enumeration<JarEntry> all = ball.entries();
            while (all.hasMoreElements()) {
                JarEntry element = all.nextElement();
                if (pattern.matcher(element.getName()).matches()) init(loader, element);
            }
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
        return mapping.size() - i;
    }

    private void init(ClassLoader loader, JarEntry element) {
        try {
            String i = element.getName().replace('/', '.');
            Class<?> clz = loader.loadClass(i.substring(0, i.length() - 6));
            if (valid(clz)) init(clz);
        } catch (NoClassDefFoundError | ClassNotFoundException i) {
        }
    }

    public int init(String plugin) {
        return init(Bukkit.getPluginManager().getPlugin(plugin));
    }

    public void init(Class<?> clz) {
        Preconditions.checkArgument(valid(clz), clz.getName() + " not valid");
        String name = clz.getSimpleName().toLowerCase();
        if (mapping.get(name) == null) {
            mapping.put(name, new Mapping(name, clz));
        }
    }

    private static boolean valid(Class<?> clz) {
        return Event.class.isAssignableFrom(clz) && Modifier.isPublic(clz.getModifiers()) && !Modifier.isAbstract(clz.getModifiers());
    }

    protected static HandlerList getHandler(Mapping mapping) {
        return getHandler(mapping.clz);
    }

    private static HandlerList getHandler(Class<?> clz) {
        try {
            Method e = clz.getDeclaredMethod("getHandlerList");
            e.setAccessible(true);
            return (HandlerList) e.invoke(null);
        } catch (NoSuchMethodException e) {
            Class<?> father = clz.getSuperclass();
            if (valid(father)) {
                return getHandler(father);
            }
            throw new RuntimeException(e.toString());
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e.toString());
        }
    }

    public static final EventMapping INSTANCE = new EventMapping();

}
