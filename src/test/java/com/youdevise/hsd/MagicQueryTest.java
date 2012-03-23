package com.youdevise.hsd;

import java.util.Date;

import org.junit.Test;

import com.youdevise.hsd.BatchInsertTest.BatchInsert;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;

public class MagicQueryTest {

    public static interface TestInterface {
    
        @Sql({ "SELECT foo",
               "FROM bar",
               "WHERE baz=?" })
        Query selectFoo(int baz);
        
        @Sql({ "INSERT INTO xyzzy",
               "(foo, bar, baz)",
               "VALUES",
               "(?, ?, ?" })
        BatchInsert insertIntoXyzzy(String foo, int bar, Date baz);
    }
    
    @Test public void
    proxies_method_call_to_create_query_with_parameters() {
        TestInterface testInterface = MagicQuery.proxying(TestInterface.class);
        Query query = testInterface.selectFoo(100);
       
        assertThat(query.sql(), is("SELECT foo\nFROM bar\nWHERE baz=?"));
        assertThat(query.parameters(), hasItem(100));
    }
    
    @Test public void
    proxies_method_call_to_create_batch_insert() {
        BatchInsert insert = MagicQuery.proxying(TestInterface.class).insertIntoXyzzy("foo", 23, new Date())
                                                                     .addValues("foo2", 42, new Date());
        
        assertThat(insert.rows(), is(2));
    }
    
}
