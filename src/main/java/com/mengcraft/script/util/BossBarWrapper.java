package com.mengcraft.script.util;

import lombok.Data;
import lombok.experimental.Delegate;
import org.bukkit.boss.BossBar;

@Data
public class BossBarWrapper {

    @Delegate
    private final BossBar bossBar;
}
