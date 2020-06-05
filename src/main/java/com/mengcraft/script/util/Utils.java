package com.mengcraft.script.util;

import com.mengcraft.script.ScriptBootstrap;
import lombok.SneakyThrows;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.yaml.snakeyaml.Yaml;

import javax.script.Bindings;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public class Utils {

    private static final Yaml YAML = new Yaml();
    private static Function<Object, Bindings> java_from_invoker;
    private static Consumer<Bindings> function_invoker;
    private static BiConsumer<Bindings, Object> function_alt_invoker;

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
    public static void setup(ScriptEngine js) {
        Bindings bindings = js.createBindings();
        js.eval("function apply(a){return Java.from(a);}", bindings);
        java_from_invoker = ((Invocable) js).getInterface(bindings, Function.class);
        bindings = js.createBindings();
        js.eval("function accept(a){a();}", bindings);
        function_invoker = ((Invocable) js).getInterface(bindings, Consumer.class);
        bindings = js.createBindings();
        js.eval("function accept(a, b){a(b);}", bindings);
        function_alt_invoker = ((Invocable) js).getInterface(bindings, BiConsumer.class);
    }

    public static void invoke(Bindings bindings) {
        function_invoker.accept(bindings);
    }

    public static <T> void invoke(Bindings invokable, T obj) {
        function_alt_invoker.accept(invokable, obj);
    }

    @SneakyThrows
    public static <T> Bindings fromJava(T obj) {
        return java_from_invoker.apply(obj);
    }

    public static <T, Obj> T as(Obj obj, Class<T> cls) {
        return (T) obj;
    }

    public static Field getAccessibleField(Class<?> cls, String name) {
        try {
            Field f = cls.getDeclaredField(name);
            f.setAccessible(true);
            return f;
        } catch (Exception e) {
        }
        return null;
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
