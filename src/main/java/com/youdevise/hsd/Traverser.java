package com.youdevise.hsd;

import java.sql.SQLException;

public interface Traverser<T> {
    boolean traverse(T arg) throws SQLException;
}
