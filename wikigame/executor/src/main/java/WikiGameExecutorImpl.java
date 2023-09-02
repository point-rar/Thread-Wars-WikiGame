import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.apache.http.client.utils.URIBuilder;
import org.jetbrains.annotations.NotNull;
import point.rar.model.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class WikiGameExecutorImpl implements WikiGame {
    private static final String URL = "https://ru.wikipedia.org/w/api.php";
    private static final RateLimiterConfig config = RateLimiterConfig.custom()
            .limitRefreshPeriod(Duration.ofMillis(50))
            .limitForPeriod(10)
            .timeoutDuration(Duration.ofMillis(5))
            .build();
    private static final RateLimiterRegistry rateLimiterRegistry = RateLimiterRegistry.of(config);
    private static final RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter("rate");

    private static AtomicBoolean isFinished = new AtomicBoolean(false);

    @NotNull
    @Override
    public List<String> play(@NotNull String startPageTitle, @NotNull String endPageTitle, int maxDepth) {
        var startedPage = new Page(startPageTitle, null);
        Queue<Page> rawPages = new ConcurrentLinkedQueue<>(Collections.singleton(startedPage));
        Queue<Page> parsedPages = new ConcurrentLinkedQueue<>();
        Set<String> receivedLinks = new ConcurrentSkipListSet<>();

        ExecutorService exec = Executors.newVirtualThreadPerTaskExecutor();

        do {
            exec.execute(makeSearch(rawPages, parsedPages, receivedLinks, endPageTitle));
        } while (!isFinished.get());
        exec.shutdown();
        exec.close();

        parsedPages.add(
                new Page(endPageTitle,
                        rawPages.stream()
                                .filter(p -> p.getTitle().equals(endPageTitle))
                                .findAny()
                                .orElseThrow()
                )
        );

        return getResultPath(parsedPages, endPageTitle);
    }

    public static Runnable makeSearch(Queue<Page> rawPages, Queue<Page> parsedPages, Set<String> receivedLinks, String endPageTitle) {
        return () -> {
            Page curPage = rawPages.poll();
            if (curPage != null) {
                List<String> newLinks = getChildLinks(curPage.getTitle());
                if (newLinks != null) {
                    parsedPages.add(curPage);
                    for (String link : newLinks) {
                        if (receivedLinks.add(link)) {
                            rawPages.add(new Page(link, curPage));
                        }
                        if (endPageTitle.equals(link)) {
                            isFinished.set(true);
                            break;
                        }
                    }
                } else {
                    rawPages.add(curPage);
                }
            }
        };
    }

    private static List<String> getResultPath(Queue<Page> parsedPages, String endPageTitle) {
        var path = new ArrayList<String>();
        var curPage = parsedPages.stream()
                .filter(p -> p.getTitle().equals(endPageTitle))
                .findAny()
                .orElseThrow();
        while (curPage.getParentPage() != null) {
            curPage = curPage.getParentPage();
            path.add(curPage.getTitle());
        }
        Collections.reverse(path); // Reverse the order of elements in the list
        return path;
    }


    private static List<String> getChildLinks(String title) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        try {
            System.out.println("Get links for: " + title);
            System.out.println(rateLimiter.getMetrics().getAvailablePermissions());
            URIBuilder uriBuilder = getUriBuilder();
            String responseBody = makeRequest(title, uriBuilder);
            WikiLinksResponse response = objectMapper.readValue(responseBody, WikiLinksResponse.class);

            return parseLinks(response);
        } catch (Throwable e) {
            return null;
        }
    }

    private static String makeRequest(String title, URIBuilder uriBuilder) throws Throwable {
        return RateLimiter.decorateCheckedSupplier(rateLimiter, () -> HttpClient.newBuilder()
                .build()
                .sendAsync(HttpRequest.newBuilder()
                                .uri(URI.create(uriBuilder.addParameter("titles", title).build().toString()))
                                .GET()
                                .build(),
                        HttpResponse.BodyHandlers.ofString())
                .get()
                .body()).get();
    }

    @NotNull
    private static URIBuilder getUriBuilder() throws URISyntaxException {
        URIBuilder uriBuilder = new URIBuilder(URL);
        uriBuilder.addParameter("action", "query");
        uriBuilder.addParameter("prop", "links");
        uriBuilder.addParameter("pllimit", "max");
        uriBuilder.addParameter("format", "json");
        uriBuilder.addParameter("plnamespace", "titles");
        return uriBuilder;
    }

    @NotNull
    private static List<String> parseLinks(WikiLinksResponse response) {
        return response.getQuery().getPages().entrySet().iterator().next().getValue().getLinks()
                .stream()
                .map(Link::getTitle)
                .toList();
    }
}
