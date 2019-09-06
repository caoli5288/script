package com.mengcraft.script;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredListener;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.logging.Level;

/**
 * Created on 16-10-17.
 */
public class EventListener implements Listener {

    private final EnumMap<EventPriority, Handled> handledExecutors = Maps.newEnumMap(EventPriority.class);
    private final Class<?> clz;
    private final String name;
    private final HandlerList handlerList;

    public EventListener(EventMapping.Mapping mapping) {
        clz = mapping.clazz();
        name = mapping.getName();
        handlerList = EventMapping.getHandler(mapping);
    }

    public void handle(Event event, EventPriority priority) {
        if (clz == event.getClass()) {
            for (HandledListener l : handledExecutors.get(priority).executors) {
                handle(l, event);
            }
        }
    }

    private void handle(HandledListener listener, Event event) {
        try {
            listener.getListener().handle(event);
        } catch (Exception e) {
            listener.getPlugin().getLogger().log(Level.SEVERE, name(), e);
        }
    }

    protected void remove(HandledListener listener) {
        EventPriority priority = listener.getEventPriority();
        Handled handled = handledExecutors.get(priority);
        if (handled.remove(listener) == 0) {
            handlerList.unregister(handled);
        }
    }

    public HandledListener add(ScriptBootstrap main, ScriptPlugin plugin, ScriptPlugin.Listener listener) {
        Handled handled = handledExecutors.computeIfAbsent(listener.getEventPriority(), priority -> new Handled(this,
                (_this, event) -> handle(event, priority),
                priority,
                main,
                false));
        if (handled.isEmpty()) {
            handlerList.register(handled);
        }
        HandledListener output = new HandledListener(this, plugin, listener);
        handled.add(output);
        return output;
    }

    public String name() {
        return name;
    }

    protected static class Handled extends RegisteredListener {

        private final List<HandledListener> executors = Lists.newArrayList();

        public Handled(Listener listener, EventExecutor executor, EventPriority priority, Plugin plugin, boolean ignoreCancelled) {
            super(listener, executor, priority, plugin, ignoreCancelled);
        }

        public int add(HandledListener e) {
            int index = Collections.binarySearch(executors, e, HandledListener.comparator());
            executors.add(index <= -1 ? -(index) - 1 : index, e);
            return executors.size();
        }

        /**
         * @return Handled executors size
         */
        public int remove(HandledListener e) {
            executors.remove(e);
            return executors.size();
        }

        public boolean isEmpty() {
            return executors.isEmpty();
        }
    }

}
