package com.youdevise.hsd;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.EnumMap;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Function;

public class MetadataToEnumMapTransformerTest {
    private final Mockery context = new Mockery();
    
    private final ResultSet resultSet = context.mock(ResultSet.class);
    private final ResultSetMetaData metadata = context.mock(ResultSetMetaData.class);
    
    @Before public void
    setup_mock_resultset() throws SQLException {
        context.checking(new Expectations() {{
            allowing(resultSet).getMetaData(); will(returnValue(metadata));
            
            allowing(metadata).getColumnCount(); will(returnValue(3));
            allowing(metadata).getColumnName(2); will(returnValue("foo"));
            allowing(metadata).getColumnName(1); will(returnValue("bar"));
            allowing(metadata).getColumnName(0); will(returnValue("baz"));
        }});
    }
    
    public static enum TestEnum1 {
        foo,
        bar,
        baz
    }
    
    @Test public void
    maps_enum_values_to_field_indices() {
        MetadataToEnumMapTransformer<TestEnum1> transformer = MetadataToEnumMapTransformer.forEnumClass(TestEnum1.class);
        
        EnumMap<TestEnum1, Integer> indices = transformer.apply(metadata);
        
        MatcherAssert.assertThat(indices.get(TestEnum1.foo), Matchers.is(2));
        MatcherAssert.assertThat(indices.get(TestEnum1.bar), Matchers.is(1));
        MatcherAssert.assertThat(indices.get(TestEnum1.baz), Matchers.is(0));
    }
    
    public static enum TestEnum2 {
        FOO,
        BAR,
        BAZ
    }
    
    @SuppressWarnings("unchecked")
    @Test public void
    uses_supplied_transformer_to_map_enum_values_to_column_names() {
        final Function<TestEnum2, String> columnNameTransformer = context.mock(Function.class);
        
        context.checking(new Expectations() {{
            oneOf(columnNameTransformer).apply(TestEnum2.FOO); will(returnValue("foo"));
            oneOf(columnNameTransformer).apply(TestEnum2.BAR); will(returnValue("bar"));
            oneOf(columnNameTransformer).apply(TestEnum2.BAZ); will(returnValue("baz"));
        }});
        
        MetadataToEnumMapTransformer<TestEnum2> transformer = MetadataToEnumMapTransformer.forEnumClass(TestEnum2.class, columnNameTransformer);
        
        EnumMap<TestEnum2, Integer> indices = transformer.apply(metadata);
        
        MatcherAssert.assertThat(indices.get(TestEnum2.FOO), Matchers.is(2));
        MatcherAssert.assertThat(indices.get(TestEnum2.BAR), Matchers.is(1));
        MatcherAssert.assertThat(indices.get(TestEnum2.BAZ), Matchers.is(0));
        
        context.assertIsSatisfied();
    }

}
