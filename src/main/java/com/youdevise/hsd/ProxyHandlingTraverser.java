package com.youdevise.hsd;

import java.sql.SQLException;

import com.youdevise.hsd.asm.ProxyGenerator;

public class ProxyHandlingTraverser<T, E extends Enum<E>> implements Traverser<EnumIndexedCursor<E>> {
    
    public static <T, E extends Enum<E>> ProxyHandlingTraverser<T, E> proxying(Class<T> proxyClass, Class<E> enumClass, Handler<T, Boolean> handler) {
        ProxyGenerator<E, T> proxyGenerator = ProxyGenerator.mapping(enumClass).to(proxyClass);
        return new ProxyHandlingTraverser<T, E>(proxyGenerator, handler);
    }
    
    private final ProxyGenerator<E, T> proxyGenerator;
    private final Handler<T, Boolean> handler;
    
    private ProxyHandlingTraverser(ProxyGenerator<E, T> proxyGenerator, Handler<T, Boolean> handler) {
        this.proxyGenerator = proxyGenerator;
        this.handler = handler;
    }
    
    @Override public boolean traverse(EnumIndexedCursor<E> cursor) throws SQLException {
        T proxy = proxyGenerator.generateView(cursor);
        while (cursor.next()) {
            if (!handler.handle(proxy)) { return false; }
        }
        return true;
    }
}