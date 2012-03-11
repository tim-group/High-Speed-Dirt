package com.youdevise.hsd;

import java.sql.SQLException;

public class DynamicProxyHandlingTraverser<T, E extends Enum<E>> implements Traverser<EnumIndexedCursor<E>> {
    
    public static <T, E extends Enum<E>> DynamicProxyHandlingTraverser<T, E> proxying(Class<T> proxyClass, Class<E> enumClass, Handler<T, Boolean> handler) {
        return new DynamicProxyHandlingTraverser<T, E>(enumClass, proxyClass, handler);
    }

    private final Class<E> enumClass;
    private final Class<T> proxyClass;
    private final Handler<T, Boolean> handler;
    
    private DynamicProxyHandlingTraverser(Class<E> enumClass, Class<T> proxyClass, Handler<T, Boolean> handler) {
        this.enumClass = enumClass;
        this.proxyClass = proxyClass;
        this.handler = handler;
    }
    
    @Override public boolean traverse(EnumIndexedCursor<E> cursor) throws SQLException {
        T proxy = EnumIndexedCursorProxy.proxying(cursor, enumClass, proxyClass);
        while (cursor.next()) {
            if (!handler.handle(proxy)) { return false; }
        }
        return true;
    }
}