package com.youdevise.hsd;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Test;

import com.google.common.collect.Lists;

public class BatchInsertTest {

    private final Mockery context = new Mockery();

    private final Connection connection = context.mock(Connection.class);
    private final PreparedStatement statement = context.mock(PreparedStatement.class);

    public static class BatchInsert {
        private final String sql;
        private final List<Object[]> valueSets = Lists.newLinkedList();

        public BatchInsert(String sql) {
            this.sql = sql;
        }

        public String sql() {
            return sql;
        }

        public BatchInsert addValues(Object... values) {
            valueSets.add(values);
            return this;
        }

        public int[] execute(Connection connection) throws SQLException {
            boolean autoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                PreparedStatement statement = connection.prepareStatement(sql);
                try {
                    for (Object[] params : valueSets) {
                        setParameters(statement, params);
                        statement.addBatch();
                    }
                    return statement.executeBatch();
                } finally {
                    statement.close();
                }
            } finally {
                connection.setAutoCommit(autoCommit);
            }
        }

        private void setParameters(PreparedStatement statement, Object[] params) throws SQLException {
            for (int i = 0; i < params.length; i++) {
                statement.setObject(i + 1, params[i]);
            }
        }

        public int rows() {
            return valueSets.size();
        }
    }

    @Test
    public void accumulates_parameter_sets() throws SQLException {
        BatchInsert batchInsert = new BatchInsert("INSERT INTO xyzzy (foo, bar) VALUES (?, ?)");
        batchInsert.addValues("foo1", "bar1");
        batchInsert.addValues("foo2", "bar2");
        batchInsert.addValues("foo3", "bar3");

        context.checking(new Expectations() {
            {
                allowing(connection).getAutoCommit(); will(returnValue(true));
                oneOf(connection).setAutoCommit(false);
                allowing(connection).prepareStatement("INSERT INTO xyzzy (foo, bar) VALUES (?, ?)");
                will(returnValue(statement));

                exactly(3).of(statement).addBatch();
                oneOf(statement).setObject(1, "foo1");
                oneOf(statement).setObject(1, "foo2");
                oneOf(statement).setObject(1, "foo3");
                oneOf(statement).setObject(2, "bar1");
                oneOf(statement).setObject(2, "bar2");
                oneOf(statement).setObject(2, "bar3");

                oneOf(statement).executeBatch(); will(returnValue(new int[] { 1, 1 }));
                oneOf(statement).close();
                oneOf(connection).setAutoCommit(true);
            }
        });

        batchInsert.execute(connection);

        context.assertIsSatisfied();
    }

}
