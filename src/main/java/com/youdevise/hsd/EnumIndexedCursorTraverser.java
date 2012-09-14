package com.youdevise.hsd;

import java.sql.SQLException;

public class EnumIndexedCursorTraverser<E extends Enum<E>> implements Traverser<EnumIndexedCursor<E>> {
    
    public static <E extends Enum<E>> EnumIndexedCursorTraverser<E> forHandler(SQLHandler<EnumIndexedCursor<E>> handler) {
        return new EnumIndexedCursorTraverser<E>(handler);
    }
    
    private final SQLHandler<EnumIndexedCursor<E>> handler;
    
    private EnumIndexedCursorTraverser(SQLHandler<EnumIndexedCursor<E>> handler) {
        this.handler = handler;
    }
    
    @Override public boolean traverse(EnumIndexedCursor<E> arg) throws SQLException {
        while (arg.next()) {
            if (!handler.handle(arg)) { return false; }
        }
        return true;
    }
    
}