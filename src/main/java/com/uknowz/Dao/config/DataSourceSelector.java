package com.uknowz.Dao.config;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * @description 继承了AbstractRoutingDataSource，动态选择数据源
 */

public class DataSourceSelector extends AbstractRoutingDataSource {

    @Override
    protected Object determineCurrentLookupKey() {
    /*    StackTraceElement[] stackTraces = Thread.currentThread().getStackTrace();
        for (StackTraceElement stackTrace : stackTraces) {
            if ("DataSourceTransactionManager.java".equals(stackTrace.getFileName())) {
                DynamicDataSourceHolder.setDataSourceType(DynamicDataSourceHolder.DB_MASTER);
                break;
            }
        }*/
        return DynamicDataSourceHolder.getDataSourceType();
    }
}