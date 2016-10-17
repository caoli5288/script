package com.mengcraft.script;

import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created on 16-10-17.
 */
public class ScriptPlugin {

    private final List<EventListener.HandledListener> handled = new ArrayList<>();
    private final Main main;
    private final Map<String, Object> description = new HashMap<>();

    public ScriptPlugin(Main main) {
        this.main = main;
    }

    public List<EventListener.HandledListener> getHandled() {
        return handled;
    }

    public String getDescription(String key) {
        if (description.containsKey(key)) {
            return description.get(key).toString();
        }
        return null;
    }

    public void shutdown() {
        for (EventListener.HandledListener i : handled) {
            i.remove();
        }
    }

    public Player getPlayer(String id) {
        return main.getServer().getPlayerExact(id);
    }

    public Player getPlayer(UUID id) {
        return main.getServer().getPlayer(id);
    }

    public EventListener.HandledListener addListener(String name, ScriptListener listener) {
        return EventMapping.INSTANCE.getListener(name).addListener(main, this, listener, -1);
    }

    public EventListener.HandledListener addListener(String name, ScriptListener listener, int priority) {
        return EventMapping.INSTANCE.getListener(name).addListener(main, this, listener, priority);
    }

    public EventMapping getMapping() {
        return EventMapping.INSTANCE;
    }

    public void setDescription(Map<String, Object> in) {
        in.forEach((i, value) -> {
            if (!description.containsKey(i)) description.put(i, value);
        });
    }

    public void setDescription(String key, String value) {
        if (!description.containsKey(key)) description.put(key, value);
    }

    @Override
    public String toString() {
        return description.toString();
    }

}
