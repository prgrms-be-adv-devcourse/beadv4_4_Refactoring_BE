package com.thock.back.market.monitoring.sql;

import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class MarketSqlMetricsConfiguration {

    @Bean
    public MarketSqlStatementInspector marketSqlStatementInspector() {
        return new MarketSqlStatementInspector();
    }

    @Bean
    public HibernatePropertiesCustomizer hibernateStatementInspectorCustomizer(
            MarketSqlStatementInspector marketSqlStatementInspector
    ) {
        return hibernateProperties -> hibernateProperties.put(
                "hibernate.session_factory.statement_inspector",
                marketSqlStatementInspector
        );
    }
}
