package com.thock.back.market.monitoring.sql;

import org.hibernate.resource.jdbc.spi.StatementInspector;

public class MarketSqlStatementInspector implements StatementInspector {

    @Override
    public String inspect(String sql) {
        MarketRequestSqlMetricsContext.recordQuery(sql);
        return sql;
    }
}
