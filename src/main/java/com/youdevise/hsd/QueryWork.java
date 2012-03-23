package com.youdevise.hsd;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.jdbc.Work;

public class QueryWork<E extends Enum<E>> implements Work {
    
    public static <E extends Enum<E>> QueryWork<E> executing(Query query, Traverser<ResultSet> traverser) {
        return new QueryWork<E>(query, traverser);
    }
    
    private final Traverser<ResultSet> traverser;
    private final Query query;
    private boolean completed = false;
    
    private QueryWork(Query query, Traverser<ResultSet> traverser) {
        this.traverser = traverser;
        this.query = query;
    }

    @Override
    public void execute(Connection hibernateConnection) throws SQLException {
        completed = query.execute(hibernateConnection, traverser);
    }
    
    public boolean completed() { return completed; }
}