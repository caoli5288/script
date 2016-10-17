package com.mengcraft.script;

import com.google.common.collect.ImmutableList;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created on 16-10-17.
 */
public class ScriptPlugin {

    private final List<HandledListener> listener = new LinkedList<>();
    private final List<HandledTask> scheduled = new LinkedList<>();
    private final Main main;
    private final Map<String, Object> description = new HashMap<>();

    public ScriptPlugin(Main main) {
        this.main = main;
    }

    public List<HandledTask> getScheduled() {
        return ImmutableList.copyOf(scheduled);
    }

    public List<HandledListener> getListener() {
        return ImmutableList.copyOf(listener);
    }

    public String getDescription(String key) {
        if (description.containsKey(key)) {
            return description.get(key).toString();
        }
        return null;
    }

    public void shutdown() {
        for (HandledListener i : listener) {
            i.remove();
        }
        for (HandledTask i : scheduled) {
            i.cancel();
        }
        listener.clear();
        scheduled.clear();
    }

    public Player getPlayer(String id) {
        return main.getServer().getPlayerExact(id);
    }

    public Player getPlayer(UUID id) {
        return main.getServer().getPlayer(id);
    }

    public HandledTask schedule(Runnable runnable, int delay, int repeat, boolean thread) {
        HandledTask i;
        if (thread) {
            i = new HandledTask(this, main.execute(runnable, delay, repeat));
        }
        i = new HandledTask(this, main.process(runnable, delay, repeat));
        scheduled.add(i);
        return i;
    }

    public int run(Runnable runnable) {
        return run(runnable, false);
    }

    public int run(Runnable runnable, boolean thread) {
        if (thread) {
            return main.execute(runnable, -1, -1);
        }
        return main.process(runnable, -1, -1);
    }

    public HandledListener addListener(String event, ScriptListener i) {
        return addListener(event, i, -1);
    }

    public HandledListener addListener(String event, ScriptListener i, int priority) {
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
        return EventMapping.INSTANCE;
    }

    public void setDescription(Map<String, Object> in) {
        in.forEach((i, value) -> {
            if (!description.containsKey(i)) description.put(i, value);
        });
    }

    public void setDescription(String key, String value) {
        if (!description.containsKey(key)) description.put(key, value);
    }

    @Override
    public String toString() {
        return description.toString();
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
