package com.mengcraft.script;

import com.google.common.base.Preconditions;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.mengcraft.script.Main.nil;

/**
 * Created on 16-10-17.
 */
public final class ScriptPlugin {

    private final List<HandledListener> listener = new LinkedList<>();
    private final List<HandledTask> task = new LinkedList<>();
    private final File file;

    private final ScriptDescription description;
    private Main main;

    public ScriptPlugin(Main main, File file) {
        this.file = file;
        this.main = main;
        description = new ScriptDescription();
    }

    public List<HandledListener> getListener() {
        return new ArrayList<>(listener);
    }

    public String getDescription(String key) {
        if (description.containsKey(key)) {
            return description.get(key);
        }
        return null;
    }

    public void unload() {
        Preconditions.checkState(!nil(main), "unloaded");
        main.unload(this);
        main = null;
        for (HandledListener i : listener) {
            i.remove();
        }
        for (HandledTask i : task) {
            i.cancel();
        }
    }

    public Player getPlayer(String id) {
        return main.getServer().getPlayerExact(id);
    }

    public Player getPlayer(UUID id) {
        return main.getServer().getPlayer(id);
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

    public void cancel(HandledTask i) {
        Preconditions.checkArgument(i.getPlugin() == this, "unhandled");
        if (task.remove(i)) {
            i.cancel();
        }
    }

    public int run(Runnable runnable) {
        return run(runnable, false);
    }

    public int run(Runnable runnable, boolean b) {
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

    public HandledExecutor addExecutor(String label, ScriptExecutor executor, String permission) {
        throw new UnsupportedOperationException();
    }

    public HandledExecutor addExecutor(String label, ScriptExecutor executor) {
        return addExecutor(label, executor, null);
    }

    public EventMapping getMapping() {
        Preconditions.checkState(!nil(main), "unloaded");
        return EventMapping.INSTANCE;
    }

    public void setDescription(Map<String, Object> in) {
        in.forEach((i, value) -> {
            description.put(i, value.toString());
        });
    }

    public void setDescription(String key, String value) {
        description.put(key, value);
    }

    @Override
    public String toString() {
        return file.getName();
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
