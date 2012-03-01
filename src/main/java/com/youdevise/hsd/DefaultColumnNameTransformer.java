package com.youdevise.hsd;

import com.google.common.base.Function;
import com.google.common.base.Functions;

public class DefaultColumnNameTransformer<E extends Enum<E>> implements Function<E, String> {
    
    public static <E extends Enum<E>> DefaultColumnNameTransformer<E> forEnumClass(Class<E> enumClass) {
        return forEnumClass(enumClass, Functions.<String>identity());
    }
    
    public static <E extends Enum<E>> DefaultColumnNameTransformer<E> forEnumClass(Class<E> enumClass,
                                                                                   Function<String, String> nameTransformer) {
        return new DefaultColumnNameTransformer<E>(enumClass, nameTransformer);
    }
    
    private final Class<E> enumClass;
    private final Function<String, String> nameTransformer;
    
    private DefaultColumnNameTransformer(Class<E> enumClass, Function<String, String> nameTransformer) {
        this.enumClass = enumClass;
        this.nameTransformer = nameTransformer;
    }
    
    @Override
    public String apply(E value) {
        try {
            Column columnAnnotation = enumClass.getField(value.name()).getAnnotation(Column.class);
            return columnAnnotation == null
                    ? nameTransformer.apply(value.name())
                    : columnAnnotation.value();
        } catch (NoSuchFieldException e) {
          throw new RuntimeException(e);
        }
    }
}