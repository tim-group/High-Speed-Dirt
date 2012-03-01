package com.youdevise.hsd;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Date;
import java.util.EnumMap;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Before;
import org.junit.Test;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class ResultSetAdapterTest {

    private final Mockery context = new Mockery();
    
    private final ResultSet resultSet = context.mock(ResultSet.class);
    private final ResultSetMetaData metadata = context.mock(ResultSetMetaData.class);
    private final Date theDate = new Date();
    
    @Before public void
    setup_stub_resultset() throws SQLException {
        context.checking(new Expectations() {{
            allowing(resultSet).getMetaData(); will(returnValue(metadata));
            
            allowing(metadata).getColumnCount(); will(returnValue(3));
            allowing(metadata).getColumnName(3); will(returnValue("foo"));
            allowing(metadata).getColumnName(2); will(returnValue("bar"));
            allowing(metadata).getColumnName(1); will(returnValue("baz_id"));
            
            allowing(resultSet).getObject(3); will(returnValue("Hello"));
            allowing(resultSet).getObject(2); will(returnValue(23));
            allowing(resultSet).getObject(1); will(returnValue(theDate));
        }});
    }
    
    public static interface Record {
        public enum Fields {
            foo,
            bar,
            @Column("baz_id") baz
        }
        
        public String getFoo();
        public int getBar();
        public Date getBaz();
    }
    
    @Test public void
    looks_up_key_indices_in_recordset_metadata_using_column_annotations_where_present() throws SQLException {
        ResultSetAdapter<Record.Fields> adapter = ResultSetAdapter.adapting(resultSet, Record.Fields.class);

        assertThat(adapter.<String>get(Record.Fields.foo), equalTo("Hello"));
        assertThat(adapter.<Integer>get(Record.Fields.bar), equalTo(23));
        assertThat(adapter.<Date>get(Record.Fields.baz), equalTo(theDate));
    }
    
    @Test public void
    exports_result_data_as_enum_map() throws SQLException {
        ResultSetAdapter<Record.Fields> adapter = ResultSetAdapter.adapting(resultSet, Record.Fields.class);
        EnumMap<Record.Fields, Object> values = adapter.values();
        
        assertThat((String) values.get(Record.Fields.foo), equalTo("Hello"));
        assertThat((Integer) values.get(Record.Fields.bar), equalTo(23));
        assertThat((Date) values.get(Record.Fields.baz), equalTo(theDate));
    }
}
