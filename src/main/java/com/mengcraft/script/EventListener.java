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

    public static class HandledListener implements Comparable<HandledListener> {

        private final ScriptListener listener;
        private final EventListener up;
        private int priority;

        private HandledListener(EventListener up, ScriptListener listener, int priority) {
            this.up = up;
            this.listener = listener;
            this.priority = priority;
        }

        public boolean remove() {
            return up.remove(this);
        }

        public int getPriority() {
            return priority;
        }

        public void setPriority(int priority) {
            this.priority = priority;
        }

        @Override
        public int compareTo(HandledListener i) {
            return priority - i.priority;
        }

    }

    private final List<HandledListener> list = new LinkedList<>();
    private final HandlerList handler;
    private ScriptListener[] pipe;

    public EventListener(EventMapping.Binding binding) {
        handler = EventMapping.getHandler(binding);
    }

    public void handle(Event event) {
        for (ScriptListener listener : pipe) {
            listener.handle(event);
        }
    }

    protected void shutdown() {
        list.clear();
        handler.unregister(this);
    }

    public boolean remove(HandledListener listener) {
        if (list.remove(listener)) {
            if (list.isEmpty()) {
                handler.unregister(this);
            } else {
                pipe = Main.collect(ScriptListener.class, list, i -> i.listener);
            }
            return true;
        }
        return false;
    }

    public HandledListener addListener(Main main, ScriptPlugin plugin, ScriptListener listener, int priority) {
        HandledListener handled = new HandledListener(this, listener, priority);
        if (list.isEmpty()) {
            handler.register(new RegisteredListener(this, (i, event) ->
                    handle(event),
                    EventPriority.NORMAL,
                    main, false));
        }
        add(handled, priority);
        plugin.getHandled().add(handled);

        pipe = Main.collect(ScriptListener.class, list, i -> i.listener);

        return handled;
    }

    private void add(HandledListener handled, int priority) {
        if (list.isEmpty()) {
            list.add(handled);
        } else {
            Iterator<HandledListener> it = list.iterator();
            int index = -1;
            for (int i = 0; index < 0 && it.hasNext(); i++) {
                if (priority < it.next().priority) index = i;
            }
            if (index < 0) {
                list.add(handled);
            } else {
                list.add(index, handled);
            }
        }
    }

}
