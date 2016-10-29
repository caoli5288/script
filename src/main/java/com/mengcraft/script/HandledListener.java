package com.mengcraft.script;

/**
 * Created on 16-10-17.
 */
public class HandledListener implements Comparable<HandledListener> {

    private final ScriptListener listener;
    private final EventListener up;
    private final int priority;
    private final ScriptPlugin plugin;

    public HandledListener(EventListener up, ScriptPlugin plugin, ScriptPlugin.Listener i) {
        this.up = up;
        this.plugin = plugin;
        listener = i.getListener();
        priority = i.getPriority();
    }

    public boolean remove() {
        return plugin.remove(this) && up.remove(this);
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

    @Override
    public int compareTo(HandledListener i) {
        return priority - i.priority;
    }

}