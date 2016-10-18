package com.mengcraft.script;

import com.google.common.base.Preconditions;
import com.mengcraft.script.loader.ScriptDescription;
import com.mengcraft.script.loader.ScriptLogger;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import static com.mengcraft.script.Main.nil;

/**
 * Created on 16-10-17.
 */
public final class ScriptPlugin {

    private final Unsafe unsafe = new Unsafe() {
        public Plugin getJavaPlugin(String id) {
            return main.getServer().getPluginManager().getPlugin(id);
        }

        public ScriptPlugin getPlugin(String id) {
            return main.getPlugin(id);
        }
    };

    private List<HandledListener> listener = new LinkedList<>();
    private List<HandledTask> task = new LinkedList<>();

    private final ScriptDescription description;
    private final File file;
    private final Logger logger;

    private Main main;
    private Runnable unloadHook;

    public ScriptPlugin(Main main, File file) {
        this.file = file;
        this.main = main;
        description = new ScriptDescription();
        logger = new ScriptLogger(main, this);
    }

    public List<HandledListener> getListener() {
        return new ArrayList<>(listener);
    }

    public String getDescription(String key) {
        return description.get(key);
    }

    public boolean isLoaded() {
        return !nil(main);
    }

    public void unload() {
        Preconditions.checkState(!nil(main), "unloaded");
        main.unload(this);
        main = null;
        if (!nil(unloadHook)) unloadHook.run();
        task.forEach(HandledTask::cancel);
        listener.forEach(HandledListener::remove);
        task = null;
        listener = null;
    }

    public Unsafe getUnsafe() {
        return unsafe;
    }

    public Player getPlayer(String id) {
        return main.getServer().getPlayerExact(id);
    }

    public Player getPlayer(UUID id) {
        return main.getServer().getPlayer(id);
    }

    public Collection<?> getOnlineList() {
        return main.getServer().getOnlinePlayers();
    }

    public Object getService(String id) {
        Collection<Class<?>> set = main.getServer().getServicesManager().getKnownServices();
        for (Class<?> i : set) {
            if (i.getSimpleName().equals(id)) return getService(i);
        }
        return null;
    }

    public Object getService(Class<?> i) {
        return main.getServer().getServicesManager().load(i);
    }

    public HandledTask schedule(Runnable runnable, int delay, int period, boolean b) {
        Preconditions.checkState(!nil(main), "unloaded");
        HandledTask i;
        if (b) {
            i = new HandledTask(this, main.execute(runnable, delay, period));
        }
        i = new HandledTask(this, main.process(runnable, delay, period));
        task.add(i);
        return i;
    }

    public HandledTask schedule(Runnable runnable, int delay) {
        return schedule(runnable, delay, -1, false);
    }

    public boolean cancel(HandledTask i) {
        Preconditions.checkArgument(i.getPlugin() == this, "unhandled");
        boolean result = task.remove(i);
        if (result) {
            main.getServer().getScheduler().cancelTask(i.getId());
        }
        return result;
    }

    public boolean isCancelled(HandledTask i) {
        Preconditions.checkArgument(i.getPlugin() == this, "unhandled");
        return task.contains(i);
    }

    public int run(Runnable runnable) {
        return run(runnable, false);
    }

    public int run(Runnable runnable, boolean b) {
        Preconditions.checkState(!nil(main), "unloaded");
        if (b) {
            return main.execute(runnable, -1, -1);
        }
        return main.process(runnable, -1, -1);
    }

    public HandledListener addListener(String event, ScriptListener i) {
        return addListener(event, i, -1);
    }

    public HandledListener addListener(String event, ScriptListener i, int priority) {
        Preconditions.checkState(!nil(main), "unloaded");
        EventListener handle = EventMapping.INSTANCE.getListener(event);
        HandledListener add = handle.add(main, new Listener(i, priority));
        listener.add(add);
        return add;
    }

    public HandledExecutor addExecutor(String label, String permission, ScriptExecutor executor) {
        throw new UnsupportedOperationException();
    }

    public HandledExecutor addExecutor(String label, ScriptExecutor executor) {
        return addExecutor(label, null, executor);
    }

    public void setUnloadHook(Runnable unloadHook) {
        this.unloadHook = unloadHook;
    }

    public Logger getLogger() {
        return logger;
    }

    public EventMapping getMapping() {
        Preconditions.checkState(!nil(main), "unloaded");
        return EventMapping.INSTANCE;
    }

    public void setDescription(Map<String, Object> in) {
        Preconditions.checkState(!nil(main), "unloaded");
        in.forEach((i, value) -> {
            description.put(i, value.toString());
        });
    }

    public void setDescription(String key, String value) {
        Preconditions.checkState(!nil(main), "unloaded");
        description.put(key, value);
    }

    @Override
    public String toString() {
        return file.getName();
    }

    public interface Unsafe {
        Plugin getJavaPlugin(String id);

        ScriptPlugin getPlugin(String id);
    }

    public static class Listener {
        private final ScriptListener listener;
        private final int priority;

        private Listener(ScriptListener listener, int priority) {
            this.listener = listener;
            this.priority = priority;
        }

        public int getPriority() {
            return priority;
        }

        public ScriptListener getListener() {
            return listener;
        }
    }

}
