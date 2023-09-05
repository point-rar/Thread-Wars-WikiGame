import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import point.rar.model.*;
import point.rar.repository.WikiGame;
import point.rar.wiki.WikiRemoteDataSource;
import point.rar.wiki.WikiRemoteDataSourceImpl;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

public class WikiGameSerialImpl implements WikiGame {
    private static final String URL = "https://ru.wikipedia.org/w/api.php";

    private final WikiRemoteDataSource wikiRemoteDataSource = new WikiRemoteDataSourceImpl();

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
            var newLinks = wikiRemoteDataSource.getLinksByTitle(curPage.getTitle());
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
}
