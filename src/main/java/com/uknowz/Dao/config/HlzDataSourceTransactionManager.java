package com.uknowz.Dao.config;

import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionDefinition;

import javax.sql.DataSource;

public class HlzDataSourceTransactionManager extends DataSourceTransactionManager {

    public HlzDataSourceTransactionManager(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    protected void doBegin(Object transaction, TransactionDefinition definition) {
        DynamicDataSourceHolder.setDataSourceType(DynamicDataSourceHolder.DB_MASTER);
        super.doBegin(transaction, definition);
    }
}
