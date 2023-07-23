package point.rar;

import point.rar.wiki.domain.model.Page;

import java.util.*;
import java.util.concurrent.*;

public class WikiGameLoomImpl implements WikiGame {


    private final Integer REQUEST_SENT = 1;
    private final Integer RESPONSE_RECEIVED = 2;

    private WikiRemoteDataSourceImpl wikiRemoteDataSource = new WikiRemoteDataSourceImpl();

    @Override
    public List<String> play(String startPageTitle, String endPageTitle, int maxDepth) {
        Map<String, Integer> visitedPages = new ConcurrentHashMap<>();
        var executor = Executors.newVirtualThreadPerTaskExecutor();

        Page startPage = new Page(startPageTitle, null);
        Page endPage;

        try {
            endPage = executor.submit(() ->
                    processPage(startPage, endPageTitle, 0, maxDepth, visitedPages, executor)
            ).get().orElseThrow();
        } catch (Throwable e) {
            throw new RuntimeException(e.getMessage());
        }

        List<String> path = List.of();
        Page curPg = endPage;
        do {
            path.add(curPg.getTitle());
            curPg = curPg.getParentPage();
        } while (curPg != null);
    }

    Optional<Page> processPage(
       Page page,
       String endPageTitle,
       Integer curDepth,
       Integer maxDepth,
       Map<String, Integer> visitedPages,
       ExecutorService executor
    ) {
        if (visitedPages.containsKey(page.getTitle())) {
            return Optional.empty();
        }
        visitedPages.put(page.getTitle(), REQUEST_SENT);

        if (page.getTitle().equals(endPageTitle)) {
            return Optional.of(page);
        }

        if (curDepth == maxDepth) {
            return Optional.empty();
        }


    }
}
