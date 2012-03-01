package com.youdevise.hsd;

public interface EnumIndexedCursorHandler<E extends Enum<E>> {
    boolean handle(EnumIndexedCursor<E> handler);
}
