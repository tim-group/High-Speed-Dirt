package com.youdevise.hsd.asm;

import java.sql.SQLException;
import java.util.Date;
import java.util.EnumMap;

import org.junit.Test;

import com.youdevise.hsd.BenchmarkRunner;
import com.youdevise.hsd.DynamicProxyHandlingTraverser;
import com.youdevise.hsd.EnumIndexedCursor;
import com.youdevise.hsd.ProxyHandler;
import com.youdevise.hsd.ProxyHandlingTraverser;

public class BenchmarkASMAgainstProxyTest {

    private final BenchmarkRunner runner = new BenchmarkRunner(50);
    
    private static class TestProxyHandler implements ProxyHandler<Record> {
        @Override
        public Boolean handle(Record arg) throws SQLException {
            String output = String.format("%s, %s, %s, %s, %s, %s",
                    arg.getFoo(),
                    arg.getBar(),
                    arg.getBaz(),
                    arg.getFrobnitz(),
                    arg.getQuux(),
                    arg.getXyzzy());
            return (!output.equals("Zalgo"));
        }
    }

    public enum Fields {
        foo,
        bar,
        baz,
        xyzzy,
        quux,
        frobnitz
    }
    
    public static interface Record {
        int getFoo();
        boolean getBar();
        byte getBaz();
        long getXyzzy();
        String getQuux();
        Date getFrobnitz();
    }
    
    private static final class TestCursor implements EnumIndexedCursor<Fields> {
        private int index = 0;
        @SuppressWarnings("unchecked")
        @Override
        public <T> T get(Fields key) {
            switch (key) {
            case foo:
                return (T) Integer.valueOf(index);
            case bar:
                return (T) Boolean.valueOf(index > 100);
            case baz:
                return (T) Byte.valueOf((byte) (index % 256));
            case xyzzy:
                return (T) Long.valueOf(index);
            case quux:
                return (T) Integer.toString(index);
            case frobnitz:
                return (T) new Date();
            }
            return null;
        }

        @Override
        public int getInt(Fields key) {
            return index;
        }

        @Override
        public short getShort(Fields key) {
            return (short) index;
        }

        @Override
        public long getLong(Fields key) {
            return index;
        }

        @Override
        public float getFloat(Fields key) {
            return index;
        }

        @Override
        public double getDouble(Fields key) {
            return index;
        }

        @Override
        public byte getByte(Fields key) {
            return (byte) index;
        }

        @Override
        public EnumMap<Fields, Object> values() {
            return null;
        }

        @Override
        public boolean next() {
            index += 1;
            return index < 100000;
        }
        
        public void reset() {
            index = 0;
        }
    }
    
    @Test public void
    dynamic_proxy_performance_test() {
        final TestCursor cursor = new TestCursor();
        ProxyHandler<Record> handler = new TestProxyHandler();
        final DynamicProxyHandlingTraverser<Record, Fields> traverser = DynamicProxyHandlingTraverser.proxying(Record.class, Fields.class, handler);
        runner.runBenchmark("Dynamic proxy", new Runnable() {
            @Override public void run() {
                try {
                    cursor.reset();
                    traverser.traverse(cursor);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }
    
    @Test public void
    generated_proxy_performance_test() {
        final TestCursor cursor = new TestCursor();
        ProxyHandler<Record> handler = new TestProxyHandler();
        final ProxyHandlingTraverser<Record, Fields> traverser = ProxyHandlingTraverser.proxying(Record.class, Fields.class, handler);
        runner.runBenchmark("ASM-generated proxy", new Runnable() {
            @Override public void run() {
                try {
                    cursor.reset();
                    traverser.traverse(cursor);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }
}
