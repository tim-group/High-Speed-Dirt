package com.youdevise.hsd;

import java.sql.SQLException;

import com.google.common.base.Function;

public class AdaptingTraverser<T1, T2> implements Traverser<T1> {
    
    public static <T1, T2> AdaptingTraverser<T1, T2> adapting(Function<T1, T2> adapter, Traverser<T2> traverser) {
        return new AdaptingTraverser<T1, T2>(adapter, traverser);
    }
    
    private final Traverser<T2> traverser;
    private final Function<T1, T2> adapter;
    
    private AdaptingTraverser(Function<T1, T2> adapter, Traverser<T2> traverser) {
        this.adapter = adapter;
        this.traverser = traverser;
    }
    
    @Override
    public boolean traverse(T1 arg) throws SQLException {
        return traverser.traverse(adapter.apply(arg));
    }

}
