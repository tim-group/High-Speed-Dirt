package com.youdevise.hsd;

public interface CursorHandler<E extends Enum<E>> {
    boolean handle(EnumIndexedCursor<E> cursor);
}