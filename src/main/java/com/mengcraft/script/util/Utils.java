package com.mengcraft.script.util;

import com.mengcraft.script.ScriptBootstrap;
import lombok.SneakyThrows;
import lombok.experimental.var;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.yaml.snakeyaml.Yaml;

import javax.script.Bindings;
import javax.script.Invocable;
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public class Utils {

    private static final Yaml YAML = new Yaml();
    private static Function<Object, Bindings> java_from_invoker;

    public static Yaml getYaml() {
        return YAML;
    }

    public static void sendBossBar(Player p, BossBarWrapper bar, int tick) {
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

    @SneakyThrows
    public static <T> Bindings fromJava(T obj) {
        if (java_from_invoker == null) {
            var js = ScriptBootstrap.jsEngine();
            var function = js.eval("(function a(a){return Java.from(a);})");
            java_from_invoker = ((Invocable) js).getInterface(function, Function.class);
        }
        return java_from_invoker.apply(obj);
    }

    public static <T, Obj> T as(Obj obj, Class<T> cls) {
        return (T) obj;
    }

    public static EventPriority getEventPriority(int priority) {
        if (priority <= Byte.MIN_VALUE) {
            return EventPriority.LOWEST;
        }
        if (priority <= -64) {
            return EventPriority.LOW;
        }
        if (priority <= -1) {
            return EventPriority.NORMAL;
        }
        if (priority <= 63) {
            return EventPriority.HIGH;
        }
        if (priority <= Byte.MAX_VALUE) {
            return EventPriority.HIGHEST;
        }
        return EventPriority.MONITOR;
    }
}
