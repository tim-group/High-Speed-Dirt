package com.youdevise.hsd;

import java.sql.Connection;

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
}