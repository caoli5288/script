package com.mengcraft.script;

import com.google.common.base.Preconditions;
import com.mengcraft.script.loader.ScriptDescription;
import com.mengcraft.script.loader.ScriptLoader;
import com.mengcraft.script.loader.ScriptLogger;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import javax.script.ScriptEngine;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.mengcraft.script.Main.nil;

/**
 * Created on 16-10-17.
 */
public final class ScriptPlugin {

    private final Unsafe unsafe = new Unsafe() {
        public Plugin getPlugin(String id) {
            return main.getServer().getPluginManager().getPlugin(id);
        }

        public ScriptEngine getScript(String id) {
            ScriptLoader.ScriptBinding binding = main.getScript(id);
            return !nil(binding) ? binding.getEngine() : null;
        }
    };

    private List<HandledListener> listener = new LinkedList<>();
    private List<HandledTask> task = new LinkedList<>();

    private final String id;
    private final ScriptDescription description;
    private final Logger logger;

    private Main main;
    private Runnable unloadHook;

    public ScriptPlugin(Main main, String id) {
        this.id = id;
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
        if (isLoaded()) {
            task.forEach((i) -> i.cancel());
            listener.forEach((i) -> i.remove());
            main.unload(this);
            main = null;
            task = null;
            listener = null;
            if (!nil(unloadHook)) {
                try {
                    unloadHook.run();
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Error while script unload", e);
                }
                unloadHook = null;
            }
        }
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

    public void runCommand(String str) {
        logger.info("Run console command " + str);
        main.getServer().dispatchCommand(main.getServer().getConsoleSender(), str);
    }

    public HandledTask schedule(Runnable runnable, int delay, int period, boolean b) {
        Preconditions.checkState(isLoaded(), "unloaded");
        HandledTask i = new HandledTask(this);
        // may the only way remove one-shot task
        Runnable r = period < 0 ? () -> callback(i, runnable) : runnable;
        i.setId(b ? main.execute(r, delay, period) : main.process(r, delay, period));
        task.add(i);
        return i;
    }

    public HandledTask schedule(Runnable runnable, int delay, int period) {
        return schedule(runnable, delay, period, false);
    }

    public HandledTask schedule(Runnable runnable, int delay, boolean b) {
        return schedule(runnable, delay, -1, b);
    }

    public HandledTask schedule(Runnable runnable, int delay) {
        return schedule(runnable, delay, -1, false);
    }

    public HandledTask schedule(Runnable runnable, boolean b) {
        return schedule(runnable, -1, -1, b);
    }

    public HandledTask schedule(Runnable runnable) {
        return schedule(runnable, -1, -1, false);
    }

    private Runnable callback(HandledTask i, Runnable runnable) {
        return () -> {
            try {
                runnable.run();
            } catch (Exception e) {
                logger.log(Level.WARNING, e.getMessage(), e);
            }
            i.cancel();
        };
    }

    protected boolean cancel(HandledTask i) {
        boolean b = task.remove(i);
        if (b) {
            main.getServer().getScheduler().cancelTask(i.getId());
            i.setId(-1);
        }
        return b;
    }

    protected boolean cancel(HandledListener i) {
        return listener.remove(i);
    }

    public HandledListener addListener(String event, ScriptListener i) {
        return addListener(event, i, -1);
    }

    public HandledListener addListener(String event, ScriptListener i, int priority) {
        Preconditions.checkState(isLoaded(), "unloaded");
        EventListener handle = EventMapping.INSTANCE.getListener(event);
        HandledListener add = handle.add(main, new Listener(this, i, priority));
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
        Preconditions.checkState(isLoaded(), "unloaded");
        return EventMapping.INSTANCE;
    }

    public void setDescription(Map<String, Object> in) {
        Preconditions.checkState(isLoaded(), "unloaded");
        in.forEach((i, value) -> {
            description.put(i, value.toString());
        });
    }

    public void setDescription(String key, String value) {
        Preconditions.checkState(isLoaded(), "unloaded");
        description.put(key, value);
    }

    @Override
    public String toString() {
        return id;
    }

    public interface Unsafe {
        Plugin getPlugin(String id);

        ScriptEngine getScript(String id);
    }

    public static class Listener {
        private final ScriptPlugin plugin;
        private final ScriptListener listener;
        private final int priority;

        private Listener(ScriptPlugin plugin, ScriptListener listener, int priority) {
            this.plugin = plugin;
            this.listener = listener;
            this.priority = priority;
        }

        public ScriptPlugin getPlugin() {
            return plugin;
        }

        public int getPriority() {
            return priority;
        }

        public ScriptListener getListener() {
            return listener;
        }
    }

}
