package com.mengcraft.script;

/**
 * Created on 16-10-17.
 */
public class HandledListener implements Comparable<HandledListener> {

    private final ScriptListener listener;
    private final EventListener up;
    private int priority;

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

    public void setPriority(int priority) {
        this.priority = priority;
    }

    @Override
    public int compareTo(HandledListener i) {
        return priority - i.priority;
    }

}