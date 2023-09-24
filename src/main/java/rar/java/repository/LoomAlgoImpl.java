package rar.java.repository;

import rar.java.wiki.WikiRemoteDataSource;
import rar.java.wiki.WikiRemoteDataSourceImpl;
import rar.kotlin.model.BackwardPage;
import rar.kotlin.model.ForwardPage;
import rar.kotlin.model.Page;

import java.util.*;
import java.util.concurrent.*;

public class LoomAlgoImpl implements WikiGame {

    private record PairPages(ForwardPage forwardPage, BackwardPage backwardPage) {}

    private final WikiRemoteDataSource wikiRemoteDataSource = new WikiRemoteDataSourceImpl();
    @Override
    public List<String> play(String startPageTitle, String endPageTitle, int maxDepth) {
        var pathOpt = process(startPageTitle, endPageTitle, maxDepth);
        if (pathOpt.isEmpty()) {
            throw new RuntimeException("Could not find");
        }

        return pathOpt.get();
    }

    private Optional<List<String>> process(
            String startPageTitle,
            String endPageTitle,
            int maxDepth
    ) {
        var visitedForwardPages = new ConcurrentHashMap<String, ForwardPage>();
        var visitedBackwardPages = new ConcurrentHashMap<String, BackwardPage>();

        var executor = Executors.newVirtualThreadPerTaskExecutor();

        var startForwardPage = new ForwardPage(startPageTitle, null);
        var endBackwardPage = new BackwardPage(endPageTitle, null);

        var queue = new SynchronousQueue<Optional<PairPages>>();

        executor.execute(() -> {
            var res = processPageForward(
                    startForwardPage,
                    0,
                    maxDepth,
                    visitedForwardPages,
                    visitedBackwardPages,
                    executor
            );
            try {
                queue.put(res);
            } catch (Throwable e ) {}
        });

        executor.execute(() -> {
            var res = processPageBackward(
                    endBackwardPage,
                    0,
                    maxDepth,
                    visitedForwardPages,
                    visitedBackwardPages,
                    executor
            );
            try {
                queue.put(res);
            } catch (Throwable e ) {}
        });

        for (int i = 0; i < 2; i++) {
            try {
                var res = queue.take();
                if (res.isPresent()) {
                    var pair = res.get();
                    return Optional.of(getFinalPathFromForwardAndBackward(pair.forwardPage, pair.backwardPage));
                }
            } catch (Throwable e) {}
        }

        return Optional.empty();
    }

    private Optional<PairPages> processPageForward(
            ForwardPage page,
            int curDepth,
            int maxDepth,
            ConcurrentMap<String, ForwardPage> visitedForwardPages,
            ConcurrentMap<String, BackwardPage> visitedBackwardPages,
            Executor executor
    ) {
        if (visitedForwardPages.putIfAbsent(page.getTitle(), page) != null) {
            return Optional.empty();
        }

        var backwardPage = visitedBackwardPages.get(page.getTitle());
        if (backwardPage != null) {
            return Optional.of(new PairPages(page, backwardPage));
        }

        if (curDepth == maxDepth) {
            return Optional.empty();
        }

        var links = wikiRemoteDataSource.getLinksByTitle(page.getTitle());

        var queue = new SynchronousQueue<Optional<PairPages>>();

        links.forEach((String title) -> executor.execute(() -> {
            var res = processPageForward(
                    new ForwardPage(title, page),
                    curDepth + 1,
                    maxDepth,
                    visitedForwardPages,
                    visitedBackwardPages,
                    executor
            );

            try {
                queue.put(res);
            } catch (Throwable e) {}
        }));

        for (int i = 0; i < links.size(); i++) {
            try {
                var res = queue.take();
                if (res.isPresent()) {
                    return res;
                }
            } catch(Throwable e) {}
        }

        return Optional.empty();
    }

    private Optional<PairPages> processPageBackward(
            BackwardPage page,
            int curDepth,
            int maxDepth,
            ConcurrentMap<String, ForwardPage> visitedForwardPages,
            ConcurrentMap<String, BackwardPage> visitedBackwardPages,
            Executor executor
    ) {
        if (visitedBackwardPages.putIfAbsent(page.getTitle(), page) != null) {
            return Optional.empty();
        }

        var forwardPage = visitedForwardPages.get(page.getTitle());
        if (forwardPage != null) {
            return Optional.of(new PairPages(forwardPage, page));
        }

        if (curDepth == maxDepth) {
            return Optional.empty();
        }

        var backlinks = wikiRemoteDataSource.getBacklinksByTitle(page.getTitle());

        var queue = new SynchronousQueue<Optional<PairPages>>();

        backlinks.forEach((String title) -> executor.execute(() -> {
            var res = processPageBackward(
                    new BackwardPage(title, page),
                    curDepth + 1,
                    maxDepth,
                    visitedForwardPages,
                    visitedBackwardPages,
                    executor
            );

            try {
                queue.put(res);
            } catch (Throwable e) {}
        }));

        for (int i = 0; i < backlinks.size(); i++) {
            try {
                var res = queue.take();
                if (res.isPresent()) {
                    return res;
                }
            } catch(Throwable e) {}
        }

        return Optional.empty();
    }

    private List<String> getFinalPathFromForwardAndBackward(
            ForwardPage forwardPage,
            BackwardPage backwardPage
    ) {
        var path = new ArrayList<String>();

        var forwardPages = new ArrayList<ForwardPage>();
        var curFwdPage = forwardPage;
        while (curFwdPage != null) {
            forwardPages.add(curFwdPage);
            curFwdPage = curFwdPage.getParentPage();
        }

        Collections.reverse(forwardPages);
        for (var fwdPg : forwardPages) {
            path.add(fwdPg.getTitle());
        }

        var curBwdPage = backwardPage.getChildPage();
        while (curBwdPage != null) {
            path.add(curBwdPage.getTitle());
            curBwdPage = curBwdPage.getChildPage();
        }

        return path;
    }

}
