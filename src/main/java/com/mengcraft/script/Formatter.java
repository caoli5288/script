package com.mengcraft.script;

import lombok.AccessLevel;
import lombok.Setter;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;

import static com.mengcraft.script.Main.nil;

public class Formatter {

    @Setter(AccessLevel.PACKAGE)
    private static boolean replacePlaceholder;

    public static String format(Player p, String input) {
        return nil(input) ? null : splitLine(replacePlaceholder ? PlaceholderAPI.setPlaceholders(p, input) : input);
    }

    public static String splitLine(String input) {
        return nil(input) ? null : input.replace("\\n", "\n");
    }
}
