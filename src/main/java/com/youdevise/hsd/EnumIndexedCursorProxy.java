package com.youdevise.hsd;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

import com.google.common.collect.Maps;

public class EnumIndexedCursorProxy<E extends Enum<E>> implements InvocationHandler {

    private final EnumIndexedCursor<E> cursor;
    private final Map<Method, E> methodMap;

    @SuppressWarnings("unchecked")
    public static <T, E extends Enum<E>> T proxying(EnumIndexedCursor<E> cursor, Class<E> enumClass, Class<T> iface) {
        Map<Method, E> methodMap = mapInterface(enumClass, iface);
        return (T) Proxy.newProxyInstance(iface.getClassLoader(),
                                      new Class<?>[] { iface },
                                      new EnumIndexedCursorProxy<E>(cursor, methodMap));
    }
    
    private static <T, E extends Enum<E>> Map<Method, E> mapInterface(Class<E> enumClass, Class<T> iface) {
        Map<Method, E> methodMap = Maps.newHashMap();
        for (E value : enumClass.getEnumConstants()) {
            try {
                Method method = iface.getMethod("get" + value.name().substring(0, 1).toUpperCase() + value.name().substring(1), new Class<?>[] { });
                methodMap.put(method, value);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }
        return methodMap;
    }
    

    public EnumIndexedCursorProxy(EnumIndexedCursor<E> cursor, Map<Method, E> methodMap) {
        this.cursor = cursor;
        this.methodMap = methodMap;
    }

    @Override
    public Object invoke(Object arg0, Method arg1, Object[] arg2) throws Throwable {
        E fieldEnum = methodMap.get(arg1);
        return cursor.get(fieldEnum);
    }
}
