package com.youdevise.hsd;

import java.sql.ResultSet;

public final class Traversers {

    private Traversers() { }
    
    public static Traverser<ResultSet> adapt(Handler<ResultSet, Boolean> resultSetHandler) {
        return ResultSetTraverser.forHandler(resultSetHandler);
    }

    public static <E extends Enum<E>> Traverser<ResultSet> adapt(Class<E> enumClass, Traverser<EnumIndexedCursor<E>> traverser) {
        return AdaptingTraverser.adapting(ResultSetTransformer.transforming(enumClass), traverser);
    }

    public static <E extends Enum<E>> Traverser<ResultSet> adapt(Class<E> enumClass, Handler<EnumIndexedCursor<E>, Boolean> cursorHandler) {
        return adapt(enumClass, EnumIndexedCursorTraverser.forHandler(cursorHandler));
    }

    public static <T, E extends Enum<E>> Traverser<ResultSet> adapt(Class<T> proxyClass, Class<E> enumClass, Handler<T, Boolean> proxyHandler) {
        return adapt(enumClass, ProxyHandlingTraverser.proxying(proxyClass, enumClass, proxyHandler));
    }
}
