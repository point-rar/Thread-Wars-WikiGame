package rar.java.repository;

import org.jetbrains.annotations.NotNull;
import rar.java.wiki.data.source.WikiDataSource;
import rar.java.wiki.data.source.WikiDataSourceImpl;
import rar.kotlin.model.Page;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class WikiGameExecutorImpl implements WikiGame {

    private static AtomicBoolean isFinished = new AtomicBoolean(false);

    private final WikiDataSource wikiDataSource = new WikiDataSourceImpl();

    @NotNull
    @Override
    public List<String> play(@NotNull String startPageTitle, @NotNull String endPageTitle, int maxDepth) {
        var startPage = new Page(startPageTitle, null);
        Queue<Page> rawPages = new ConcurrentLinkedQueue<>(Collections.singleton(startPage));
        Queue<Page> parsedPages = new ConcurrentLinkedQueue<>();
        Set<String> receivedLinks = new ConcurrentSkipListSet<>();

        var executor = Executors.newCachedThreadPool();

        do {
            executor.execute(makeSearch(rawPages, parsedPages, receivedLinks, endPageTitle));
        } while (!isFinished.get());
        executor.shutdown();
        executor.close();

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

    public Runnable makeSearch(Queue<Page> rawPages, Queue<Page> parsedPages, Set<String> receivedLinks, String endPageTitle) {
        return () -> {
            Page currentPage = rawPages.poll();
            if (currentPage != null) {
                List<String> newLinks = wikiDataSource.getLinksByTitle(currentPage.getTitle());
                if (newLinks != null) {
                    parsedPages.add(currentPage);
                    for (String link : newLinks) {
                        if (receivedLinks.add(link)) {
                            rawPages.add(new Page(link, currentPage));
                        }
                        if (endPageTitle.equals(link)) {
                            isFinished.set(true);
                            break;
                        }
                    }
                } else {
                    rawPages.add(currentPage);
                }
            }
        };
    }

    private static List<String> getResultPath(Queue<Page> parsedPages, String endPageTitle) {
        var path = new ArrayList<String>();
        var currentPage = parsedPages.stream()
                .filter(p -> p.getTitle().equals(endPageTitle))
                .findAny()
                .orElseThrow();
        while (currentPage.getParentPage() != null) {
            currentPage = currentPage.getParentPage();
            path.add(currentPage.getTitle());
        }
        Collections.reverse(path); // Reverse the order of elements in the list
        return path;
    }
}
