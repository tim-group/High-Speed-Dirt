package com.youdevise.hsd;

import java.util.EnumMap;

public interface EnumIndexedCursor<E extends Enum<E>> {
    <T> T get(E key);
    EnumMap<E, Object> values();
    boolean next();
}