package point.rar.feature;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import point.rar.common.wiki.domain.model.Page;
import point.rar.common.wiki.domain.model.Link;
import point.rar.common.wiki.domain.model.WikiLinksResponse;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

public class WikiGameSerialImpl implements WikiGame {
    private static final String URL = "https://ru.wikipedia.org/w/api.php";

    @NotNull
    @Override
    public List<String> play(String startPageTitle, String endPageTitle, int maxDepth) {
        var startedPage = new Page(startPageTitle, null);
        Queue<Page> rawPages = new LinkedList<>(Collections.singleton(startedPage));
        Queue<Page> parsedPages = new LinkedList<>();
        Set<String> parsedTitle = new HashSet<>();
        var parentEndPage = startedPage;
        do {
            System.out.println("parsedPages size =  " + parsedPages.size());
            System.out.println("rawPages size =  " + rawPages.size());
            System.out.println("parsedTitle size =  " + parsedTitle.size());
            var curPage = rawPages.poll();
            parentEndPage = curPage;
            var newLinks = parseLinks(curPage.getTitle());
            parsedPages.add(curPage);
            parsedTitle.addAll(newLinks);
            rawPages.addAll(
                    newLinks.stream()
                            .map(m -> new Page(m, curPage))
                            .toList()
            );
        } while (!parsedTitle.contains(endPageTitle));
        parsedPages.add(new Page(endPageTitle, parentEndPage));

        return getResultPath(parsedPages, endPageTitle);
    }

    private List<String> getResultPath(Queue<Page> parsedPages, String endPageTitle) {
        var path = new ArrayList<String>();
        var curPage = parsedPages.stream()
                .filter(p -> p.getTitle().equals(endPageTitle))
                .findAny()
                .orElseThrow();
        while (curPage.getParentPage() != null) {
            path.add(curPage.getTitle());
            curPage = curPage.getParentPage();
        }
        path.add(curPage.getTitle());
        return path;
    }


    private List<String> parseLinks(String title) {
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
