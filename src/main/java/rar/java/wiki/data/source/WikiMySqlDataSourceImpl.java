package rar.java.wiki.data.source;


import com.github.jasync.sql.db.Connection;
import com.github.jasync.sql.db.mysql.MySQLConnectionBuilder;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class WikiMySqlDataSourceImpl implements WikiDataSource {
    private static final RateLimiterConfig config = RateLimiterConfig.custom()
            .limitRefreshPeriod(Duration.ofMillis(100))
            .limitForPeriod(1)
            .timeoutDuration(Duration.ofDays(100000))
            .build();
    private static final RateLimiterRegistry rateLimiterRegistry = RateLimiterRegistry.of(config);
    private static final RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter("rate");

    // Connection to MySQL DB
    Connection connection = MySQLConnectionBuilder.createConnectionPool(
            "jdbc:mysql://localhost:3306/wiki?user=root&password=123456789");

    @Override
    public List<String> getLinksByTitle(String title) {
        try {
            var queryResult = RateLimiter.decorateCheckedSupplier(rateLimiter, () ->
                    connection.sendPreparedStatement(
                            "SELECT pl_title from pagelinks join page on page_id = pl_from where page_title = ? and pl_namespace = 0",
                            Collections.singletonList(title)
                    ).join());

            return queryResult.get().getRows().stream()
                    .map(row -> row.get(0))
                    .map(bytes -> new String((byte[]) bytes, StandardCharsets.UTF_8))
                    .toList();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<String> getBacklinksByTitle(String title) {
        try {
            var queryResult = RateLimiter.decorateCheckedSupplier(rateLimiter, () ->
                    connection.sendPreparedStatement(
                            "SELECT page_title from pagelinks join page on pl_from = page_id where pl_title = ? and pl_namespace = 0;",
                            Collections.singletonList(title)
                    ).join());

            return queryResult.get().getRows().stream()
                    .map(row -> row.get(0))
                    .filter(Objects::nonNull)
                    .map(bytes -> new String((byte[]) bytes, StandardCharsets.UTF_8))
                    .toList();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
