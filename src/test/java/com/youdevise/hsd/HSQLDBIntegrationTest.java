package com.youdevise.hsd;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Joiner;

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

        for (int i=0; i<100000; i++) {
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
    
    public class Handler {
        public int count = 0;
        public boolean handle(int id, String name) {
            count += 1;
            return true;
        }
    }
    
    public interface Record {
        int getId();
        String getName();
    }
    
    public class RecordHandler implements ProxyHandler<Record> {
        public int count = 0;
        @Override public boolean handle(Record cursor) {
            int id = cursor.getId();
            String name = cursor.getName();
            count += 1;
            return id > -1 && name.length() > 0;
        }
    }
    
    @Test public void
    passes_retrieved_record_fields_to_handler_method() throws Exception {
        Handler handler = new Handler();
        CursorHandlingTraverser<Fields> traverser = getMethodDispatchingCursorHandler(handler);
        
        Date before = new Date();
        
        executeTestQuery(traverser, Fields.class);
        
        Date after = new Date();
        System.out.println(String.format("Traversed 100000 records in %s milliseconds using reflection", after.getTime() - before.getTime()));
        
        MatcherAssert.assertThat(handler.count, Matchers.equalTo(100000));
    }
    
    @Test public void
    passes_retrieved_record_fields_to_proxy_handler() throws Exception {
        RecordHandler handler = new RecordHandler();
        Date before = new Date();
        ProxyHandlingTraverser<Record, Fields> traverser = new ProxyHandlingTraverser<Record, Fields>(Record.class, Fields.class, handler);
        
        executeTestQuery(traverser, Fields.class);
        
        Date after = new Date();
        System.out.println(String.format("Traversed 100000 records in %s milliseconds using proxy", after.getTime() - before.getTime()));
        MatcherAssert.assertThat(handler.count, Matchers.equalTo(100000));
    }
    
    private CursorHandlingTraverser<Fields> getMethodDispatchingCursorHandler(Handler handler) throws NoSuchMethodException {
        Method method = Handler.class.getMethod("handle", Integer.TYPE, String.class);
        MethodDispatcher<Handler, Fields> factory = MethodDispatcherFactory.dispatching(Handler.class, method, Fields.class, Fields.id, Fields.name);
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
