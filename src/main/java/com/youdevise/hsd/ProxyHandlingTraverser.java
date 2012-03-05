package com.youdevise.hsd;


public class ProxyHandlingTraverser<T, E extends Enum<E>> implements EnumIndexedCursorTraverser<E> {
    private final Class<E> enumClass;
    private final Class<T> proxyClass;
    private final ProxyHandler<T> handler;
    
    public ProxyHandlingTraverser(Class<T> proxyClass, Class<E> enumClass, ProxyHandler<T> handler) {
        this.proxyClass = proxyClass;
        this.enumClass = enumClass;
        this.handler = handler;
    }
    
    @Override public boolean traverse(EnumIndexedCursor<E> cursor) {
        T proxy = EnumIndexedCursorProxy.proxying(cursor, enumClass, proxyClass);
        while (cursor.next()) {
            if (!handler.handle(proxy)) { return false; }
        }
        return true;
    }
}