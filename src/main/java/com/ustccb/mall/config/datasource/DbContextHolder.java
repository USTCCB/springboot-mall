package com.ustccb.mall.config.datasource;

/**
 * 数据库上下文持有者：使用 ThreadLocal 管理当前线程的数据源选择
 */
public class DbContextHolder {
    private static final ThreadLocal<String> CONTEXT_HOLDER = new ThreadLocal<>();

    public static final String WRITE_DATASOURCE = "master";
    public static final String READ_DATASOURCE = "slave";

    public static void setDbType(String dbType) {
        CONTEXT_HOLDER.set(dbType);
    }

    public static String getDbType() {
        return CONTEXT_HOLDER.get();
    }

    public static void clearDbType() {
        CONTEXT_HOLDER.remove();
    }
}
