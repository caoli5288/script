package com.mengcraft.script;

import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.RegisteredListener;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Created on 16-10-17.
 */
public class EventListener implements Listener {

    private final List<HandledListener> list = new LinkedList<>();
    private final HandlerList handler;
    private final String name;
    private ScriptListener[] pipe;

    public EventListener(EventMapping.Mapping mapping) {
        handler = EventMapping.getHandler(mapping);
        name = mapping.getName();
    }

    public void handle(Event event) {
        for (ScriptListener listener : pipe) {
            listener.handle(event);
        }
    }

    public boolean isHandled(HandledListener listener) {
        return list.contains(listener);
    }

    protected boolean remove(HandledListener listener) {
        if (list.remove(listener)) {
            if (list.isEmpty()) {
                handler.unregister(this);
            } else {
                pipe = Main.collect(ScriptListener.class, list, i -> i.getListener());
            }
            return true;
        }
        return false;
    }

    public HandledListener add(Main main, ScriptPlugin.Listener listener) {
        HandledListener handled = new HandledListener(this, listener);
        if (list.isEmpty()) {
            handler.register(new RegisteredListener(this, (i, event) ->
                    handle(event),
                    EventPriority.NORMAL,
                    main, false));
        }
        add(handled, listener.getPriority());

        pipe = Main.collect(ScriptListener.class, list, i -> i.getListener());

        return handled;
    }

    private void add(HandledListener handled, int priority) {
        if (list.isEmpty()) {
            list.add(handled);
        } else {
            Iterator<HandledListener> it = list.iterator();
            int index = -1;
            for (int i = 0; index < 0 && it.hasNext(); i++) {
                if (priority < it.next().getPriority()) index = i;
            }
            if (index < 0) {
                list.add(handled);
            } else {
                list.add(index, handled);
            }
        }
    }

    public String getName() {
        return name;
    }

}
