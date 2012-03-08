package com.youdevise.hsd;

import java.sql.ResultSet;

import com.google.common.base.Function;

public class ResultSetTransformer<E extends Enum<E>> implements Function<ResultSet, EnumIndexedCursor<E>> {
    
    public static <E extends Enum<E>> ResultSetTransformer<E> transforming(Class<E> enumClass) {
        return new ResultSetTransformer<E>(enumClass);
    }
    
    private final Class<E> enumClass;
    private ResultSetTransformer(Class<E> enumClass) {
        this.enumClass = enumClass;
    }
    
    @Override public EnumIndexedCursor<E> apply(ResultSet resultSet) {
        return ResultSetAdapter.adapting(resultSet, enumClass);
    }
}