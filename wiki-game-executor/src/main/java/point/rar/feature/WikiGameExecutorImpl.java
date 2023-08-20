package point.rar.feature;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.RateLimiter;
import org.apache.http.client.utils.URIBuilder;
import org.jetbrains.annotations.NotNull;
import point.rar.common.wiki.domain.model.Page;
import point.rar.common.wiki.domain.model.Link;
import point.rar.common.wiki.domain.model.WikiLinksResponse;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

public class WikiGameExecutorImpl implements WikiGame {
    private static final String URL = "https://ru.wikipedia.org/w/api.php";
    private static final RateLimiter rateLimiter = RateLimiter.create(100);

    @NotNull
    @Override
    public List<String> play(@NotNull String startPageTitle, @NotNull String endPageTitle, int maxDepth) {
        var startedPage = new Page(startPageTitle, null);
        Queue<Page> rawPages = new ConcurrentLinkedQueue<>(Collections.singleton(startedPage));
        Queue<Page> parsedPages = new ConcurrentLinkedQueue<>();
        Set<String> receivedLinks = new ConcurrentSkipListSet<>();


//        ExecutorService exec = Executors.newCachedThreadPool();
//        ExecutorService exec = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 1);
        ExecutorService exec = Executors.newVirtualThreadPerTaskExecutor();

        do {
            Page curPage = rawPages.poll();
            if (curPage != null) {
                exec.execute(() -> makeSearch(curPage, rawPages, parsedPages, receivedLinks));
            }
        } while (!receivedLinks.contains(endPageTitle));
        exec.shutdown();

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

    private void makeSearch(Page curPage, Queue<Page> rawPages, Queue<Page> parsedPages, Set<String> receivedLinks) {
        List<String> newLinks = getChildLinks(curPage.getTitle());
        parsedPages.add(curPage);
        for (String link : newLinks) {
            if (receivedLinks.add(link)) {
                rawPages.add(new Page(link, curPage));
            }
        }
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


    private List<String> getChildLinks(String title) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        try {
            URIBuilder uriBuilder = new URIBuilder(URL);
            uriBuilder.addParameter("action", "query");
            uriBuilder.addParameter("prop", "links");
            uriBuilder.addParameter("pllimit", "max");
            uriBuilder.addParameter("format", "json");
            uriBuilder.addParameter("plnamespace", "titles");

            System.out.println("Get links for: " + title + ", acquire: " + rateLimiter.acquire());
            String responseBody = HttpClient.newBuilder()
                    .build()
                    .sendAsync(HttpRequest.newBuilder()
                            .uri(URI.create(uriBuilder.addParameter("titles", title).build().toString()))
                            .GET()
                            .build(), HttpResponse.BodyHandlers.ofString()).get().body();
            WikiLinksResponse response = objectMapper.readValue(responseBody, WikiLinksResponse.class);

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
