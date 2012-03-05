package com.youdevise.hsd;

interface EnumIndexedCursorTraverser<E extends Enum<E>> {
    boolean traverse(EnumIndexedCursor<E> cursor);
}