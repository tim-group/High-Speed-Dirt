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
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Joiner;

public class HSQLDBIntegrationTest {

    private final Connection connection;
    
    public HSQLDBIntegrationTest() throws SQLException {
        DriverManager.registerDriver(new org.hsqldb.jdbcDriver());
        connection = DriverManager.getConnection("jdbc:hsqldb:mem:mymemdb", "SA", "");
    }
    
    public static class Strings {
        public static String concat(String...strings) {
            return Joiner.on("\n").join(strings);
        }
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
    
    private void executeStatement(String...sql) throws SQLException {
        Statement statement = connection.createStatement();
        try {
            statement.execute(Joiner.on("\n").join(sql));
        } finally {
            statement.close();
        }
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
    
    @Test public void
    passes_retrieved_record_fields_to_handler() throws Exception {
        Handler handler = new Handler();
        Date before = new Date();
        
        executeQuery(handler);
        
        Date after = new Date();
        System.out.println(String.format("Traversed 100000 records in %s milliseconds", after.getTime() - before.getTime()));
        
        MatcherAssert.assertThat(handler.count, Matchers.equalTo(100000));
    }

    private void executeQuery(Handler handler) throws SQLException, NoSuchMethodException {
        Statement statement = connection.createStatement();
        try {
            ResultSet resultSet = statement.executeQuery("SELECT id, name FROM test");
            try {
                traverseResults(resultSet, handler);
            } finally {
                resultSet.close();
            }
        } finally {
            statement.close();
        }
    }

    private void traverseResults(ResultSet resultSet, Handler handler) throws NoSuchMethodException {
        EnumIndexedCursor<Fields> cursor = ResultSetAdapter.adapting(resultSet, Fields.class);
        Method method = Handler.class.getMethod("handle", Integer.TYPE, String.class);
        MethodDispatcher<Handler, Fields> factory = MethodDispatcherFactory.dispatching(Handler.class, method, Fields.class, Fields.id, Fields.name);
        EnumIndexedCursorHandler<Fields> cursorHandler = factory.to(handler);
        while (cursor.next()) {
            cursorHandler.handle(cursor);
        }
    }
}
