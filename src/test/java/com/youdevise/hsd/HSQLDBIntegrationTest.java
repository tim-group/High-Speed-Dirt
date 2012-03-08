package com.youdevise.hsd;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.Map;

import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AnnotationConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Joiner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class HSQLDBIntegrationTest {

    private final Connection connection;
    private static final Query TEST_QUERY = new Query("SELECT id, name FROM test", new Object[] {});
    
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
            executeStatement("INSERT INTO test (name) values ('" + Integer.toString(i) + "');");
        }
    }
    
    @After public void
    drop_table() throws SQLException {
        executeStatement("DROP TABLE test;");
    }
    
    private final class ResultSetBasedHandler implements ResultSetHandler {
        @Override
        public Boolean handle(ResultSet arg) throws SQLException {
            int id = (Integer) arg.getObject(1);
            String name = (String) arg.getObject(2);
            if (name.equals("Zalgo")) {
                throw new RuntimeException("He comes!");
            }
            return id > -1 && name.length() > 0;
        }
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
        @Override public Boolean handle(Record cursor) {
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
        public Boolean handle(EnumIndexedCursor<Fields> cursor) {
            int id = cursor.<Integer>get(Fields.id);
            String name = cursor.<String>get(Fields.name);
            if (name.equals("Zalgo")) {
                throw new RuntimeException("He comes!");
            }
            return id > -1 && name.length() > 0;
        }
    }
    
    @Test public void
    passes_retrieved_records_to_handler_method() throws Exception {
        MethodBasedHandler handler = new MethodBasedHandler();
        EnumIndexedCursorTraverser<Fields> traverser = getMethodDispatchingCursorHandler(handler);
        
        Date before = new Date();
        
        assertThat(executeTestQuery(traverser, Fields.class), equalTo(true));
        
        Date after = new Date();
        System.out.println(String.format("Traversed 1000000 records in %s milliseconds using reflection", after.getTime() - before.getTime()));
    }
    
    @Test public void
    passes_retrieved_records_to_proxy_handler() throws Exception {
        ProxyBasedHandler handler = new ProxyBasedHandler();
        Date before = new Date();
        ProxyHandlingTraverser<Record, Fields> traverser = ProxyHandlingTraverser.proxying(Record.class, Fields.class, handler);
        
        assertThat(executeTestQuery(traverser, Fields.class), equalTo(true));
        
        Date after = new Date();
        System.out.println(String.format("Traversed 1000000 records in %s milliseconds using proxy", after.getTime() - before.getTime()));
    }
    
    @Test public void
    passes_retrieved_records_to_enum_based_handler() throws Exception {
        EnumBasedHandler handler = new EnumBasedHandler();
        Date before = new Date();
        
        assertThat(executeTestQuery(EnumIndexedCursorTraverser.forHandler(handler), Fields.class), equalTo(true));
        
        Date after = new Date();
        System.out.println(String.format("Traversed 1000000 records in %s milliseconds using enum-based handler", after.getTime() - before.getTime()));
    }
    
    @Test public void
    passes_retrieved_records_to_result_set_handler() throws Exception {
        ResultSetHandler resultSetHandler = new ResultSetBasedHandler();
        Date before = new Date();
        
        assertThat(TEST_QUERY.execute(connection).traverse(resultSetHandler), equalTo(true));
        
        Date after = new Date();
        System.out.println(String.format("Traversed 1000000 records in %s milliseconds using result set handler", after.getTime() - before.getTime()));
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
    
    public static class HibernateRecord {
        public final int id;
        public final String name;
        public HibernateRecord(int id, String name) {
            this.id = id;
            this.name = name;
        }
    }
    
    @Test public void
    hibernate_with_constructor_calls_is_less_slow() {
        Session session = createHibernateSession();
        
        ScrollableResults results = session.createQuery(String.format("SELECT new %s(p.id, p.name) FROM MyPersistable p",
                                                                      HibernateRecord.class.getName()))
                                           .setReadOnly(true)
                                           .setCacheable(false)
                                           .scroll(ScrollMode.FORWARD_ONLY);
        
        Date before = new Date();
        while (results.next()) {
            HibernateRecord persistable = (HibernateRecord) results.get()[0];
            if (persistable.name.equals("Zalgo")) {
                throw new RuntimeException("He comes!");
            }
        }
        Date after = new Date();
        System.out.println(String.format("Traversed 1000000 records in %s milliseconds using Hibernate with constructor method", after.getTime() - before.getTime()));
        
        results.close();
        session.close();
    }
    
    @SuppressWarnings("unchecked")
    @Test public void
    hibernate_with_select_map_is_not_too_shabby() {
        Session session = createHibernateSession();
        
        ScrollableResults results = session.createQuery(String.format("SELECT new map(p.id, p.name as name) FROM MyPersistable p",
                                                                      HibernateRecord.class.getName()))
                                           .setReadOnly(true)
                                           .setCacheable(false)
                                           .scroll(ScrollMode.FORWARD_ONLY);
        
        Date before = new Date();
        while (results.next()) {
            Map<String, Object> persistable = (Map<String, Object>) results.get()[0];
            if (persistable.get("name").equals("Zalgo")) {
                throw new RuntimeException("He comes!");
            }
        }
        Date after = new Date();
        System.out.println(String.format("Traversed 1000000 records in %s milliseconds using Hibernate with map selector", after.getTime() - before.getTime()));
        
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
        final EnumIndexedCursorTraverser<Fields> traverser = EnumIndexedCursorTraverser.forHandler(handler);
        
        Date before = new Date();
        session.doWork(QueryWork.executing(TEST_QUERY, Fields.class, traverser));
        Date after = new Date();
        
        System.out.println(String.format("Traversed 1000000 records in %s milliseconds using enum-based handler in a Work object inside Hibernate", after.getTime() - before.getTime()));
        
        session.close();
    }
    
    private EnumIndexedCursorTraverser<Fields> getMethodDispatchingCursorHandler(MethodBasedHandler handler) throws NoSuchMethodException {
        Method method = MethodBasedHandler.class.getMethod("handle", Integer.TYPE, String.class);
        MethodDispatcher<MethodBasedHandler, Fields> factory = MethodDispatcherFactory.dispatching(MethodBasedHandler.class, method, Fields.class, Fields.id, Fields.name);
        EnumIndexedCursorHandler<Fields> cursorHandler = factory.to(handler);
        EnumIndexedCursorTraverser<Fields> traverser = EnumIndexedCursorTraverser.forHandler(cursorHandler);
        return traverser;
    }
    
    private <E extends Enum<E>> boolean executeTestQuery(Traverser<EnumIndexedCursor<E>> traverser, Class<E> enumClass) throws SQLException {
        return executeTestQuery(traverser, enumClass, connection);
    }
    
    private <E extends Enum<E>> boolean executeTestQuery(Traverser<EnumIndexedCursor<E>> traverser, Class<E> enumClass, Connection conn) throws SQLException {
        return TEST_QUERY.execute(connection).traverse(enumClass, traverser);
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
