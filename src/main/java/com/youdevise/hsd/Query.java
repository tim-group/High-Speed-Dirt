package com.youdevise.hsd;

import java.sql.Connection;
import java.util.List;

import com.google.common.collect.Lists;

public class Query {

    private final String sql;
    private final Object[] parameters;
    
    public Query(String sql, Object[] parameters) {
        this.sql = sql;
        this.parameters = parameters;
    }
    
    public QueryResult execute(Connection connection) {
        return new QueryResult(sql, parameters, connection);
    }
    
    public String sql() { return sql; }
    public List<Object> parameters() { return Lists.newArrayList(parameters); }
}