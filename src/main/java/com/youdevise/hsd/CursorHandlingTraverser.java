package com.youdevise.hsd;


public class CursorHandlingTraverser<E extends Enum<E>> implements EnumIndexedCursorTraverser<E> {
    private final EnumIndexedCursorHandler<E> handler;
    
    public CursorHandlingTraverser(EnumIndexedCursorHandler<E> handler) {
        this.handler = handler;
    }

    @Override public boolean traverse(EnumIndexedCursor<E> cursor) {
        while (cursor.next()) {
            if (!handler.handle(cursor)) { return false; }
        }
        return true;
    }
}