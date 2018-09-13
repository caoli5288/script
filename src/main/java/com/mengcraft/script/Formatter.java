package com.mengcraft.script;

import lombok.AccessLevel;
import lombok.Setter;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.mengcraft.script.Main.nil;

public class Formatter {

    private static final Pattern PATTERN = Pattern.compile("\\$\\{(?<lab>[^${}]+)\\}");

    @Setter(AccessLevel.PACKAGE)
    private static boolean replacePlaceholder;

    public static String format(Player p, String input) {
        return input == null ? null : splitLine(replacePlaceholder ? multi(p, input) : ChatColor.translateAlternateColorCodes('&', input));
    }

    protected static String multi(Player p, String input) {
        Matcher matcher = PATTERN.matcher(input);
        if (matcher.find()) {
            String label = PlaceholderAPI.setPlaceholders(p, "%" + matcher.group("lab") + "%");
            input = input.replace(matcher.group(), label);
            return multi(p, input);
        }
        return PlaceholderAPI.setPlaceholders(p, input);
    }

    public static String splitLine(String input) {
        return input == null ? null : input.replace("\\n", "\n");
    }

}
