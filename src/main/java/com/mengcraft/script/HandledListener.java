package com.mengcraft.script;

import com.mengcraft.script.util.Utils;
import lombok.EqualsAndHashCode;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;

import javax.script.Bindings;
import java.util.Comparator;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Created on 16-10-17.
 */
@EqualsAndHashCode(of = "id")
public class HandledListener {

    private final UUID id = UUID.randomUUID();// Use random id to func
    private final Bindings executor;
    private final EventListener managedListener;
    private final int priority;
    private final ScriptPlugin plugin;
    private final EventPriority eventPriority;

    public HandledListener(EventListener managedListener, ScriptPlugin plugin, Bindings executor, int priority, EventPriority eventPriority) {
        this.managedListener = managedListener;
        this.plugin = plugin;
        this.executor = executor;
        this.priority = priority;
        this.eventPriority = eventPriority;
    }

    public void handle(Event event) {
        long millis = System.currentTimeMillis();
        try {
            Utils.invoke(executor, event);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, String.format("Exception occurred while handle %s\n%s", event.getEventName(), executor.toString()), e);
        } finally {
            millis = System.currentTimeMillis() - millis;
            if (millis >= 5) {
                plugin.getLogger().warning(String.format("Consume too much time to handle %s. (%s millis)\n%s", event.getEventName(), millis, executor.toString()));
            }
        }
    }

    public void remove() {
        if (plugin.remove(this)) {
            managedListener.remove(this);
        }
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