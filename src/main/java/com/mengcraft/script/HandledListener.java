package com.mengcraft.script;

import lombok.EqualsAndHashCode;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;

import java.util.Comparator;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Created on 16-10-17.
 */
@EqualsAndHashCode(of = "id")
public class HandledListener {

    private final UUID id = UUID.randomUUID();// Use random id to func
    private final Consumer<Event> executor;
    private final EventListener managedListener;
    private final int priority;
    private final ScriptPlugin plugin;
    private final EventPriority eventPriority;

    public HandledListener(EventListener managedListener, ScriptPlugin plugin, Consumer<Event> executor, int priority, EventPriority eventPriority) {
        this.managedListener = managedListener;
        this.plugin = plugin;
        this.executor = executor;
        this.priority = priority;
        this.eventPriority = eventPriority;
    }

    public void remove() {
        if (plugin.remove(this)) {
            managedListener.remove(this);
        }
    }

    public Consumer<Event> getExecutor() {
        return executor;
    }

    public ScriptPlugin getPlugin() {
        return plugin;
    }

    public EventPriority getEventPriority() {
        return eventPriority;
    }

    public int priority() {
        return priority;
    }

    public static Comparator<HandledListener> comparator() {
        return Comparator.comparingInt(HandledListener::priority);
    }

}