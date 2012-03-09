package com.youdevise.hsd;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AnnotationConfiguration;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.theories.DataPoints;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;

public class HSQLDBIntegrationTest {

    private static Connection connection;
    private static final Query TEST_QUERY = new Query("SELECT id, name FROM test", new Object[] {});
    
    @BeforeClass public static void
    create_and_populate_table() throws SQLException {
        DriverManager.registerDriver(new org.hsqldb.jdbcDriver());
        connection = DriverManager.getConnection("jdbc:hsqldb:mem:mymemdb", "SA", "");
        executeStatement(
                "CREATE TABLE test (",
                "       id INT IDENTITY,",
                "       name VARCHAR(255),",
                "       primary key(id));");

        for (int i=0; i<1000000; i++) {
            executeStatement("INSERT INTO test (name) values ('" + Integer.toString(i) + "');");
        }
    }
    
    @AfterClass public static void
    drop_table() throws SQLException {
        executeStatement("DROP TABLE test;");
    }
    
    @DataPoints public static final boolean[] WARM_UP = new boolean[] { true, false };
    
    private static void executeStatement(String...sql) throws SQLException {
        Statement statement = connection.createStatement();
        try {
            statement.execute(Joiner.on("\n").join(sql));
        } finally {
            statement.close();
        }
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
    
    public void runBenchmark(final String description, Runnable benchmark) {
        long warmupTime = 0;
        long firstTime = 0;
        long totalTime = 0;
        for (int i = 0; i<11; i++) {
            long start = System.nanoTime();
            benchmark.run();
            long end = System.nanoTime();
            switch(i) {
                case 0:
                    warmupTime = end - start;
                    break;
                case 1:
                    firstTime = end - start;
                default:
                    totalTime += end - start;
            }
        }
        System.out.println(description);
        System.out.println(Strings.repeat("=", description.length()));
        System.out.println(String.format("Avg over 10 runs: %d ms", totalTime / 10000000));
        System.out.println(String.format("Warmup: %d ms", warmupTime / 1000000));
        System.out.println(String.format("First run: %d ms", firstTime / 1000000));
        System.out.println();
    }
    
    @Test public void
    passes_retrieved_records_to_handler_method() throws Exception {
        MethodBasedHandler handler = new MethodBasedHandler();
        final EnumIndexedCursorTraverser<Fields> traverser = getMethodDispatchingCursorHandler(handler);
        
        runBenchmark("Reflection", new Runnable() {
            @Override public void run() {
                executeTestQuery(traverser, Fields.class);
            }
        });
    }
    
    @Test public void
    passes_retrieved_records_to_proxy_handler() throws Exception {
        ProxyBasedHandler handler = new ProxyBasedHandler();
        final ProxyHandlingTraverser<Record, Fields> traverser = ProxyHandlingTraverser.proxying(Record.class, Fields.class, handler);
        
        runBenchmark("Proxy", new Runnable() {
            @Override public void run() {
                executeTestQuery(traverser, Fields.class);
            }
        });
    }
    
    @Test public void
    passes_retrieved_records_to_enum_based_handler() throws Exception {
        EnumBasedHandler handler = new EnumBasedHandler();
        final EnumIndexedCursorTraverser<Fields> traverser = EnumIndexedCursorTraverser.forHandler(handler);
        
        runBenchmark("Enum-based handler", new Runnable() {
            @Override public void run() {
                executeTestQuery(traverser, Fields.class);
            }
        });
    }
    
    @Test public void
    passes_retrieved_records_to_result_set_handler() throws Exception {
        final ResultSetHandler resultSetHandler = new ResultSetBasedHandler();
        runBenchmark("ResultSet handler", new Runnable() {
            @Override public void run() {
                try {
                    TEST_QUERY.execute(connection).traverse(resultSetHandler);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    
    @Test public void
    hibernate_is_slow() {
        final Session session = createHibernateSession();
        
        runBenchmark("Hibernate", new Runnable() {
            @Override public void run() {
                final ScrollableResults results = session.createQuery("SELECT p FROM MyPersistable p")
                        .setReadOnly(true)
                        .setCacheable(false)
                        .scroll(ScrollMode.FORWARD_ONLY);

                while (results.next()) {
                    MyPersistable persistable = (MyPersistable) results.get()[0];
                    if (persistable.getName().equals("Zalgo")) {
                        throw new RuntimeException("He comes!");
                    }
                }
                results.close();
            }
        });

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
        final Session session = createHibernateSession();
        
        runBenchmark("Hibernate with constructor expression", new Runnable() {
            @Override public void run() {
                final ScrollableResults results = session.createQuery(String.format("SELECT new %s(p.id, p.name) FROM MyPersistable p",
                                                                                    HibernateRecord.class.getName()))
                                                                                    .setReadOnly(true)
                                                                                    .setCacheable(false)
                                                                                    .scroll(ScrollMode.FORWARD_ONLY);
                
                while (results.next()) {
                    HibernateRecord persistable = (HibernateRecord) results.get()[0];
                    if (persistable.name.equals("Zalgo")) {
                        throw new RuntimeException("He comes!");
                    }
                }
                results.close();
            }
        });

        session.close();
    }
    
    @SuppressWarnings("unchecked")
    @Test public void
    hibernate_with_select_map_is_not_too_shabby() {
        final Session session = createHibernateSession();
        
        runBenchmark("Hibernate with map selector", new Runnable() {
            @Override public void run() {
                final ScrollableResults results = session.createQuery(String.format("SELECT new map(p.id, p.name as name) FROM MyPersistable p",
                                                                                    HibernateRecord.class.getName()))
                                                                                    .setReadOnly(true)
                                                                                    .setCacheable(false)
                                                                                    .scroll(ScrollMode.FORWARD_ONLY);
                while (results.next()) {
                    Map<String, Object> persistable = (Map<String, Object>) results.get()[0];
                    if (persistable.get("name").equals("Zalgo")) {
                        throw new RuntimeException("He comes!");
                    }
                }
                results.close();
            }
        });
        
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
        final Session session = createHibernateSession();
        final EnumBasedHandler handler = new EnumBasedHandler();
        final EnumIndexedCursorTraverser<Fields> traverser = EnumIndexedCursorTraverser.forHandler(handler);
        
        runBenchmark("Enum-based handler in a Work object inside Hibernate", new Runnable() {
            @Override public void run() {
                session.doWork(QueryWork.executing(TEST_QUERY, Fields.class, traverser));
            }
        });

        session.close();
    }
    
    private EnumIndexedCursorTraverser<Fields> getMethodDispatchingCursorHandler(MethodBasedHandler handler) throws NoSuchMethodException {
        Method method = MethodBasedHandler.class.getMethod("handle", Integer.TYPE, String.class);
        MethodDispatcher<MethodBasedHandler, Fields> factory = MethodDispatcherFactory.dispatching(MethodBasedHandler.class, method, Fields.class, Fields.id, Fields.name);
        EnumIndexedCursorHandler<Fields> cursorHandler = factory.to(handler);
        EnumIndexedCursorTraverser<Fields> traverser = EnumIndexedCursorTraverser.forHandler(cursorHandler);
        return traverser;
    }
    
    private <E extends Enum<E>> boolean executeTestQuery(Traverser<EnumIndexedCursor<E>> traverser, Class<E> enumClass) {
        return executeTestQuery(traverser, enumClass, connection);
    }
    
    private <E extends Enum<E>> boolean executeTestQuery(Traverser<EnumIndexedCursor<E>> traverser, Class<E> enumClass, Connection conn) {
        try {
            return TEST_QUERY.execute(connection).traverse(enumClass, traverser);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
