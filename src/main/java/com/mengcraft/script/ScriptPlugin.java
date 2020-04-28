package com.mengcraft.script;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.mengcraft.script.loader.ScriptDescription;
import com.mengcraft.script.loader.ScriptLogger;
import com.mengcraft.script.util.Utils;
import com.mengcraft.script.util.BossBarWrapper;
import com.mengcraft.script.util.Named;
import com.mengcraft.script.util.Reflector;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.permissions.PermissionAttachment;

import javax.script.Bindings;
import java.io.Closeable;
import java.io.File;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created on 16-10-17.
 */
public final class ScriptPlugin implements Named, Closeable {

    private final String id;
    private final ScriptDescription description;
    private final Logger logger;
    private LinkedList<HandledPlaceholder> placeholders = new LinkedList<>();
    private LinkedList<HandledCommand> commands = new LinkedList<>();
    private LinkedList<HandledListener> listeners = new LinkedList<>();
    private LinkedList<HandledTask> tasks = new LinkedList<>();
    private ScriptBootstrap main;
    private Runnable unloadHook;

    public ScriptPlugin(ScriptBootstrap main, String id) {
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
        return placeholders.isEmpty() && commands.isEmpty() && listeners.isEmpty() && tasks.isEmpty();
    }

    public synchronized boolean unload() {
        if (isHandled()) {
            while (!commands.isEmpty()) commands.peek().remove();
            commands = null;
            while (!tasks.isEmpty()) tasks.peek().cancel();
            tasks = null;
            while (!listeners.isEmpty()) listeners.peek().remove();
            listeners = null;
            while (!placeholders.isEmpty()) placeholders.peek().remove();
            placeholders = null;
            main.unload(this);
            main = null;
            if (unloadHook != null) {
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
        return main != null;
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

    public void runCommand(Player p, String command) {
        p.chat("/" + Formatter.format(p, command));
    }

    public boolean depend(String depend, Runnable runnable) {
        return depend(Lists.newArrayList(depend), runnable);
    }

    public boolean depend(List<String> depend, Runnable runnable) {
        val call = DependCall.build(depend, runnable, this);
        return call.call();
    }

    public HandledTask runTask(Bindings invokable) {
        return runTask(invokable, 0, -1, false);
    }

    public HandledTask runTask(@NonNull Bindings invokable, int delay, int period, boolean b) {
        Preconditions.checkState(isHandled(), "unloaded");
        HandledTask task = new HandledTask(this, invokable, period);
        tasks.add(task);
        task.setId(b ? Bukkit.getScheduler().runTaskTimerAsynchronously(main, task, delay, period).getTaskId()
                : Bukkit.getScheduler().runTaskTimer(main, task, delay, period).getTaskId());
        return task;
    }

    public HandledTask runTask(Bindings invokable, int delay, int period) {
        return runTask(invokable, delay, period, false);
    }

    public HandledTask runTask(Bindings invokable, int delay, boolean b) {
        return runTask(invokable, delay, -1, b);
    }

    public HandledTask runTask(Bindings invokable, int delay) {
        return runTask(invokable, delay, -1, false);
    }

    public HandledTask runTask(Bindings invokable, boolean b) {
        return runTask(invokable, 0, -1, b);
    }

    boolean remove(HandledCommand i) {
        return commands.remove(i) && main.remove(i);
    }

    boolean remove(HandledListener i) {
        return listeners.remove(i);
    }

    boolean remove(HandledPlaceholder i) {
        if (placeholders.remove(i)) {
            val map = Utils.as(Reflector.getField(PlaceholderAPI.class, "placeholders"), Map.class);
            return map.remove(i.getId(), i.getHook());
        }
        return false;
    }

    boolean cancel(HandledTask i) {
        val b = tasks.remove(i);
        if (b) {
            main.getServer().getScheduler().cancelTask(i.getId());
            i.setId(-1);
        }
        return b;
    }

    public HandledPlaceholder addPlaceholder(String id, HandledPlaceholder.Func func) {
        val hook = new HandledPlaceholder(this, id, func);
        if (PlaceholderAPI.registerPlaceholderHook(id, hook.getHook())) {
            placeholders.add(hook);
            return hook;
        }
        throw new IllegalStateException("id " + id + " conflict");
    }

    public HandledListener addListener(String event, Consumer<Event> i) {
        return addListener(event, i, -1);
    }

    public HandledListener addListener(String eventName, Consumer<Event> executor, int priority) {
        Preconditions.checkState(isHandled(), "unloaded");
        EventListener handle = EventMapping.INSTANCE.getListener(eventName);
        HandledListener add = handle.add(this, executor, priority);
        listeners.add(add);
        return add;
    }

    public HandledCommand addExecutor(String label, BiConsumer<CommandSender, Bindings> executor) {
        return addExecutor(label, null, executor);
    }

    public HandledCommand addExecutor(String label, String permission, BiConsumer<CommandSender, Bindings> executor) {
        Preconditions.checkArgument(!label.equals("script"));
        HandledCommand handled = new HandledCommand(this, label, executor, permission);
        main.addExecutor(handled);
        commands.add(handled);
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

    public void sendBossBar(Player p, BossBarWrapper bar, int tick) {
        Utils.sendBossBar(p, bar, tick);
    }

    public ScriptBootstrap.Unsafe getUnsafe() {
        return main.getUnsafe();
    }

    public void sendBossBar(Player p, String text, int tick) {
        sendBossBar(p, getUnsafe().createBossBar(Formatter.format(p, text)), tick);
    }

    public void setMetadata(Player player, String key, Object value) {
        player.setMetadata(key, new FixedMetadataValue(main, value));
    }

    public Object getMetadata(Player player, String key) {
        for (MetadataValue metadataValue : player.getMetadata(key)) {
            if (metadataValue.getOwningPlugin() == main) {
                return metadataValue.value();
            }
        }
        return null;
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

    public Object require(String path) {
        return ScriptBootstrap.require(new File(ScriptBootstrap.get().getDataFolder(), path));
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

    @Override
    public String getId() {
        return id;
    }

    //===== Internal


    @Override
    public void close() {
        unload();
    }

    @Override
    public String getName() {
        return getDescription("name");
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
                if (p != null && p.isEnabled()) {
                    it.remove();
                }
            }
            return depend.isEmpty();
        }

        private static DependCall build(List<String> depend, Runnable command, ScriptPlugin plugin) {
            return new DependCall(plugin, depend, command);
        }

    }
}
