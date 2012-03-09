package com.youdevise.hsd;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.EnumMap;
import java.util.Map;


public class ResultSetAdapter<E extends Enum<E>> implements EnumIndexedCursor<E> {
    
    public static <E extends Enum<E>> ResultSetAdapter<E> adapting(ResultSet resultSet, Class<E> enumClass) {
        MetadataToEnumMapTransformer<E> transformer = MetadataToEnumMapTransformer.forEnumClass(enumClass);
        EnumMap<E, Integer> indices;
        try {
            indices = transformer.apply(resultSet.getMetaData());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return new ResultSetAdapter<E>(resultSet, enumClass, indices);
    }
    
    private final ResultSet resultSet;
    private final Class<E> enumClass;
    private final EnumMap<E, Integer> indices;
    
    private ResultSetAdapter(ResultSet resultSet, Class<E> enumClass, EnumMap<E, Integer> indices) {
        this.resultSet = resultSet;
        this.enumClass = enumClass;
        this.indices = indices;
    }
    
    @Override public <T> T get(E key) {
        return get(indices.get(key));
    }
    
    @SuppressWarnings("unchecked")
    private <T> T get(int index) {
        try {
            return (T) resultSet.getObject(index);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override public boolean next() {
        try {
            return resultSet.next();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    
    @Override public EnumMap<E, Object> values() {
        EnumMap<E, Object> values = new EnumMap<E, Object>(enumClass);
        for (Map.Entry<E, Integer> entry : indices.entrySet()) {
            values.put(entry.getKey(), get(entry.getValue()));
        }
        return values;
    }

    @Override
    public int getInt(E key) {
        try {
            return resultSet.getInt(indices.get(key));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getString(E key) {
        try {
            return resultSet.getString(indices.get(key));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    
}