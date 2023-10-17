package com.uknowz.Dao.config;

public class DynamicDataSourceHolder {

    /**用来存取key，ThreadLocal保证了线程安全*/
    private static ThreadLocal<String> contextHolder = new ThreadLocal<String>();
    /**主库*/
    public static final String DB_MASTER = "master";
    /**从库1*/
    public static final String DB_SLAVE_1 = "slave1";
    /**从库2*/
    public static final String DB_SLAVE_2 = "slave2";

    /**
     * 获取线程的数据源
     * @return
     */
    public static String getDataSourceType() {
        String db = contextHolder.get();
        if (db == null){
            //如果db为空则默认使用主库（因为主库支持读和写）
            db = DB_MASTER;
        }
        return db;
    }

    /**
     * 设置线程的数据源
     * @param s
     */
    public static void setDataSourceType(String s) {
        contextHolder.set(s);
    }

    /**
     * 清理连接类型
     */
    public static void clearDataSource(){
        contextHolder.remove();
    }
}
