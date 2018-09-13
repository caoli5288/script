package com.mengcraft.script;

import com.google.common.base.Preconditions;
import com.mengcraft.script.loader.ScriptDescription;
import com.mengcraft.script.loader.ScriptLogger;
import com.mengcraft.script.util.$;
import com.mengcraft.script.util.ArrayHelper;
import com.mengcraft.script.util.PluginHelper;
import com.mengcraft.script.util.Reflector;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.permissions.PermissionAttachment;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.mengcraft.script.Main.nil;

/**
 * Created on 16-10-17.
 */
public final class ScriptPlugin {

    private final String id;
    private final ScriptDescription description;
    private final Logger logger;
    private List<HandledPlaceholder> placeholder = new LinkedList<>();
    private List<HandledExecutor> executor = new LinkedList<>();
    private List<HandledListener> listener = new LinkedList<>();
    private List<HandledTask> task = new LinkedList<>();
    private Main main;
    private Runnable unloadHook;

    public ScriptPlugin(Main main, String id) {
        this.id = id;
        this.main = main;
        description = new ScriptDescription();
        logger = new ScriptLogger(main, this);
    }

    public String getDescription(String key) {
        return description.get(key);
    }

    public boolean unload4Idle() {
        return isIdled() && unload();
    }

    public boolean isIdled() {
        Preconditions.checkState(isHandled(), "unloaded");
        return placeholder.isEmpty() && executor.isEmpty() && listener.isEmpty() && task.isEmpty();
    }

    public synchronized boolean unload() {
        if (isHandled()) {
            new ArrayList<>(executor).forEach(HandledExecutor::remove);
            executor = null;
            new ArrayList<>(task).forEach(HandledTask::cancel);
            task = null;
            new ArrayList<>(listener).forEach(HandledListener::remove);
            listener = null;
            new ArrayList<>(placeholder).forEach(HandledPlaceholder::remove);
            placeholder = null;
            main.unload(this);
            main = null;
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

    public boolean isHandled() {
        return !nil(main);
    }

    public Player getPlayer(String id) {
        return main.getServer().getPlayerExact(id);
    }

    public Player getPlayer(UUID id) {
        return main.getServer().getPlayer(id);
    }

    public Collection<?> getAll() {
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

    public boolean depend(String depend, Runnable runnable) {
        return depend(ArrayHelper.link(depend), runnable);
    }

    public boolean depend(List<String> depend, Runnable runnable) {
        val call = DependCall.build(depend, runnable, this);
        return call.call();
    }

    public HandledTask runTask(Runnable runnable) {
        return runTask(runnable, 0, -1, false);
    }

    public HandledTask runTask(@NonNull Runnable runnable, int delay, int period, boolean b) {
        Preconditions.checkState(isHandled(), "unloaded");
        val handled = new HandledTask(this, runnable, period);
        task.add(handled);
        val run = Bukkit.getScheduler();
        handled.setId(b ? run.runTaskTimerAsynchronously(main, handled, delay, period).getTaskId()
                : run.runTaskTimer(main, handled, delay, period).getTaskId());
        return handled;
    }

    public HandledTask runTask(Runnable runnable, int delay, int period) {
        return runTask(runnable, delay, period, false);
    }

    public HandledTask runTask(Runnable runnable, int delay, boolean b) {
        return runTask(runnable, delay, -1, b);
    }

    public HandledTask runTask(Runnable runnable, int delay) {
        return runTask(runnable, delay, -1, false);
    }

    public HandledTask runTask(Runnable runnable, boolean b) {
        return runTask(runnable, 0, -1, b);
    }

    boolean remove(HandledExecutor i) {
        return executor.remove(i) && main.remove(i);
    }

    boolean remove(HandledListener i) {
        return listener.remove(i);
    }

    boolean remove(HandledPlaceholder i) {
        if (placeholder.remove(i)) {
            val map = (Map) Reflector.getField(PlaceholderAPI.class, "placeholders");
            return map.remove(i.getId(), i.getHook());
        }
        return false;
    }

    boolean cancel(HandledTask i) {
        val b = task.remove(i);
        if (b) {
            main.getServer().getScheduler().cancelTask(i.getId());
            i.setId(-1);
        }
        return b;
    }

    public HandledPlaceholder addPlaceholder(String id, HandledPlaceholder.Func func) {
        val hook = new HandledPlaceholder(this, id, func);
        if (PlaceholderAPI.registerPlaceholderHook(id, hook.getHook())) {
            placeholder.add(hook);
            return hook;
        }
        throw new IllegalStateException("id " + id + " conflict");
    }

    public HandledListener addListener(String event, ScriptListener i) {
        return addListener(event, i, -1);
    }

    public HandledListener addListener(String event, ScriptListener i, int priority) {
        Preconditions.checkState(isHandled(), "unloaded");
        EventListener handle = EventMapping.INSTANCE.getListener(event);
        HandledListener add = handle.add(main, this, new Listener(i, priority));
        listener.add(add);
        return add;
    }

    public HandledExecutor addExecutor(String label, ScriptExecutor executor) {
        return addExecutor(label, null, executor);
    }

    public HandledExecutor addExecutor(String label, String permission, ScriptExecutor i) {
        Preconditions.checkArgument(!label.equals("script"));
        HandledExecutor handled = new HandledExecutor(this, new Executor(label, permission, i));
        main.addExecutor(handled);
        executor.add(handled);
        return handled;
    }

    public PermissionAttachment addPermission(Player p, String any) {
        PermissionAttachment attachment = p.addAttachment(main);
        boolean add = true;
        while (any.charAt(0) == '-') {
            any = any.substring(1);
            if (add) {
                add = false;
            }
        }
        attachment.setPermission(any, add);
        return attachment;
    }

    public void sendBossBar(Player p, String text, Map<String, Object> attribute, int tick) {
        sendBossBar(p, getUnsafe().createBossBar(Formatter.format(p, text), attribute), tick);
    }

    public void sendBossBar(Player p, BossBar bar, int tick) {
        $.sendBossBar(p, bar, tick);
    }

    public Main.Unsafe getUnsafe() {
        return main.getUnsafe();
    }

    public void sendBossBar(Player p, String text, int tick) {
        sendBossBar(p, getUnsafe().createBossBar(Formatter.format(p, text)), tick);
    }

    public void setUnloadHook(Runnable unloadHook) {
        this.unloadHook = unloadHook;
    }

    public void broadcast(String... in) {
        for (String i : in) {
            main.getServer().broadcastMessage(ChatColor.translateAlternateColorCodes('&', i));
        }
    }

    @SneakyThrows
    public Class<?> loadType(String type) {
        return main.getClass().getClassLoader().loadClass(type);
    }

    public Logger getLogger() {
        return logger;
    }

    public String format(Player p, String input) {
        return Formatter.format(p, input);
    }

    public EventMapping getMapping() {
        Preconditions.checkState(isHandled(), "unloaded");
        return EventMapping.INSTANCE;
    }

    public void setDescription(Map<String, String> in) {
        Preconditions.checkState(isHandled(), "unloaded");
        in.forEach((i, value) -> {
            if (!description.containsKey(i)) {
                description.put(i, value);
            }
        });
    }

    public boolean setDescription(@NonNull String key, String value) {
        Preconditions.checkState(isHandled(), "unloaded");
        return !description.containsKey(key) && description.put(key, value) == null;
    }

    @Override
    public String toString() {
        return getId();
    }

    public String getId() {
        return id;
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

    @RequiredArgsConstructor
    static class DependCall {

        private final ScriptPlugin plugin;
        private final List<String> depend;
        private final Runnable runner;

        private boolean call() {
            boolean valid = valid();
            if (valid) runner.run();
            return valid;
        }

        private boolean valid() {
            if (depend.isEmpty()) return true;
            val it = depend.iterator();
            while (it.hasNext()) {
                val i = it.next();
                val p = plugin.getUnsafe().getPlugin(i);
                if (!nil(p) && p.isEnabled()) {
                    it.remove();
                }
            }
            return depend.isEmpty();
        }

        private static DependCall build(List<String> depend, Runnable command, ScriptPlugin plugin) {
            return new DependCall(plugin, depend, command);
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

        public EventPriority getEventPriority() {
            if (priority <= Byte.MIN_VALUE) {
                return EventPriority.LOWEST;
            }
            if (priority >= Byte.MAX_VALUE) {
                return EventPriority.MONITOR;
            }
            int idx = ((priority - Byte.MIN_VALUE) >> 6) + 1;
            return EventPriority.values()[idx];
        }

        public ScriptListener getListener() {
            return listener;
        }
    }

}
