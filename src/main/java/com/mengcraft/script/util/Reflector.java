package com.mengcraft.script.util;

import lombok.SneakyThrows;
import lombok.val;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by on 2017/7/3.
 */
public enum Reflector {

    MAPPING;

    private final Map<Type, Map> f = new HashMap<>();
    private final Map<Type, Map> m = new HashMap<>();


    @SneakyThrows
    static Field b(Class<?> type, String name) {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    @SneakyThrows
    static Method b(Class<?> type, String name, Class<?>[] p) {
        try {
            val method = type.getDeclaredMethod(name, p);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException e) {
            if (!(type == Object.class)) {
                return getMethodRef(type, name, p);
            } else {
                throw e;
            }
        }
    }

    @SneakyThrows
    static Field getFieldRef(Class<?> type, String name) {
        Map<String, Field> map = MAPPING.f.computeIfAbsent(type, t -> new HashMap<>());
        return map.computeIfAbsent(name, n -> b(type, name));
    }

    @SneakyThrows
    static Method getMethodRef(Class<?> type, String name, Class<?>[] p) {
        Map<String, Method> map = MAPPING.m.computeIfAbsent(type, t -> new HashMap<>());
        return map.computeIfAbsent(name + "|" + Arrays.toString(p), n -> b(type, name, p));
    }

    static Class<?> clz(Object any) {
        return any instanceof Class ? ((Class) any) : any.getClass();
    }

    @SneakyThrows
    public static <T> T invoke(Object any, String method, Object... input) {
        Class<?>[] p = new Class[input.length];
        for (int i = 0; i < input.length; i++) {
            p[i] = input[i].getClass();
        }
        val invokable = getMethodRef(clz(any), method, p);
        return (T) invokable.invoke(any, input);
    }

    @SneakyThrows
    public static <T> T invoke(Class what, Object obj, String invoke, Object... arguments) {
        Class<?>[] p = new Class[arguments.length];
        for (int i = 0; i < arguments.length; i++) {
            p[i] = arguments[i].getClass();
        }
        val invokable = getMethodRef(what, invoke, p);
        return (T) invokable.invoke(obj, arguments);
    }

    @SneakyThrows
    public static <T> T getField(Object any, String field) {
        Field ref = getFieldRef(clz(any), field);
        return (T) ref.get(any);
    }
}
