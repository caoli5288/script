package com.mengcraft.script;

import com.google.common.base.Preconditions;
import com.mengcraft.script.loader.ScriptDescription;
import com.mengcraft.script.loader.ScriptLoader;
import com.mengcraft.script.loader.ScriptLogger;
import com.mengcraft.script.util.ArrayHelper;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

import javax.script.ScriptEngine;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
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
        public Server getServer() {
            return main.getServer();
        }

        public Plugin getPlugin(String id) {
            return main.getServer().getPluginManager().getPlugin(id);
        }

        public ScriptEngine getScript(String id) {
            ScriptLoader.ScriptBinding binding = main.getScript(id);
            return !nil(binding) ? binding.getEngine() : null;
        }
    };

    private List<HandledExecutor> executor = new LinkedList<>();
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

    public boolean unload() {
        if (isLoaded()) {
            executor.forEach(i -> i.remove());
            task.forEach(i -> i.cancel());
            listener.forEach(i -> i.remove());
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
            return true;
        }
        return false;
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

    public void depend(String depend, Runnable runnable) {
        depend(ArrayHelper.link(depend), runnable);
    }

    public void depend(List<String> depend, Runnable runnable) {
        DependCall call = DependCall.build(depend, runnable, this);
        if (!call.call()) {
            schedule(call);
        }
    }

    public HandledTask schedule(Runnable run, int delay, int period, boolean b) {
        Preconditions.checkState(isLoaded(), "unloaded");
        BukkitScheduler scheduler = main.getServer().getScheduler();
        HandledTask handled = new HandledTask(this);
        BukkitTask i;
        if (b) {
            i = scheduler.runTaskTimerAsynchronously(main, valid(run, period, handled), delay, period);
        } else {
            i = scheduler.runTaskTimer(main, valid(run, period, handled), delay, period);
        }
        handled.setId(i.getTaskId());
        task.add(handled);
        return handled;
    }

    private Runnable valid(Runnable run, int period, HandledTask handled) {
        return period > -1 ? run : handle(handled, run);
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
        return schedule(runnable, 0, -1, b);
    }

    public HandledTask schedule(Runnable runnable) {
        return schedule(runnable, 0, -1, false);
    }

    private Runnable handle(HandledTask i, Runnable runnable) {
        return () -> {
            try {
                runnable.run();
            } catch (Exception e) {
                logger.log(Level.WARNING, e.getMessage(), e);
            }
            i.cancel();
        };
    }

    protected boolean remove(HandledExecutor i) {
        return executor.remove(i) && main.remove(i);
    }

    protected boolean remove(HandledListener i) {
        return listener.remove(i);
    }

    protected boolean cancel(HandledTask i) {
        boolean b = task.remove(i);
        if (b) {
            main.getServer().getScheduler().cancelTask(i.getId());
            i.setId(-1);
        }
        return b;
    }

    public HandledListener addListener(String event, ScriptListener i) {
        return addListener(event, i, -1);
    }

    public HandledListener addListener(String event, ScriptListener i, int priority) {
        Preconditions.checkState(isLoaded(), "unloaded");
        EventListener handle = EventMapping.INSTANCE.getListener(event);
        HandledListener add = handle.add(main, this, new Listener(i, priority));
        listener.add(add);
        return add;
    }

    public HandledExecutor addExecutor(String label, String permission, ScriptExecutor i) {
        Preconditions.checkArgument(!label.equals("script"));
        HandledExecutor handled = new HandledExecutor(this, new Executor(label, permission, i));
        main.addExecutor(handled);
        executor.add(handled);
        return handled;
    }

    public HandledExecutor addExecutor(String label, ScriptExecutor executor) {
        return addExecutor(label, null, executor);
    }

    public void setUnloadHook(Runnable unloadHook) {
        this.unloadHook = unloadHook;
    }

    public void broadcast(String... in) {
        for (String i : in) {
            main.getServer().broadcastMessage(ChatColor.translateAlternateColorCodes('&', i));
        }
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
        Server getServer();

        Plugin getPlugin(String id);

        ScriptEngine getScript(String id);
    }

    public static class Executor {

        private final String permission;
        private final String label;
        private final ScriptExecutor executor;

        public Executor(String label, String permission, ScriptExecutor executor) {
            this.label = label;
            this.permission = permission;
            this.executor = executor;
        }

        public String getPermission() {
            return permission;
        }

        public String getLabel() {
            return label;
        }

        public ScriptExecutor getExecutor() {
            return executor;
        }
    }

    public static class DependCall implements Runnable {
        private final List<String> depend;
        private Runnable command;
        private ScriptPlugin plugin;

        private DependCall(List<String> depend) {
            this.depend = depend;
        }

        @Override
        public void run() {
            if (!call()) {
                plugin.logger.info("Ignore depend call with " + depend + " not found");
            }
        }

        public boolean call() {
            boolean result = validata();
            if (result) {
                try {
                    command.run();
                } catch (Exception e) {
                    plugin.logger.log(Level.SEVERE, "", e);
                }
            }
            return result;
        }

        public boolean validata() {
            boolean result = depend.isEmpty();
            if (!result) {
                Iterator<String> it = depend.iterator();
                String i;
                Plugin p;
                while (it.hasNext()) {
                    i = it.next();
                    p = plugin.unsafe.getPlugin(i);
                    if (!Main.nil(p) && p.isEnabled()) {
                        it.remove();
                    }
                }
                result = depend.isEmpty();
            }
            return result;
        }

        private static DependCall build(List<String> depend, Runnable command, ScriptPlugin plugin) {
            DependCall chain = new DependCall(depend);
            chain.command = command;
            chain.plugin = plugin;
            return chain;
        }
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
