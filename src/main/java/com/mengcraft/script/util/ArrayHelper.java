package com.mengcraft.script.util;

import com.google.common.collect.Lists;
import com.mengcraft.script.ScriptBootstrap;
import lombok.SneakyThrows;
import lombok.experimental.var;

import javax.script.Invocable;
import javax.script.ScriptContext;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Created on 16-10-28.
 */
public enum ArrayHelper {

    INST;

    public interface Helper {
        Object toJSArray(Object input);
    }

    private final Helper helper;

    @SneakyThrows
    ArrayHelper() {
        var engine = ScriptBootstrap.jsEngine();
        var oldBindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
        try {
            engine.eval("" +
                    "function toJSArray(input) {\n" +
                    "    return Array.prototype.slice.call(input);\n" +
                    "}");
            helper = ((Invocable) engine).getInterface(Helper.class);
        } finally {
            engine.setBindings(oldBindings, ScriptContext.ENGINE_SCOPE);
        }
    }

    public static <T> List<T> link(T... in) {
        List<T> list = new LinkedList<>();
        for (T i : in) {
            list.add(i);
        }
        return list;
    }

    public static <T> T[] toArray(T... input) {
        return input;
    }

    public static <T> Object toJSArray(T[] input) {
        return INST.helper.toJSArray(input);
    }

    public static <T> Object toJSArray(Iterator<T> i) {
        return INST.helper.toJSArray(Lists.newArrayList(i).toArray());
    }

}
