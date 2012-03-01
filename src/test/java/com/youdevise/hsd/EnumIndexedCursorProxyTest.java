package com.youdevise.hsd;

import java.util.Date;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class EnumIndexedCursorProxyTest {

    private final Mockery context = new Mockery();
    private final Date theDate = new Date();
    
    public static interface Record {
        public enum Fields {
            foo,
            bar,
            @Column("baz_id") baz
        }
        
        public String getFoo();
        public int getBar();
        public Date getBaz();
    }
    
    @SuppressWarnings("unchecked")
    @Test public void
    proxies_method_calls_to_enum_indexed_cursor() {
        final EnumIndexedCursor<Record.Fields> cursor = context.mock(EnumIndexedCursor.class);
        
        Record record = EnumIndexedCursorProxy.proxying(cursor, Record.Fields.class, Record.class);
        
        context.checking(new Expectations() {{
            allowing(cursor).get(Record.Fields.foo); will(returnValue("A string value"));
            allowing(cursor).get(Record.Fields.bar); will(returnValue(21));
            allowing(cursor).get(Record.Fields.baz); will(returnValue(theDate));
        }});
        
        assertThat(record.getFoo(), equalTo("A string value"));
        assertThat(record.getBar(), equalTo(21));
        assertThat(record.getBaz(), equalTo(theDate));
    }
}
