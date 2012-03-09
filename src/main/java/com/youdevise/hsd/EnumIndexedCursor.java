package com.youdevise.hsd;

import java.util.EnumMap;

public interface EnumIndexedCursor<E extends Enum<E>> {
    <T> T get(E key);
    int getInt(E key);
    short getShort(E key);
    long getLong(E key);
    float getFloat(E key);
    double getDouble(E key);
    byte getByte(E key);
    EnumMap<E, Object> values();
    boolean next();
}