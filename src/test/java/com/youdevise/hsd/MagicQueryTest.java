package com.youdevise.hsd;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.junit.Test;

import com.google.common.base.Joiner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;

public class MagicQueryTest {

    public static interface TestInterface {
    
        @Sql({ "SELECT foo",
               "FROM bar",
               "WHERE baz=?" })
        Query selectFoo(int baz);
    }
    
    public static class MagicQuery implements InvocationHandler {
        
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
            return null;
        }

        private String getQuerySql(Method method) {
            return Joiner.on("\n").join(method.getAnnotation(Sql.class).value());
        }
    }
    
    @Test public void
    proxies_method_call_to_create_query_with_parameters() {
        TestInterface testInterface = MagicQuery.proxying(TestInterface.class);
        Query query = testInterface.selectFoo(100);
       
        assertThat(query.sql(), is("SELECT foo\nFROM bar\nWHERE baz=?"));
        assertThat(query.parameters(), hasItem(100));
    }
    
}
