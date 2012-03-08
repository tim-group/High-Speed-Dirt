package com.youdevise.hsd;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class QueryResult {

    private final String sql;
    private final Object[] parameters;
    private final Connection connection;

    public QueryResult(String sql, Object[] parameters, Connection connection) {
        this.sql = sql;
        this.parameters = parameters;
        this.connection = connection;
    }
    
    public boolean traverse(Traverser<ResultSet> traverser) throws SQLException {
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

    public boolean traverse(Handler<ResultSet, Boolean> resultSetHandler) throws SQLException {
        return traverse(ResultSetTraverser.forHandler(resultSetHandler));
    }

    public <E extends Enum<E>> boolean traverse(Class<E> enumClass, Traverser<EnumIndexedCursor<E>> traverser) throws SQLException {
        AdaptingTraverser<ResultSet, EnumIndexedCursor<E>> adapter = AdaptingTraverser.adapting(ResultSetTransformer.transforming(enumClass),
                                                                                                traverser);
        return traverse(adapter);
    }

    public <E extends Enum<E>> boolean traverse(Class<E> enumClass, Handler<EnumIndexedCursor<E>, Boolean> cursorHandler)
            throws SQLException {
        return traverse(enumClass, EnumIndexedCursorTraverser.forHandler(cursorHandler));
    }

    public <T, E extends Enum<E>> boolean traverse(Class<T> proxyClass, Class<E> enumClass, Handler<T, Boolean> proxyHandler)
            throws SQLException {
        return traverse(enumClass, ProxyHandlingTraverser.proxying(proxyClass, enumClass, proxyHandler));
    }

    private void setParameters(PreparedStatement statement) throws SQLException {
        for (int i = 0; i < parameters.length; i++) {
            statement.setObject(i + 1, parameters[i]);
        }
    }
}
