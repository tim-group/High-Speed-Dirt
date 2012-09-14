package com.youdevise.hsd;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ResultSetTraverser implements Traverser<ResultSet> {

    public static ResultSetTraverser forHandler(SQLHandler<ResultSet> handler) {
        return new ResultSetTraverser(handler);
    }
    
    private final SQLHandler<ResultSet> handler;
    private ResultSetTraverser(SQLHandler<ResultSet> handler) {
        this.handler = handler;
    }
    
    @Override
    public boolean traverse(ResultSet resultSet) throws SQLException {
        while(resultSet.next()) {
            if (!handler.handle(resultSet)) { return false; }
        }
        return true;
    }

}
