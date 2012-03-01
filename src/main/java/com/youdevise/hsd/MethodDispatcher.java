package com.youdevise.hsd;

public interface MethodDispatcher<T, E extends Enum<E>> {
    public EnumIndexedCursorHandler<E> to(T instance);
}