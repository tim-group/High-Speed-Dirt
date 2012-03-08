package com.youdevise.hsd;

import java.sql.SQLException;

public class EnumIndexedCursorTraverser<E extends Enum<E>> implements Traverser<EnumIndexedCursor<E>> {
    
    public static <E extends Enum<E>> EnumIndexedCursorTraverser<E> forHandler(Handler<EnumIndexedCursor<E>, Boolean> handler) {
        return new EnumIndexedCursorTraverser<E>(handler);
    }
    
    private final Handler<EnumIndexedCursor<E>, Boolean> handler;
    
    private EnumIndexedCursorTraverser(Handler<EnumIndexedCursor<E>, Boolean> handler) {
        this.handler = handler;
    }
    
    @Override public boolean traverse(EnumIndexedCursor<E> arg) throws SQLException {
        while (arg.next()) {
            if (!handler.handle(arg)) { return false; }
        }
        return true;
    }
    
}