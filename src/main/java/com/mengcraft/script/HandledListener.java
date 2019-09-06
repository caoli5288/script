package com.mengcraft.script;

import lombok.EqualsAndHashCode;
import org.bukkit.event.EventPriority;

import java.util.Comparator;
import java.util.UUID;

/**
 * Created on 16-10-17.
 */
@EqualsAndHashCode(of = "id")
public class HandledListener {

    private final UUID id = UUID.randomUUID();// Use random id to func
    private final ScriptListener listener;
    private final EventListener managedListener;
    private final int priority;
    private final ScriptPlugin plugin;
    private final EventPriority eventPriority;

    public HandledListener(EventListener managedListener, ScriptPlugin plugin, ScriptPlugin.Listener i) {
        this.managedListener = managedListener;
        this.plugin = plugin;
        listener = i.getListener();
        priority = i.getPriority();
        eventPriority = i.getEventPriority();
    }

    public void remove() {
        if (plugin.remove(this)) {
            managedListener.remove(this);
        }
    }

    public ScriptListener getListener() {
        return listener;
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