package com.youdevise.hsd;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;

import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.jdbc.Work;
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
            if (name.equals("Zalgo")) {
                throw new RuntimeException("He comes!");
            }
            return true;
        }
    }
    
    public class ProxyBasedHandler implements ProxyHandler<Record> {
        @Override public boolean handle(Record cursor) {
            int id = cursor.getId();
            String name = cursor.getName();
            if (name.equals("Zalgo")) {
                throw new RuntimeException("He comes!");
            }
            return id > -1 && name.length() > 0;
        }
    }
    
    public class EnumBasedHandler implements EnumIndexedCursorHandler<Fields> {
        @Override
        public boolean handle(EnumIndexedCursor<Fields> cursor) {
            int id = cursor.get(Fields.id);
            String name = cursor.get(Fields.name);
            if (name.equals("Zalgo")) {
                throw new RuntimeException("He comes!");
            }
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
        System.out.println(String.format("Traversed 1000000 records in %s milliseconds using enum-based handler", after.getTime() - before.getTime()));
    }

    
    @Test public void
    hibernate_is_slow() {
        Session session = createHibernateSession();
        
        ScrollableResults results = session.createQuery("SELECT p FROM MyPersistable p")
                                           .setReadOnly(true)
                                           .setCacheable(false)
                                           .scroll(ScrollMode.FORWARD_ONLY);
        
        Date before = new Date();
        while (results.next()) {
            MyPersistable persistable = (MyPersistable) results.get()[0];
            if (persistable.getName().equals("Zalgo")) {
                throw new RuntimeException("He comes!");
            }
        }
        Date after = new Date();
        System.out.println(String.format("Traversed 1000000 records in %s milliseconds using Hibernate", after.getTime() - before.getTime()));
        
        results.close();
        session.close();
    }

    private Session createHibernateSession() {
        AnnotationConfiguration cfg = new AnnotationConfiguration();
        cfg.addPackage("com.youdevise.hsd")
           .addAnnotatedClass(MyPersistable.class)
           .setProperty("hibernate.dialect", "org.hibernate.dialect.HSQLDialect")
           .setProperty("hibernate.jdbc.use_scrollable_resultset", "true");
        SessionFactory factory = cfg.buildSessionFactory();
        Session session = factory.openSession(connection);
        return session;
    }
    
    @Test public void
    traverser_can_be_used_with_hibernate() {
        Session session = createHibernateSession();
        final EnumBasedHandler handler = new EnumBasedHandler();
        final CursorHandlingTraverser<Fields> traverser = new CursorHandlingTraverser<Fields>(handler);
        
        Date before = new Date();
        session.doWork(new Work() {
            @Override
            public void execute(Connection hibernateConnection) throws SQLException {
                executeTestQuery(traverser, Fields.class, hibernateConnection);
            }
        });
        Date after = new Date();
        
        System.out.println(String.format("Traversed 1000000 records in %s milliseconds using enum-based handler in a Work object inside Hibernate", after.getTime() - before.getTime()));
        
        session.close();
    }
    
    private CursorHandlingTraverser<Fields> getMethodDispatchingCursorHandler(MethodBasedHandler handler) throws NoSuchMethodException {
        Method method = MethodBasedHandler.class.getMethod("handle", Integer.TYPE, String.class);
        MethodDispatcher<MethodBasedHandler, Fields> factory = MethodDispatcherFactory.dispatching(MethodBasedHandler.class, method, Fields.class, Fields.id, Fields.name);
        EnumIndexedCursorHandler<Fields> cursorHandler = factory.to(handler);
        CursorHandlingTraverser<Fields> traverser = new CursorHandlingTraverser<Fields>(cursorHandler);
        return traverser;
    }
    
    private <E extends Enum<E>> boolean executeTestQuery(EnumIndexedCursorTraverser<E> traverser, Class<E> enumClass) throws SQLException {
        return executeTestQuery(traverser, enumClass, connection);
    }
    
    private <E extends Enum<E>> boolean executeTestQuery(EnumIndexedCursorTraverser<E> traverser, Class<E> enumClass, Connection conn) throws SQLException {
        Statement statement = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
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
