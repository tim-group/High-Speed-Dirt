package com.youdevise.hsd;

public interface ProxyHandler<T> {
    boolean handle(T proxy);
}