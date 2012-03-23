package com.youdevise.hsd;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

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
import com.youdevise.hsd.BatchInsertTest.BatchInsert;

public class HSQLDBIntegrationTest {

    private static Connection connection;
    public static interface TestDb {
        
        @Sql("SELECT id, name FROM test")
        public Query selectIdAndName();
        
        @Sql({ "INSERT INTO test",
               "(name)",
               "VALUES",
               "(?)" })
        public BatchInsert insertNames();
    }
    
    private static final TestDb testDb = MagicQuery.proxying(TestDb.class);
    
    private static final BenchmarkRunner runner = new BenchmarkRunner(10);

    private void runBenchmark(final String description, Runnable benchmark) {
        runner.runBenchmark(description, benchmark);
    }
    
    @BeforeClass public static void
    create_and_populate_table() throws SQLException {
        DriverManager.registerDriver(new org.hsqldb.jdbcDriver());
        connection = DriverManager.getConnection("jdbc:hsqldb:mem:mymemdb", "SA", "");
        
        executeStatement(
                "CREATE TABLE test (",
                "       ID INT IDENTITY,",
                "       NAME VARCHAR(255),",
                "       primary key(ID));");

        BatchInsert insert = testDb.insertNames();
        for (int i=0; i<1000000; i++) {
            insert.addValues(Integer.toString(i));
        }
        int[] ids = insert.execute(connection);
        assert(ids.length == 1000000);
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
            int id = arg.getInt(1);
            String name = arg.getString(2);
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
            return id > -1 && name.length() > 0;
        }
    }
    
    public class ProxyBasedHandler implements ProxyHandler<Record> {
        @Override public Boolean handle(Record cursor) {
            int id = cursor.getId();
            String name = cursor.getName();
            return id > -1 && name.length() > 0;
        }
    }
    
    public class EnumBasedHandler implements EnumIndexedCursorHandler<Fields> {
        @Override
        public Boolean handle(EnumIndexedCursor<Fields> cursor) {
            int id = cursor.getInt(Fields.id);
            String name = cursor.<String>get(Fields.name);
            return id > -1 && name.length() > 0;
        }
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
        final Traverser<ResultSet> traverser = Traversers.adapt(new ResultSetBasedHandler());
        runBenchmark("ResultSet handler", new Runnable() {
            @Override public void run() {
                try {
                    testDb.selectIdAndName().execute(connection, traverser);
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
                    if (!(persistable.getId() > -1 && persistable.getName().length() > 0)) {
                        break;
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
                    if (!(persistable.id > -1 && persistable.name.length() > 0)) {
                        break;
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
        EnumBasedHandler handler = new EnumBasedHandler();
        final Traverser<ResultSet> traverser = Traversers.adapt(Fields.class, handler);
        
        runBenchmark("Enum-based handler in a Work object inside Hibernate", new Runnable() {
            @Override public void run() {
                session.doWork(QueryWork.executing(testDb.selectIdAndName(), traverser));
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
            return testDb.selectIdAndName().execute(conn, Traversers.adapt(enumClass, traverser));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
