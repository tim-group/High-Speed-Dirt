package com.youdevise.hsd;

import java.sql.SQLException;

public class DynamicProxyHandlingTraverser<T, E extends Enum<E>> implements Traverser<EnumIndexedCursor<E>> {
    
    public static <T, E extends Enum<E>> DynamicProxyHandlingTraverser<T, E> proxying(Class<T> proxyClass, Class<E> enumClass, SQLHandler<T> handler) {
        return new DynamicProxyHandlingTraverser<T, E>(enumClass, proxyClass, handler);
    }

    private final Class<E> enumClass;
    private final Class<T> proxyClass;
    private final SQLHandler<T> handler;
    
    private DynamicProxyHandlingTraverser(Class<E> enumClass, Class<T> proxyClass, SQLHandler<T> handler) {
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