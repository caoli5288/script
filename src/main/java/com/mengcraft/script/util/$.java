package com.mengcraft.script.util;

import com.mengcraft.script.ScriptBootstrap;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;

public class $ {

    public static void sendBossBar(Player p, BossBar bar, int tick) {
        AtomicInteger letch = new AtomicInteger(tick);
        bar.setProgress(1);
        bar.addPlayer(p);
        bar.show();
        PluginHelper.run(ScriptBootstrap.get(), 10, 10, t -> {
            int i = letch.addAndGet(-10);
            if (i < 1) {
                bar.removeAll();
                bar.hide();
                t.cancel();
            } else {
                double progress = BigDecimal.valueOf(i).divide(BigDecimal.valueOf(tick), 2, 4).doubleValue();
                bar.setProgress(progress);
            }
        });
    }

}
