package rar.java.wiki;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.apache.http.client.utils.URIBuilder;
import org.jetbrains.annotations.NotNull;
import rar.kotlin.model.Link;
import rar.kotlin.model.WikiLinksResponse;

import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

public class WikiRemoteDataSourceImpl implements WikiRemoteDataSource {
    private static final String URL = "https://ru.wikipedia.org/w/api.php";

    private static final RateLimiterConfig config = RateLimiterConfig.custom()
            .limitRefreshPeriod(Duration.ofMillis(40))
            .limitForPeriod(1)
            .timeoutDuration(Duration.ofDays(100000))
            .build();
    private static final RateLimiterRegistry rateLimiterRegistry = RateLimiterRegistry.of(config);
    private static final RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter("rate");
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Override
    public List<String> getLinksByTitle(String title) {
        try {
            String responseBody = RateLimiter.decorateCheckedSupplier(rateLimiter, () ->
                    HttpClient.newBuilder()
                    .build()
                    .send(
                            HttpRequest.newBuilder()
                                .uri(getUriBuilder(title).build())
                                .GET()
                                .build(),
                            HttpResponse.BodyHandlers.ofString()
                    )
                    .body()
            ).get();
            System.out.println("Got links from: " + title);
            WikiLinksResponse response = objectMapper.readValue(responseBody, WikiLinksResponse.class);

            return parseResponse(response);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    private static URIBuilder getUriBuilder(String title) throws URISyntaxException {
        URIBuilder uriBuilder = new URIBuilder(URL);
        uriBuilder.addParameter("action", "query");
        uriBuilder.addParameter("prop", "links");
        uriBuilder.addParameter("pllimit", "max");
        uriBuilder.addParameter("format", "json");
        uriBuilder.addParameter("plnamespace", "titles");
        uriBuilder.addParameter("titles", title);
        return uriBuilder;
    }

    @NotNull
    private static List<String> parseResponse(WikiLinksResponse response) {
        return response.getQuery().getPages().entrySet().iterator().next().getValue().getLinks()
                .stream()
                .map(Link::getTitle)
                .toList();
    }

    @Override
    public List<String> getBacklinksByTitle(String title) {
        return null;
    }
}
