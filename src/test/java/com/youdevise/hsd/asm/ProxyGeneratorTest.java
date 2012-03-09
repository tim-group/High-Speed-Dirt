package com.youdevise.hsd.asm;

import java.util.Date;
import java.util.EnumMap;

import org.junit.Test;

import com.youdevise.hsd.EnumIndexedCursor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class ProxyGeneratorTest {

    public enum Fields {
        foo,
        bar,
        baz
    }
    
    public static interface Record {
        String getFoo();
        int getBar();
        Date getBaz();
    }
    
    public static class TestCursorImpl<E extends Enum<E>> implements EnumIndexedCursor<E> {
        private final EnumMap<E, Object> values;
        
        public TestCursorImpl(EnumMap<E, Object> values) {
            this.values = values;
        }
        
        @SuppressWarnings("unchecked")
        @Override
        public <T> T get(E key) {
            return (T) values.get(key);
        }

        @Override
        public EnumMap<E, Object> values() {
            return values;
        }

        @Override
        public boolean next() {
            return false;
        }

        @Override
        public int getInt(E key) {
            return this.<Integer>get(key);
        }

        @Override
        public short getShort(E key) {
            return this.<Short>get(key);
        }

        @Override
        public long getLong(E key) {
            return this.<Long>get(key);
        }

        @Override
        public float getFloat(E key) {
            return this.<Float>get(key);
        }

        @Override
        public double getDouble(E key) {
            return this.<Double>get(key);
        }

        @Override
        public byte getByte(E key) {
            return this.<Byte>get(key);
        }
        
    }
    
    @Test public void
    generates_proxy_mapping_getters_to_enum_map() {
        Date theDate = new Date();
        EnumMap<Fields, Object> values = new EnumMap<Fields, Object>(Fields.class);
        values.put(Fields.foo, "Foo");
        values.put(Fields.bar, 23);
        values.put(Fields.baz, theDate);
        
        ProxyGenerator<Fields, Record> generator = ProxyGenerator.mapping(Fields.class).to(Record.class);
        Record record = generator.generateView(new TestCursorImpl<Fields>(values));
        
        assertThat(record.getFoo(), equalTo("Foo"));
        assertThat(record.getBar(), equalTo(23));
        assertThat(record.getBaz(), equalTo(theDate));
        
    }
    
}
