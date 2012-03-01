package com.youdevise.hsd;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Test;

import com.google.common.base.Function;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class DefaultColumnNameTransformerTest {

    private final Mockery context = new Mockery();
    
    public enum TestEnum {
        foo,
        @Column("bar_id") bar,
        bazId
    }
    
    @Test public void
    converts_enum_value_name_to_string() {
        DefaultColumnNameTransformer<TestEnum> transformer = DefaultColumnNameTransformer.forEnumClass(TestEnum.class);
        
        assertThat(transformer.apply(TestEnum.foo), equalTo("foo"));
    }
    
    @Test public void
    takes_column_name_from_annotation_where_supplied() {
        DefaultColumnNameTransformer<TestEnum> transformer = DefaultColumnNameTransformer.forEnumClass(TestEnum.class);
        
        assertThat(transformer.apply(TestEnum.bar), equalTo("bar_id"));
    }
    
    @SuppressWarnings("unchecked")
    @Test public void
    converts_enum_values_using_name_transformer_if_supplied() {
        final Function<String, String> nameTransformer = context.mock(Function.class);

        context.checking(new Expectations() {{
            oneOf(nameTransformer).apply("bazId"); will(returnValue("baz_id"));
        }});
            
        DefaultColumnNameTransformer<TestEnum> transformer = DefaultColumnNameTransformer.forEnumClass(TestEnum.class, nameTransformer);
        
        assertThat(transformer.apply(TestEnum.bazId), equalTo("baz_id"));
        context.assertIsSatisfied();
    }
    
    @SuppressWarnings("unchecked")
    @Test public void
    does_not_convert_names_provided_by_annotation() {
        final Function<String, String> nameTransformer = context.mock(Function.class);

        context.checking(new Expectations() {{
            never(nameTransformer);
        }});
            
        DefaultColumnNameTransformer<TestEnum> transformer = DefaultColumnNameTransformer.forEnumClass(TestEnum.class, nameTransformer);
        
        assertThat(transformer.apply(TestEnum.bar), equalTo("bar_id"));
        context.assertIsSatisfied();
    }

}
