package rar.java.repository;

import rar.java.wiki.WikiRemoteDataSource;
import rar.java.wiki.WikiRemoteDataSourceImpl;
import rar.kotlin.model.BackwardPage;
import rar.kotlin.model.ForwardPage;
import rar.kotlin.model.Page;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;

public class LoomAlgoImpl implements WikiGame {

    private record PairPages(ForwardPage forwardPage, BackwardPage backwardPage) {}

    private final WikiRemoteDataSource wikiRemoteDataSource = new WikiRemoteDataSourceImpl();
    @Override
    public List<String> play(String startPageTitle, String endPageTitle, int maxDepth) {
        var visitedForwardPages = new ConcurrentHashMap();
        var visitedBackwardPages = new ConcurrentHashMap();

        var executor = Executors.newVirtualThreadPerTaskExecutor();

        var startForwardPage = new ForwardPage(startPageTitle, null);
        var endBackwardPage = new BackwardPage(endPageTitle, null);

        var queue = new SynchronousQueue<Optional<PairPages>>();

        executor.execute(() -> {
            var res = ;
            try {
                queue.put(res);
            } catch (Throwable e ) {}
        });

        executor.execute(() -> {
            try {
                queue.put(res);
            } catch (Throwable e ) {}
        });

        for (int i = 0; i < 2; i++) {
            try {
                var res = queue.take();
                if (res.isPresent()) {
                    var pair = res.get();
                }
            } catch (Throwable e) {}
        }

        return pathOpt.get();
    }

    private Optional<List<String>> process(
            String startPageTitle,
            String endPageTitle,
            int maxDepth
    ) {

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
