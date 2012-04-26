package com.youdevise.hsd;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import com.youdevise.hsd.Query;

import com.google.common.base.Joiner;

public class MagicQuery implements InvocationHandler {
    
    @SuppressWarnings("unchecked")
    public static <T> T proxying(Class<T> iface) {
        return (T) Proxy.newProxyInstance(MagicQuery.class.getClassLoader(),
                                      new Class<?>[] { iface },
                                      new MagicQuery());
    }

    @Override
    public Object invoke(Object target, Method method, Object[] params) throws Throwable {
        if (method.getReturnType().isAssignableFrom(Query.class)) {
            return new Query(getQuerySql(method), params);
        }
        
        if (method.getReturnType().isAssignableFrom(BatchInsert.class)) {
            BatchInsert insert = new BatchInsert(getQuerySql(method));
            if (params != null) {
                insert.addValues(params);
            }
            return insert;
        }
        return null;
    }

    private String getQuerySql(Method method) {
        return Joiner.on("\n").join(method.getAnnotation(Sql.class).value());
    }
}