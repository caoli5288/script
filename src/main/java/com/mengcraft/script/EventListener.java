package com.mengcraft.script;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import lombok.val;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.RegisteredListener;

import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.logging.Level;

/**
 * Created on 16-10-17.
 */
public class EventListener implements Listener {

    private final EnumMap<EventPriority, RegisteredListener> actually = Maps.newEnumMap(EventPriority.class);
    private final ArrayListMultimap<EventPriority, HandledListener> handled = ArrayListMultimap.create();
    private final Class<?> clz;
    private final String name;
    private final HandlerList loop;

    public EventListener(EventMapping.Mapping mapping) {
        clz = mapping.clazz();
        name = mapping.getName();
        loop = EventMapping.getHandler(mapping);
    }

    public void handle(Event event, EventPriority priority) {
        if (clz == event.getClass()) {
            for (HandledListener l : handled.get(priority)) {
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

    protected boolean remove(HandledListener listener) {
        EventPriority priority = listener.getEventPriority();
        val container = handled.get(priority);
        boolean result = container.remove(listener);
        if (result && container.isEmpty()) {
            loop.unregister(actually.remove(priority));
        }
        return result;
    }

    public HandledListener add(Main main, ScriptPlugin plugin, ScriptPlugin.Listener listener) {
        HandledListener output = new HandledListener(this, plugin, listener);
        EventPriority priority = listener.getEventPriority();
        val container = handled.get(priority);
        if (container.isEmpty()) {
            RegisteredListener actuallyListener = new RegisteredListener(this,
                    (i, event) -> handle(event, priority),
                    priority,
                    main,
                    false);
            actually.put(priority, actuallyListener);
            /*
             * Add to handled list before actually register event listener.
             */
            add(container, output);
            loop.register(actuallyListener);
        } else {
            add(container, output);
        }
        return output;
    }

    private void add(List<HandledListener> container, HandledListener element) {
        int idx = Collections.binarySearch(container, element, Comparator.comparingInt(HandledListener::priority));
        if (idx > -1) {
            container.add(idx, element);
        } else {
            container.add(-(idx) - 1, element);
        }
    }

    public String name() {
        return name;
    }

}
