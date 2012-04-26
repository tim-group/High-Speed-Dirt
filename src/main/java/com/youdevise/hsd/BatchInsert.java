package com.youdevise.hsd;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import com.google.common.collect.Lists;

public class BatchInsert {
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