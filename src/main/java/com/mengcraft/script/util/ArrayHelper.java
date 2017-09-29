package com.mengcraft.script.util;

import com.google.common.collect.Lists;
import lombok.SneakyThrows;
import lombok.val;

import javax.script.Invocable;
import javax.script.ScriptEngineManager;
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

    final Helper helper;

    @SneakyThrows
    ArrayHelper() {
        val engine = new ScriptEngineManager().getEngineByName("js");
        engine.eval("" +
                "var toJSArray = function (input) {\n" +
                "    return Array.prototype.slice.call(input);\n" +
                "}");
        helper = Invocable.class.cast(engine).getInterface(Helper.class);
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
