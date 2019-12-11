package com.mengcraft.script;

import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import com.mengcraft.script.util.ArrayHelper;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

/**
 * Created on 16-10-17.
 */
public final class EventMapping {

    public static final EventMapping INSTANCE = new EventMapping();
    private final Map<String, Mapping> mapping = Maps.newHashMap();
    private final Set<String> plugins = Sets.newHashSet();
    private final Multimap<String, Binding> knownClasses = ArrayListMultimap.create();

    public Multimap<String, Binding> getKnownClasses() {
        return knownClasses;
    }

    public boolean initialized(String name) {
        return mapping.containsKey(name.toLowerCase());
    }

    @SneakyThrows
    public EventListener getListener(String name) {
        name = name.toLowerCase();
        if (mapping.containsKey(name)) {
            return mapping.get(name).getListener();
        }
        if (!knownClasses.containsKey(name)) {
            throw new IllegalStateException("event not found & initialized");
        }
        for (Binding binding : knownClasses.get(name)) {
                Class<?> loadedClass = Class.forName(binding.getFullName(), true, binding.getLoader());
                if (isEventClass(loadedClass)) {
                    loadEventClass(binding.getPlugin(), loadedClass);
                }
        }
        return Objects.requireNonNull(mapping.get(name), "event not found & initialized").getListener();// NPE
    }

    protected void loadClasses() {
        URL path = Bukkit.class.getProtectionDomain().getCodeSource().getLocation();
        loadClasses(null, Bukkit.class.getClassLoader(), path, "org/bukkit/event/(.*)/(.*)\\.class");
    }

    protected void loadClasses(Plugin plugin, ClassLoader loader, URL path, String regex) {
        Preconditions.checkArgument(path.getProtocol().equals("file"));
        Pattern pattern = Pattern.compile(regex);
        try {
            JarFile ball = new JarFile(path.getFile());
            Enumeration<JarEntry> all = ball.entries();
            while (all.hasMoreElements()) {
                JarEntry element = all.nextElement();
                if (pattern.matcher(element.getName()).matches()) {
                    loadEntry(plugin, loader, element);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    private void loadEntry(Plugin plugin, ClassLoader loader, JarEntry element) {
        Binding binding = Binding.create(plugin, loader, element.getName());
        String name = Files.getNameWithoutExtension(element.getName()).toLowerCase();
        knownClasses.put(name, binding);
        knownClasses.put(String.format("%s:%s", plugin == null ? "bukkit" : plugin.getName().toLowerCase(), name), binding);
    }

    protected void loadEventClass(Plugin plugin, Class<?> clz) {
        Preconditions.checkArgument(isEventClass(clz), clz.getName() + " not valid");
        val label = clz.getSimpleName().toLowerCase();
        val value = new Mapping(label, clz);
        if (!mapping.containsKey(label)) {
            mapping.put(label, value);
        }
        mapping.put((plugin == null ? "bukkit" : plugin.getName().toLowerCase()) + ':' + label, value);
    }

    public void init(String plugin) {
        init(Bukkit.getPluginManager().getPlugin(plugin));
    }

    @SneakyThrows
    public void init(Plugin plugin) {
        if (plugins.add(plugin.getName().toLowerCase())) {
            Class<?> pluginClass = plugin.getClass();
            URL path = pluginClass.getProtectionDomain().getCodeSource().getLocation();
            loadClasses(plugin, pluginClass.getClassLoader(), path, "(.*)\\.class");
        }
    }

    public Object filter(String regex) {// Export to scripts
        List<String> list = new ArrayList<>();
        Pattern p = Pattern.compile(regex);
        mapping.forEach((key, value) -> {
            if (p.matcher(key).matches()) {
                list.add(key);
            }
        });
        return ArrayHelper.toJSArray(list.toArray());
    }

    protected static HandlerList getHandler(Mapping mapping) {
        return getHandler(mapping.clz);
    }

    public static EventMapping get() {
        return INSTANCE;
    }

    private static HandlerList getHandler(Class<?> clz) {
        try {
            Method e = clz.getDeclaredMethod("getHandlerList");
            e.setAccessible(true);
            return (HandlerList) e.invoke(null);
        } catch (NoSuchMethodException e) {
            Class<?> father = clz.getSuperclass();
            if (Event.class.isAssignableFrom(father)) {
                return getHandler(father);
            }
            throw new RuntimeException(e.toString());
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e.toString());
        }
    }

    public static boolean isEventClass(Class<?> clz) {
        return Event.class.isAssignableFrom(clz) && Modifier.isPublic(clz.getModifiers()) && !Modifier.isAbstract(clz.getModifiers());
    }

    @Data
    protected static class Binding {

        private final Plugin plugin;
        private final ClassLoader loader;
        private final String fullName;

        public static Binding create(Plugin plugin, ClassLoader classLoader, String pathName) {
            return new Binding(plugin, classLoader, pathName.replace('/', '.').substring(0, pathName.length() -6));
        }
    }

    public final static class Mapping {

        private final Class<?> clz;
        private final String name;

        private EventListener listener;

        private Mapping(String name, Class<?> clz) {
            this.clz = clz;
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public Class<?> clazz() {
            return clz;
        }

        private EventListener getListener() {
            if (listener == null) {
                listener = new EventListener(this);
            }
            return listener;
        }
    }

}
