import org.jetbrains.annotations.NotNull;
import point.rar.model.*;
import point.rar.repository.WikiGame;
import point.rar.wiki.WikiRemoteDataSource;
import point.rar.wiki.WikiRemoteDataSourceImpl;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class WikiGameExecutorImpl implements WikiGame {

    private static AtomicBoolean isFinished = new AtomicBoolean(false);

    private final WikiRemoteDataSource wikiRemoteDataSource = new WikiRemoteDataSourceImpl();

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

    public Runnable makeSearch(Queue<Page> rawPages, Queue<Page> parsedPages, Set<String> receivedLinks, String endPageTitle) {
        return () -> {
            Page curPage = rawPages.poll();
            if (curPage != null) {
                List<String> newLinks = wikiRemoteDataSource.getLinksByTitle(curPage.getTitle());
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
}
