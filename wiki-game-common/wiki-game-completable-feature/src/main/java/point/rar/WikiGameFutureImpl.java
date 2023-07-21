package point.rar;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import point.rar.wiki.domain.model.Link;
import point.rar.wiki.domain.model.Page;
import point.rar.wiki.domain.model.WikiLinksResponse;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.*;

public class WikiGameFutureImpl implements WikiGame {
    private static final String URL = "https://ru.wikipedia.org/w/api.php";

    @NotNull
    @Override
    public List<String> play(String startPageTitle, String endPageTitle, int maxDepth) {
        var startedPage = new Page(startPageTitle, null);
        Queue<Page> rawPages = new ConcurrentLinkedQueue<>(Collections.singleton(startedPage));
        Queue<Page> parsedPages = new ConcurrentLinkedQueue<>();
        Set<String> parsedTitle = new ConcurrentSkipListSet<>();

        ExecutorService executorService = Executors.newCachedThreadPool();
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        do {
            for (int i = 0; i < 100; i++) {
                Page curPage = rawPages.poll();
                if (curPage == null) {
                    break;
                }
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> makeSearch(curPage, rawPages, parsedPages, parsedTitle), executorService);
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

    private void makeSearch(Page curPage, Queue<Page> rawPages, Queue<Page> parsedPages, Set<String> parsedTitle) {
        List<String> newLinks = getChildLinks(curPage.getTitle());
        parsedPages.add(curPage);
        parsedTitle.addAll(newLinks);
        rawPages.addAll(newLinks.stream()
                .map(m -> new Page(m, curPage))
                .toList());
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
            System.out.println("Get links for: " + title);
            String responseBody = HttpClient.newBuilder()
                    .build()
                    .send(HttpRequest.newBuilder()
                            .uri(URI.create(URL + "?action=query&prop=links&pllimit=max&format=json&plnamespace=0&titles=" + title.replace(" ", "%20")))
                            .GET()
                            .build(), HttpResponse.BodyHandlers.ofString()).body();
            WikiLinksResponse response = objectMapper.readValue(responseBody, WikiLinksResponse.class);

            return parseLinks(response);
        } catch (IOException | InterruptedException e) {
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
