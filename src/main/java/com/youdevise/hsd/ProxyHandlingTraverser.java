package com.youdevise.hsd;

import java.sql.SQLException;


public class ProxyHandlingTraverser<T, E extends Enum<E>> implements Traverser<EnumIndexedCursor<E>> {
    
    public static <T, E extends Enum<E>> ProxyHandlingTraverser<T, E> proxying(Class<T> proxyClass, Class<E> enumClass, Handler<T, Boolean> handler) {
        return new ProxyHandlingTraverser<T, E>(proxyClass, enumClass, handler);
    }
    
    private final Class<E> enumClass;
    private final Class<T> proxyClass;
    private final Handler<T, Boolean> handler;
    
    private ProxyHandlingTraverser(Class<T> proxyClass, Class<E> enumClass, Handler<T, Boolean> handler) {
        this.proxyClass = proxyClass;
        this.enumClass = enumClass;
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