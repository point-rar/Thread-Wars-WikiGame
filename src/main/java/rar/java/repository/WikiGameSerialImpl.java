package rar.java.repository;

import org.jetbrains.annotations.NotNull;
import rar.java.wiki.data.source.WikiDataSource;
import rar.java.wiki.data.source.WikiDataSourceImpl;
import rar.kotlin.model.Page;

import java.util.*;

public class WikiGameSerialImpl implements WikiGame {
    private static final String URL = "https://ru.wikipedia.org/w/api.php";

    private final WikiDataSource wikiDataSource = new WikiDataSourceImpl();

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
            var newLinks = wikiDataSource.getLinksByTitle(curPage.getTitle());
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
