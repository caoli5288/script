package com.mengcraft.script;

import com.google.common.base.Preconditions;
import com.mengcraft.script.util.ArrayHelper;
import com.mengcraft.script.util.Reflector;
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
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

import static com.mengcraft.script.Main.nil;

/**
 * Created on 16-10-17.
 */
public final class EventMapping {

    public static final EventMapping INSTANCE = new EventMapping();
    private final Map<String, Mapping> mapping = new HashMap<>();
    private final Set<String> list = new HashSet<>();

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

    public boolean initialized(String name) {
        return mapping.containsKey(name.toLowerCase());
    }

    public EventListener getListener(String name) {
        String id = name.toLowerCase();
        Preconditions.checkState(mapping.containsKey(id));
        return mapping.get(id).getListener();
    }

    protected int init() {
        URL path = Bukkit.class.getProtectionDomain().getCodeSource().getLocation();
        return init(null, Bukkit.class.getClassLoader(), path, "org/bukkit/event/(.*)/(.*)\\.class");
    }

    private int init(Plugin plugin, ClassLoader loader, URL path, String regex) {
        Preconditions.checkArgument(path.getProtocol().equals("file"));
        Pattern pattern = Pattern.compile(regex);
        int i = mapping.size();
        try {
            JarFile ball = new JarFile(path.getFile());
            Enumeration<JarEntry> all = ball.entries();
            while (all.hasMoreElements()) {
                JarEntry element = all.nextElement();
                if (pattern.matcher(element.getName()).matches()) init(plugin, loader, element);
            }
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
        return mapping.size() - i;
    }

    private void init(Plugin plugin, ClassLoader loader, JarEntry element) {
        try {
            String i = element.getName().replace('/', '.');
            Class<?> clz = loader.loadClass(i.substring(0, i.length() - 6));
            if (valid(clz)) init(plugin, clz);
        } catch (NoClassDefFoundError | ClassNotFoundException i) {
        }
    }

    private static boolean valid(Class<?> clz) {
        return Event.class.isAssignableFrom(clz) && Modifier.isPublic(clz.getModifiers()) && !Modifier.isAbstract(clz.getModifiers());
    }

    public void init(Plugin plugin, Class<?> clz) {
        Preconditions.checkArgument(valid(clz), clz.getName() + " not valid");
        val label = clz.getSimpleName().toLowerCase();
        val value = new Mapping(label, clz);
        if (!mapping.containsKey(label)) {
            mapping.put(label, value);
        }
        mapping.put((plugin == null ? "bukkit" : plugin.getName().toLowerCase()) + ':' + label, value);
    }

    public int init(String plugin) {
        return init(Bukkit.getPluginManager().getPlugin(plugin));
    }

    @SneakyThrows
    public int init(Plugin plugin) {
        if (list.add(plugin.getName().toLowerCase())) {
            Class<?> clz = plugin.getClass();
            URL path = clz.getProtectionDomain().getCodeSource().getLocation();
            URLClassLoader ctx = (URLClassLoader) Main.class.getClassLoader();
            for (URL url : ((URLClassLoader) plugin.getClass().getClassLoader()).getURLs()) {
                Reflector.invoke(URLClassLoader.class, ctx, "addURL", url);
            }
            return init(plugin, clz.getClassLoader(), path, "(.*)\\.class");
        }
        return 0;
    }

    public Object filter(String regex) {
        List<String> list = new ArrayList<>();
        Pattern p = Pattern.compile(regex);
        mapping.forEach((key, value) -> {
            if (p.matcher(key).matches()) {
                list.add(key);
            }
        });
        return ArrayHelper.toJSArray(list.toArray());
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
            if (nil(listener)) {
                listener = new EventListener(this);
            }
            return listener;
        }
    }

}
