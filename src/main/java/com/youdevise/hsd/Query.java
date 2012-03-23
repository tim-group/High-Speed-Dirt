package com.youdevise.hsd;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import com.google.common.collect.Lists;

public class Query {

    private final String sql;
    private final Object[] parameters;
    
    public Query(String sql, Object[] parameters) {
        this.sql = sql;
        this.parameters = parameters;
    }
    
    public boolean execute(Connection connection, Traverser<ResultSet> traverser) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(sql);
        try {
            setParameters(statement);
            statement.execute();
            ResultSet resultSet = statement.getResultSet();
            try {
                return traverser.traverse(resultSet);
            } finally {
                resultSet.close();
            }
        } finally {
            statement.close();
        }
    }
    
    private void setParameters(PreparedStatement statement) throws SQLException {
        if (parameters == null) {
            return;
        }
        for (int i = 0; i < parameters.length; i++) {
            statement.setObject(i + 1, parameters[i]);
        }
    }

    public String sql() {
        return sql;
    }
    
    public List<Object> parameters() {
        return Lists.newArrayList(parameters);
    }
}