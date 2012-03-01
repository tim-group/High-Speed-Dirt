package com.youdevise.hsd;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class MethodDispatcherFactory {

    public static <T, E extends Enum<E>> MethodDispatcher<T, E> dispatching(
            Class<T> targetClass,
            final Method method,
            final Class<E> enumClass,
            final E...fields) {
        return new ProxyMethodDispatcher<T, E>(method, fields);
    }
    
    public static class ProxyMethodDispatcher<T, E extends Enum<E>> implements MethodDispatcher<T, E> {
        private final Method method;
        private final E[] fields;

        public ProxyMethodDispatcher(Method method, E[] fields) {
            this.method = method;
            this.fields = fields;
        }
        
        public EnumIndexedCursorHandler<E> to(final T target) {
            return new EnumIndexedCursorHandler<E>() {
                @Override
                public boolean handle(EnumIndexedCursor<E> handler) {
                    Object[] params = new Object[fields.length];
                    for (int i=0; i<fields.length; i++) {
                        params[i] = handler.get(fields[i]);
                    }
                    try {
                        return (Boolean) method.invoke(target, params);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    } catch (IllegalArgumentException e) {
                        throw new RuntimeException(e);
                    } catch (InvocationTargetException e) {
                        throw new RuntimeException(e);
                    }
                }
            };
        }
    }
    
}
