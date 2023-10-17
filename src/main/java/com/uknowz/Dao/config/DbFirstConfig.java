
package com.uknowz.Dao.config;

import com.alibaba.druid.filter.Filter;
import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.wall.WallConfig;
import com.alibaba.druid.wall.WallFilter;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import tk.mybatis.spring.annotation.MapperScan;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with apollo
 *
 * @author: yanbo
 * @date: Created in 2018-03-26 11:50
 * @description:
 */
@Data
@Slf4j
@Configuration
@ConfigurationProperties(prefix = "spring.dbfirst")
@MapperScan(basePackages = "com.uknowz.Dao", sqlSessionFactoryRef = "dbFirstPrimarySqlSessionFactory")
public class DbFirstConfig {
    //主库
    private String masterUrl;
    //从库1
    private String slave1Url;
    //从库2
    private String slave2Url;

    private String username;

    private String password;

    private String driver;

    private int maxActive;

    /**
     * timeout for connection wait
     */
    private int maxWait;

    private int initialSize;

    private int maxOpenPreparedStatementConnectionSize;

    private int minEvictableIdleTimeMillis;

    private int minIdle;

    private String validationQuery;

    private boolean testWhileIdle;

    private boolean testOnBorrow;

    private boolean testOnReturn;

    private boolean poolPreparedStatements;

    private String connectionProperties;

    private String filters;

    /**
     * 主库
     *
     * @return
     * @throws SQLException
     */
    @Bean
    public DataSource master() throws SQLException {
        return getDruidDataSource(masterUrl);
    }

    /**
     * 从库
     *
     * @return
     * @throws SQLException
     */
    @Bean
    public DataSource slave1() throws SQLException {
        return getDruidDataSource(slave1Url);
    }

    /**
     * 从库2
     *
     * @return
     * @throws SQLException
     */
    @Bean
    public DataSource slave2() throws SQLException {
        return getDruidDataSource(slave2Url);
    }


    private DruidDataSource getDruidDataSource(String url) throws SQLException {
        log.info("----------init cloud dataSource !!! ");
        log.info("driver --->: " + driver + ", url --->: " + url);
        DruidDataSource druidDataSource = new DruidDataSource();
        druidDataSource.setDriverClassName(driver);
        druidDataSource.setUrl(url);
        druidDataSource.setUsername(username);
        druidDataSource.setPassword(password);
        druidDataSource.setMaxActive(maxActive);
        druidDataSource.setInitialSize(initialSize);
        druidDataSource.setMinIdle(minIdle);
        druidDataSource.setMaxWait(maxWait);
        druidDataSource.setMinEvictableIdleTimeMillis(minEvictableIdleTimeMillis);
        druidDataSource.setValidationQuery(validationQuery);
        druidDataSource.setTestWhileIdle(testWhileIdle);
        druidDataSource.setTestOnBorrow(testOnBorrow);
        druidDataSource.setTestOnReturn(testOnReturn);
        druidDataSource.setPoolPreparedStatements(poolPreparedStatements);
        druidDataSource.setMaxOpenPreparedStatements(maxOpenPreparedStatementConnectionSize);
        druidDataSource.setConnectionProperties(connectionProperties);
        druidDataSource.setFilters(filters);
        List<Filter> filters = new ArrayList<>();
        filters.add(createWallFilter());
        druidDataSource.setProxyFilters(filters);
        return druidDataSource;
    }


    /**
     * @Primary 该注解表示在同一个接口有多个实现类可以注入的时候，默认选择哪一个，而不是让@autowire注解报错
     * @Qualifier 根据名称进行注入，通常是在具有相同的多个类型的实例的一个注入（例如有多个DataSource类型的实例）
     */
    @Bean
    public DataSourceSelector dataSourceSelector(@Qualifier("master") DataSource master,
                                                 @Qualifier("slave1") DataSource slave1,
                                                 @Qualifier("slave2") DataSource slave2) {
        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put(DynamicDataSourceHolder.DB_MASTER, master);
        targetDataSources.put(DynamicDataSourceHolder.DB_SLAVE_1, slave1);
        targetDataSources.put(DynamicDataSourceHolder.DB_SLAVE_2, slave2);

        DataSourceSelector dataSource = new DataSourceSelector();
        dataSource.setTargetDataSources(targetDataSources);// 该方法是AbstractRoutingDataSource的方法
        dataSource.setDefaultTargetDataSource(master);// 默认的datasource设置为master

        return dataSource;
    }

    /**
     * apollo配置加载顺序:
     * lhc_db -> cloud_db -> device_redis -> mqtt_redis -> mqtt -> kafka_producer -> kafka_consumer
     */
    //@DependsOn("primarySqlSessionFactory")
    @Bean(name = "dbFirstPrimarySqlSessionFactory")
    @Primary
    public SqlSessionFactory createSqlSessionFactory(@Qualifier("dataSourceSelector") DataSource dataSource) {
        log.info("=================[apollo config init] order ->: 2, config ->: cloud_db");
        log.info("----------init sqlSessionFactory !!! ");
        SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
        sqlSessionFactoryBean.setDataSource(dataSource);
        PathMatchingResourcePatternResolver resource = new PathMatchingResourcePatternResolver();
        // mybatis configuration
        try {
            org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
            // enable camel Case Table:create_time to Entity(createTime)
            configuration.setMapUnderscoreToCamelCase(true);
            sqlSessionFactoryBean.setConfiguration(configuration);
            sqlSessionFactoryBean.setMapperLocations(resource.getResources("classpath*:/mapper/dbfirst/**/*.xml"));
            sqlSessionFactoryBean.setPlugins(new Interceptor[]{new DateSourceSelectInterceptor()});
            return sqlSessionFactoryBean.getObject();
        } catch (Exception e) {
            log.error("init SqlSessionFactory failure! ", e);
            throw new RuntimeException();
        }
    }

    @Bean
    public SqlSessionTemplate createSqlSessionTemplate(SqlSessionFactory sqlSessionFactory) {
        log.info("----------init sqlSessionTemplate !!! ");
        return new SqlSessionTemplate(sqlSessionFactory);
    }

    @Bean(name = "dbFirstTransactionManager")
    public DataSourceTransactionManager transactionManager(@Qualifier("dataSourceSelector") DataSource primaryDataSource) throws SQLException {
        return new HlzDataSourceTransactionManager(primaryDataSource);
    }
    //配置mybatis的分页插件pageHelper
//    @Bean
//    public PageHelper pageHelper(){
//        PageHelper pageHelper = new PageHelper();
//        Properties properties = new Properties();
//        properties.setProperty("offsetAsPageNum","true");
//        properties.setProperty("reasonable","true");
//        properties.setProperty("dialect","myql");//配置mysql数据库的方言
//        pageHelper.setProperties(properties);
//        return pageHelper;
//    }

    public WallFilter createWallFilter() {
        WallConfig wallConfig = new WallConfig();
        // 允许一次执行多条语句
        wallConfig.setMultiStatementAllow(true);
        wallConfig.setNoneBaseStatementAllow(true);

        WallFilter wallFilter = new WallFilter();
        wallFilter.setConfig(wallConfig);

        return wallFilter;
    }
}
