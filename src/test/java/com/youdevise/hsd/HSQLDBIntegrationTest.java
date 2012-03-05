package com.youdevise.hsd;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Joiner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class HSQLDBIntegrationTest {

    private final Connection connection;
    
    public HSQLDBIntegrationTest() throws SQLException {
        DriverManager.registerDriver(new org.hsqldb.jdbcDriver());
        connection = DriverManager.getConnection("jdbc:hsqldb:mem:mymemdb", "SA", "");
    }
    
    @Before public void
    create_and_populate_table() throws SQLException {
        executeStatement(
                "CREATE TABLE test (",
                "       id INT IDENTITY,",
                "       name VARCHAR(255),",
                "       primary key(id));");

        for (int i=0; i<1000000; i++) {
            executeStatement("INSERT INTO test (name) values ('",
                             Integer.toString(i), "');");
        }
    }
    
    @After public void
    drop_table() throws SQLException {
        executeStatement("DROP TABLE test;");
    }
    
    public enum Fields {
        @Column("ID") id,
        @Column("NAME") name
    }
    
    public interface Record {
        int getId();
        String getName();
    }
    
    public class MethodBasedHandler {
        public boolean handle(int id, String name) {
            return true;
        }
    }
    
    public class ProxyBasedHandler implements ProxyHandler<Record> {
        @Override public boolean handle(Record cursor) {
            int id = cursor.getId();
            String name = cursor.getName();
            return id > -1 && name.length() > 0;
        }
    }
    
    public class EnumBasedHandler implements EnumIndexedCursorHandler<Fields> {
        @Override
        public boolean handle(EnumIndexedCursor<Fields> cursor) {
            int id = cursor.get(Fields.id);
            String name = cursor.get(Fields.name);
            return id > -1 && name.length() > 0;
        }
    }
    
    @Test public void
    passes_retrieved_records_to_handler_method() throws Exception {
        MethodBasedHandler handler = new MethodBasedHandler();
        CursorHandlingTraverser<Fields> traverser = getMethodDispatchingCursorHandler(handler);
        
        Date before = new Date();
        
        assertThat(executeTestQuery(traverser, Fields.class), equalTo(true));
        
        Date after = new Date();
        System.out.println(String.format("Traversed 1000000 records in %s milliseconds using reflection", after.getTime() - before.getTime()));
    }
    
    @Test public void
    passes_retrieved_records_to_proxy_handler() throws Exception {
        ProxyBasedHandler handler = new ProxyBasedHandler();
        Date before = new Date();
        ProxyHandlingTraverser<Record, Fields> traverser = new ProxyHandlingTraverser<Record, Fields>(Record.class, Fields.class, handler);
        
        assertThat(executeTestQuery(traverser, Fields.class), equalTo(true));
        
        Date after = new Date();
        System.out.println(String.format("Traversed 1000000 records in %s milliseconds using proxy", after.getTime() - before.getTime()));
    }
    
    @Test public void
    passes_retrieved_records_to_anonymous_inner_class_handler() throws Exception {
        EnumBasedHandler handler = new EnumBasedHandler();
        Date before = new Date();
        
        assertThat(executeTestQuery(new CursorHandlingTraverser<Fields>(handler), Fields.class), equalTo(true));
        
        Date after = new Date();
        System.out.println(String.format("Traversed 1000000 records in %s milliseconds using anonymous inner class", after.getTime() - before.getTime()));
    }
    
    private CursorHandlingTraverser<Fields> getMethodDispatchingCursorHandler(MethodBasedHandler handler) throws NoSuchMethodException {
        Method method = MethodBasedHandler.class.getMethod("handle", Integer.TYPE, String.class);
        MethodDispatcher<MethodBasedHandler, Fields> factory = MethodDispatcherFactory.dispatching(MethodBasedHandler.class, method, Fields.class, Fields.id, Fields.name);
        EnumIndexedCursorHandler<Fields> cursorHandler = factory.to(handler);
        CursorHandlingTraverser<Fields> traverser = new CursorHandlingTraverser<Fields>(cursorHandler);
        return traverser;
    }
    
    private <E extends Enum<E>> boolean executeTestQuery(EnumIndexedCursorTraverser<E> traverser, Class<E> enumClass) throws SQLException, NoSuchMethodException {
        Statement statement = connection.createStatement();
        try {
            ResultSet resultSet = statement.executeQuery("SELECT id, name FROM test");
            try {
                EnumIndexedCursor<E> cursor = ResultSetAdapter.adapting(resultSet, enumClass);
                return traverser.traverse(cursor);
            } finally {
                resultSet.close();
            }
        } finally {
            statement.close();
        }
    }
    
    private void executeStatement(String...sql) throws SQLException {
        Statement statement = connection.createStatement();
        try {
            statement.execute(Joiner.on("\n").join(sql));
        } finally {
            statement.close();
        }
    }
}
