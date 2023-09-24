package rar.java.repository;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.apache.http.client.utils.URIBuilder;
import org.jetbrains.annotations.NotNull;
import rar.kotlin.model.Link;
import rar.kotlin.model.Page;
import rar.kotlin.model.WikiLinksResponse;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

public class WikiGameFutureSimpleImpl implements WikiGame {
    private static final String URL = "https://ru.wikipedia.org/w/api.php";

    @NotNull
    @Override
    public List<String> play(String startPageTitle, String endPageTitle, int maxDepth) {
        var startedPage = new Page(startPageTitle, null);
        Queue<Page> rawPages = new ConcurrentLinkedQueue<>(Collections.singleton(startedPage));
        Queue<Page> parsedPages = new ConcurrentLinkedQueue<>();
        Set<String> parsedTitle = new ConcurrentSkipListSet<>();
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofMillis(1000))
                .limitForPeriod(300)
                .timeoutDuration(Duration.ofMillis(25))
                .build();
        RateLimiterRegistry rateLimiterRegistry = RateLimiterRegistry.of(config);
        RateLimiter rateLimiter = rateLimiterRegistry
                .rateLimiter("rateLimiter", config);


        ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();
        do {
            for (int i = 0; i < 1000; i++) {
                Page curPage = rawPages.poll();
                if (curPage == null) {
                    break;
                }
                Runnable restrictedCall = RateLimiter
                        .decorateRunnable(rateLimiter, makeSearch(curPage, rawPages, parsedPages, parsedTitle));
                CompletableFuture<Void> future =CompletableFuture.runAsync(restrictedCall);
                futures.add(future);
            }
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            futures.clear();
        } while (!parsedTitle.contains(endPageTitle));
        parsedPages.add(
                new Page(endPageTitle,
                        rawPages.stream()
                                .filter(p -> p.getTitle().equals(endPageTitle))
                                .findAny()
                                .orElseThrow()
                )
        );

        executorService.shutdown();
        return getResultPath(parsedPages, endPageTitle);
    }

    private Runnable makeSearch(Page curPage, Queue<Page> rawPages, Queue<Page> parsedPages, Set<String> parsedTitle) {
        return () -> {
            List<String> newLinks = getChildLinks(curPage.getTitle());
            parsedPages.add(curPage);
            parsedTitle.addAll(newLinks);
            rawPages.addAll(newLinks.stream()
                    .map(m -> new Page(m, curPage))
                    .toList());
        };
    }

    private List<String> getResultPath(Queue<Page> parsedPages, String endPageTitle) {
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


        String responseBody = null;
        try {
            URIBuilder uriBuilder = new URIBuilder(URL);
            uriBuilder.addParameter("action", "query");
            uriBuilder.addParameter("prop", "links");
            uriBuilder.addParameter("pllimit", "max");
            uriBuilder.addParameter("format", "json");
            uriBuilder.addParameter("plnamespace", "titles");

            System.out.println("Get links for: " + title);
            responseBody = HttpClient.newBuilder()
                    .build()
                    .sendAsync(HttpRequest.newBuilder()
                            .uri(URI.create(uriBuilder.addParameter("titles", title).build().toString()))
                            .GET()
                            .build(), HttpResponse.BodyHandlers.ofString()).get().body();
            var response = objectMapper.readValue(responseBody, WikiLinksResponse.class);

            return parseLinks(response);
        } catch (IOException | InterruptedException | URISyntaxException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    private static List<String> parseLinks(WikiLinksResponse response) {
        return response.getQuery().getPages().entrySet().iterator().next().getValue().getLinks()
                .stream()
                .map(Link::getTitle)
                .toList();
    }
}
