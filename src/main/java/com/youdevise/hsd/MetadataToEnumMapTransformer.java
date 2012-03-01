package com.youdevise.hsd;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.EnumMap;
import java.util.Map;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

public class MetadataToEnumMapTransformer<E extends Enum<E>> implements Function<ResultSetMetaData, EnumMap<E, Integer>> {

    public static <E extends Enum<E>> MetadataToEnumMapTransformer<E> forEnumClass(Class<E> enumClass) {
        return forEnumClass(enumClass, DefaultColumnNameTransformer.forEnumClass(enumClass));
    }
    
    public static <E extends Enum<E>> MetadataToEnumMapTransformer<E> forEnumClass(Class<E> enumClass,
                                                                                   Function<E, String> columnNameTransformer) {
        return new MetadataToEnumMapTransformer<E>(enumClass,columnNameTransformer);
    }
    
    private final Class<E> enumClass;
    private final Function<E, String> columnNameTransformer;

    private MetadataToEnumMapTransformer(Class<E> enumClass, Function<E, String> columnNameTransformer) {
        this.enumClass = enumClass;
        this.columnNameTransformer = columnNameTransformer;
    }
    
    @Override public EnumMap<E, Integer> apply(ResultSetMetaData metadata) {
        Map<String, Integer> columnIndices = getColumnIndices(metadata);
        EnumMap<E, Integer> indices = new EnumMap<E, Integer>(enumClass);
        for (E value : enumClass.getEnumConstants()) {
            String columnName = columnNameTransformer.apply(value);
            Integer index = columnIndices.get(columnName);
            Preconditions.checkNotNull(index, "No column found named ", columnName);
            indices.put(value, index);
        }
        return indices;
    }
    
    private Map<String, Integer> getColumnIndices(ResultSetMetaData metadata) {
        ImmutableMap.Builder<String, Integer> indices = ImmutableMap.builder();
        try {
            for (int i=1; i<=metadata.getColumnCount(); i++) {
                indices.put(metadata.getColumnName(i), i);
            }
            return indices.build();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}