package com.youdevise.hsd;

public enum QueryType {
    SELECT("select"),
    INSERT("insert"),
    UPDATE("update"),
    DELETE("delete");
    
    private String keyword;
    private QueryType(String keyword) {
        this.keyword = keyword;
    }
    
    public boolean matches(String sql) {
        return sql.trim().toLowerCase().startsWith(keyword);
    }
    
    public static QueryType forSql(String sql) {
        for (QueryType queryType : QueryType.values()) {
            if (queryType.matches(sql)) {
                return queryType;
            }
        }
        throw new IllegalArgumentException("Query type not recognised for query: " + sql);
    }
}