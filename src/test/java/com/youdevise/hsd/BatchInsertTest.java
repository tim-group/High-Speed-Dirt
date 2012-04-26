package com.youdevise.hsd;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Test;


public class BatchInsertTest {

    private final Mockery context = new Mockery();

    private final Connection connection = context.mock(Connection.class);
    private final PreparedStatement statement = context.mock(PreparedStatement.class);

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
