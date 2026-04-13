package com.thock.back.market.monitoring.sql;

import net.ttddyy.dsproxy.listener.QueryExecutionListener;
import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.QueryInfo;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import javax.sql.DataSource;

@Configuration
public class MarketDatasourceProxyConfiguration {

    @Bean
    public BeanPostProcessor marketDataSourceProxyBeanPostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if (!(bean instanceof DataSource dataSource)) {
                    return bean;
                }

                if (beanName != null && beanName.contains("proxy")) {
                    return bean;
                }

                QueryExecutionListener listener = new QueryExecutionListener() {
                    @Override
                    public void beforeQuery(ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {
                    }

                    @Override
                    public void afterQuery(ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {
                        MarketRequestSqlMetricsContext.recordQueryTime(execInfo.getElapsedTime());
                    }
                };

                return ProxyDataSourceBuilder
                        .create(dataSource)
                        .name("market-datasource-proxy")
                        .listener(listener)
                        .build();
            }
        };
    }
}
