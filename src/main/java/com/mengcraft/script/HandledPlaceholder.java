package com.mengcraft.script;

import lombok.Data;
import lombok.EqualsAndHashCode;
import me.clip.placeholderapi.PlaceholderHook;
import org.bukkit.entity.Player;

/**
 * Created by on 2017/7/11.
 */
@Data
@EqualsAndHashCode(of = "id")
public class HandledPlaceholder {

    public interface Func {

        String apply(Player p, String[] input);
    }

    private final ScriptPlugin plugin;
    private final String id;
    private final Func func;
    private PlaceholderHook hook;

    public void remove() {
        plugin.remove(this);
    }

    public PlaceholderHook getHook() {
        if (hook == null) {
            hook = new PlaceholderHook() {
                public String onPlaceholderRequest(Player player, String s) {
                    return func.apply(player, s.split("_"));
                }
            };
        }
        return hook;
    }

    public void setHook(PlaceholderHook hook) {
        throw new UnsupportedOperationException();
    }
}
