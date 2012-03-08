package com.youdevise.hsd;

import java.sql.Connection;
import java.sql.SQLException;

import org.hibernate.jdbc.Work;

public class QueryWork<E extends Enum<E>> implements Work {
    
    public static <E extends Enum<E>> QueryWork<E> executing(Query query, Class<E> enumClass, EnumIndexedCursorTraverser<E> traverser) {
        return new QueryWork<E>(query, enumClass, traverser);
    }
    
    private final EnumIndexedCursorTraverser<E> traverser;
    private final Query query;
    private final Class<E> enumClass;
    private boolean completed = false;
    
    private QueryWork(Query query, Class<E> enumClass, EnumIndexedCursorTraverser<E> traverser) {
        this.traverser = traverser;
        this.query = query;
        this.enumClass = enumClass;
    }

    @Override
    public void execute(Connection hibernateConnection) throws SQLException {
        completed = query.execute(hibernateConnection).traverse(enumClass, traverser);
    }
    
    public boolean completed() { return completed; }
}