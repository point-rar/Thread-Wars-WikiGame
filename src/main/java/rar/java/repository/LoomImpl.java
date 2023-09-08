package rar.java.repository;

import rar.java.wiki.WikiRemoteDataSource;
import rar.java.wiki.WikiRemoteDataSourceImpl;
import rar.kotlin.model.Page;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;

public class LoomImpl implements WikiGame {

    private final WikiRemoteDataSource wikiRemoteDataSource = new WikiRemoteDataSourceImpl();
    @Override
    public List<String> play(String startPageTitle, String endPageTitle, int maxDepth) {
        var visitedPages = new ConcurrentHashMap<String, Boolean>();
        var executor = Executors.newVirtualThreadPerTaskExecutor();

        var startPage = new Page(startPageTitle, null);

        var res = processPage(startPage, endPageTitle, 0, maxDepth, visitedPages, executor);
        if (res.isEmpty()) {
            throw new RuntimeException("Depth reached");
        }

        executor.shutdown();

        var endPage = res.get();
        var path = new ArrayList<String>();

        var curPg = endPage;
        do {
            path.add(curPg.getTitle());
            curPg = curPg.getParentPage();
        } while (curPg != null);

        Collections.reverse(path);
        return path;
    }

    private Optional<Page> processPage(
            Page page,
            String endPageTitle,
            int curDepth,
            int maxDepth,
            Map<String, Boolean> visitedPages,
            Executor executor
    ) {
        if (visitedPages.putIfAbsent(page.getTitle(), true) != null) {
            return Optional.empty();
        }

        if (page.getTitle().equals(endPageTitle)) {
            return Optional.of(page);
        }

        if (curDepth == maxDepth) {
            return Optional.empty();
        }

        var links = wikiRemoteDataSource.getLinksByTitle(page.getTitle());

//        System.out.println("got response from " + page.getTitle());

        var queue = new SynchronousQueue<Optional<Page>>();

        links.forEach((link) -> {
            executor.execute(() -> {
                var res = processPage(
                        new Page(link, page),
                        endPageTitle,
                        curDepth+1,
                        maxDepth,
                        visitedPages,
                        executor
                );

                try {
                    queue.put(res);
                } catch (Throwable e) {}
            });
        });

        for (int i = 0; i < links.size(); i++) {
            try {
                var res = queue.take();
                if (res.isPresent()) {
                    return res;
                }
            } catch (Throwable e) {
                return Optional.empty();
            }
        }

        return Optional.empty();
    }
}
