package rar.java.repository;

import jdk.incubator.concurrent.StructuredTaskScope;
import rar.java.wiki.WikiRemoteDataSource;
import rar.java.wiki.WikiRemoteDataSourceImpl;
import rar.kotlin.model.Page;

import java.util.*;
import java.util.concurrent.*;

public class LoomImpl implements WikiGame {

    private final WikiRemoteDataSource wikiRemoteDataSource = new WikiRemoteDataSourceImpl();

    @Override
    public List<String> play(String startPageTitle, String endPageTitle, int maxDepth) {
        var visitedPages = new ConcurrentHashMap<String, Boolean>();

        var startPage = new Page(startPageTitle, null);

        Page resultPage = processPage(startPage, endPageTitle, 0, maxDepth, visitedPages);

        var path = new ArrayList<String>();

        var curPg = resultPage;
        do {
            path.add(curPg.getTitle());
            curPg = curPg.getParentPage();
        } while (curPg != null);

        Collections.reverse(path);
        return path;
    }

    private Page processPage(
        Page page,
        String endPageTitle,
        int curDepth,
        int maxDepth,
        Map<String, Boolean> visitedPages
    ) {
        if (visitedPages.putIfAbsent(page.getTitle(), true) != null) {
            throw new RuntimeException("Already visited");
        }

        if (page.getTitle().equals(endPageTitle)) {
            return page;
        }

        if (curDepth == maxDepth) {
            throw new RuntimeException("Depth reached");
        }

        var links = wikiRemoteDataSource.getLinksByTitle(page.getTitle());

        try (var scope = new StructuredTaskScope.ShutdownOnSuccess<Page>()) {
            links.forEach((link) -> {
                scope.fork(() -> processPage(
                        new Page(link, page),
                        endPageTitle,
                        curDepth + 1,
                        maxDepth,
                        visitedPages
                    )
                );
            });

            scope.join();

            return scope.result();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
