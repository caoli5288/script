package com.mengcraft.script;

/**
 * Created on 16-10-17.
 */
public class HandledListener implements Comparable<HandledListener> {

    private final ScriptListener listener;
    private final EventListener up;
    private final int priority;

    public HandledListener(EventListener up, ScriptListener listener, int priority) {
        this.up = up;
        this.listener = listener;
        this.priority = priority;
    }

    public boolean remove() {
        return up.remove(this);
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