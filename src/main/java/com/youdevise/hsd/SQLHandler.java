package com.youdevise.hsd;

import java.sql.SQLException;

public interface SQLHandler<T> {
    boolean handle(T record) throws SQLException;
}
