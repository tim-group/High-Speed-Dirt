package com.youdevise.hsd;

import java.sql.SQLException;

public interface Handler<T, R> {
    R handle(T arg) throws SQLException;
}
