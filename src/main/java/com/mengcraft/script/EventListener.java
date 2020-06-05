package com.mengcraft.script;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mengcraft.script.util.Utils;
import lombok.Getter;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.RegisteredListener;

import javax.script.Bindings;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;

/**
 * Created on 16-10-17.
 */
public class EventListener implements Listener {

    private final EnumMap<EventPriority, CustomRegisteredListener> handledExecutors = Maps.newEnumMap(EventPriority.class);
    private final Class<?> clz;
    private final String name;
    @Getter
    private final HandlerList handlerList;

    public EventListener(EventMapping.Mapping mapping) {
        clz = mapping.clazz();
        name = mapping.getName();
        handlerList = EventMapping.getHandler(mapping);
    }

    EventListener(Class<?> clz, String name, HandlerList handlerList) {
        this.clz = clz;
        this.name = name;
        this.handlerList = handlerList;
    }

    protected void remove(HandledListener listener) {
        EventPriority priority = listener.getEventPriority();
        CustomRegisteredListener custom = handledExecutors.get(priority);
        custom.remove(listener);
        if (custom.isEmpty()) {
            handlerList.unregister(custom);
        }
    }

    public HandledListener add(ScriptPlugin script, Bindings executor, int order) {
        CustomRegisteredListener custom = handledExecutors.computeIfAbsent(Utils.getEventPriority(order),
                priority -> new CustomRegisteredListener(priority, false));
        if (custom.isEmpty()) {
            handlerList.register(custom);
        }
        HandledListener output = new HandledListener(this,
                script,
                executor,
                order,
                custom.getPriority());
        custom.add(output);
        return output;
    }

    public String name() {
        return name;
    }

    public Listener asListener() {
        return this;
    }

    private class CustomRegisteredListener extends RegisteredListener {

        private final List<HandledListener> executors = Lists.newArrayList();

        private CustomRegisteredListener(EventPriority priority, boolean ignoreCancelled) {
            super(asListener(), null, priority, ScriptBootstrap.get(), ignoreCancelled);
        }

        @Override
        public void callEvent(Event event) {
            if (clz == event.getClass()) {
                for (HandledListener listener : executors) {
                    listener.handle(event);
                }
            }
        }

        private void add(HandledListener e) {
            int index = Collections.binarySearch(executors, e, HandledListener.comparator());
            executors.add(index <= -1 ? -(index) - 1 : index, e);
        }

        public void remove(HandledListener e) {
            executors.remove(e);
        }

        public boolean isEmpty() {
            return executors.isEmpty();
        }
    }

}
