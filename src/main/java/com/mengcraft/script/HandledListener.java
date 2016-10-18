package com.mengcraft.script;

/**
 * Created on 16-10-17.
 */
public class HandledListener implements Comparable<HandledListener> {

    private final ScriptListener listener;
    private final EventListener up;
    private final int priority;
    private final ScriptPlugin plugin;

    public HandledListener(EventListener up, ScriptPlugin.Listener listener) {
        this.up = up;
        this.listener = listener.getListener();
        priority = listener.getPriority();
        plugin = listener.getPlugin();
    }

    public boolean remove() {
        return plugin.cancel(this) && up.remove(this);
    }

    public ScriptPlugin getPlugin() {
        return plugin;
    }

    public ScriptListener getListener() {
        return listener;
    }

    public int getPriority() {
        return priority;
    }

    public boolean isHandled() {
        return up.isHandled(this);
    }

    @Override
    public int compareTo(HandledListener i) {
        return priority - i.priority;
    }

}