package com.mengcraft.script;

import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.RegisteredListener;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Created on 16-10-17.
 */
public class EventListener implements Listener {

    private final List<HandledListener> list = new ArrayList<>();
    private final HandlerList handler;
    private final EventMapping.Mapping mapping;

    public EventListener(EventMapping.Mapping mapping) {
        handler = EventMapping.getHandler(mapping);
        this.mapping = mapping;
    }

    public void handle(Event event) {
        if (mapping.valid(event)) {
            int size = list.size();
            for (int i = 0; i < size; i++) {
                handle(list.get(i), event);
            }
        }
    }

    private void handle(HandledListener listener, Event event) {
        try {
            listener.getListener().handle(event);
        } catch (Exception e) {
            listener.getPlugin().getLogger().log(Level.SEVERE, getName(), e);
        }
    }

    protected boolean remove(HandledListener listener) {
        if (list.remove(listener)) {
            if (list.isEmpty()) {
                handler.unregister(this);
            }
            return true;
        }
        return false;
    }

    public HandledListener add(Main main, ScriptPlugin plugin, ScriptPlugin.Listener listener) {
        HandledListener handled = new HandledListener(this, plugin, listener);
        if (list.isEmpty()) {
            handler.register(new RegisteredListener(this, (i, event) ->
                    handle(event),
                    EventPriority.NORMAL, main, false)
            );
            list.add(handled);
        } else {
            add(handled, listener.getPriority());
        }
        return handled;
    }

    private void add(HandledListener handled, int priority) {
        int size = list.size();
        int i = 0;
        for (; i < size; i++) {
            if (list.get(i).getPriority() > priority) {
                size = 0;
            }
        }
        list.add(i, handled);
    }

    public String getName() {
        return mapping.getName();
    }

}
