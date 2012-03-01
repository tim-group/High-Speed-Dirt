package com.youdevise.hsd;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.lang.reflect.Method;
import java.util.Date;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Before;
import org.junit.Test;

public class MethodDispatcherFactoryTest {

    private final Mockery context = new Mockery();
    public static enum Fields {
        foo,
        bar,
        baz
    }
    
    @SuppressWarnings("unchecked")
    private final EnumIndexedCursor<Fields> cursor = context.mock(EnumIndexedCursor.class);
    private final Date theDate = new Date();
    
    @Before public void
    stub_cursor() {
        context.checking(new Expectations() {{
            allowing(cursor).get(Fields.foo); will(returnValue("A string"));
            allowing(cursor).get(Fields.bar); will(returnValue(21));
            allowing(cursor).get(Fields.baz); will(returnValue(theDate));
        }});
    }
    
    public static interface Handler {
        boolean handle(Date baz, int bar, String foo);
    }
    
    @Test public void
    dispatches_enum_indexed_cursor_to_method() throws Exception {
        final Handler handler = context.mock(Handler.class);
        Method method = Handler.class.getMethod("handle", Date.class, Integer.TYPE, String.class);
        MethodDispatcher<Handler, Fields> dispatcher = MethodDispatcherFactory.dispatching(Handler.class, method, Fields.class, Fields.baz, Fields.bar, Fields.foo);
        
        context.checking(new Expectations() {{
            oneOf(handler).handle(theDate, 21, "A string"); will(returnValue(true));
        }});
        
        assertThat(dispatcher.to(handler).handle(cursor), equalTo(true));
        context.assertIsSatisfied();
    }
    
}
