package com.youdevise.hsd;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.Sequence;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class QueryTest {

    public static enum Fields {
        foo,
        bar,
        baz
    }
    
    private final Mockery context = new Mockery();
    private final Connection connection = context.mock(Connection.class);
    private final PreparedStatement statement = context.mock(PreparedStatement.class);
    private final ResultSet resultSet = context.mock(ResultSet.class);
    private final ResultSetHandler handler = context.mock(ResultSetHandler.class);
    
    @Test public void
    a_query_executes_against_the_supplied_connection_with_the_supplied_parameters() throws SQLException {
        final String sql = "SELECT foo FROM bar";
        final Date aDate = new Date();
        final Object[] parameters = new Object[] { "foo", 12, aDate };
        Query query = new Query(sql, parameters);
        
        context.checking(new Expectations() {{
            oneOf(connection).prepareStatement(sql); will(returnValue(statement));
            oneOf(statement).setObject(1, "foo");
            oneOf(statement).setObject(2, 12);
            oneOf(statement).setObject(3, aDate);
            
            allowing(statement).execute(); will(returnValue(true));
            allowing(statement).getResultSet(); will(returnValue(resultSet));

            final Sequence recordIteration = context.sequence("recordIteration");
            oneOf(resultSet).next(); will(returnValue(true)); inSequence(recordIteration);
            oneOf(handler).handle(resultSet); will(returnValue(true)); inSequence(recordIteration);
            oneOf(resultSet).next(); will(returnValue(false)); inSequence(recordIteration);
            
            oneOf(resultSet).close();
            oneOf(statement).close();
        }});
        
        boolean result = query.execute(connection, Traversers.adapt(handler));
        context.assertIsSatisfied();
        assertThat(result, is(true));
    }
    
}
