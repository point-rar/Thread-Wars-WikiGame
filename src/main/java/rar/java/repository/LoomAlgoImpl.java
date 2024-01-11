package rar.java.repository;

import jdk.incubator.concurrent.StructuredTaskScope;
import rar.java.wiki.domain.repository.WikiRepository;
import rar.kotlin.model.BackwardPage;
import rar.kotlin.model.ForwardPage;

import java.util.*;
import java.util.concurrent.*;

public class LoomAlgoImpl implements WikiGame {

    private record PairPages(ForwardPage forwardPage, BackwardPage backwardPage) {
    }

    private final WikiRepository wikiRepository;

    public LoomAlgoImpl(WikiRepository wikiRepository) {
        this.wikiRepository = wikiRepository;
    }

    @Override
    public List<String> play(
        String startPageTitle,
        String endPageTitle,
        int maxDepth
    ) {
        var visitedForwardPages = new ConcurrentHashMap<String, ForwardPage>();
        var visitedBackwardPages = new ConcurrentHashMap<String, BackwardPage>();

        var startForwardPage = new ForwardPage(startPageTitle, null);
        var endBackwardPage = new BackwardPage(endPageTitle, null);

        try (var scope = new StructuredTaskScope.ShutdownOnSuccess<PairPages>()) {
            scope.fork(() -> processPageForward(
                startForwardPage,
                0,
                maxDepth,
                visitedForwardPages,
                visitedBackwardPages
            ));

            scope.fork(() -> processPageBackward(
                endBackwardPage,
                0,
                maxDepth,
                visitedForwardPages,
                visitedBackwardPages
            ));

            scope.join();

            var pairPagesResult = scope.result();

            return getFinalPathFromForwardAndBackward(pairPagesResult.forwardPage, pairPagesResult.backwardPage);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private PairPages processPageForward(
        ForwardPage page,
        int curDepth,
        int maxDepth,
        ConcurrentMap<String, ForwardPage> visitedForwardPages,
        ConcurrentMap<String, BackwardPage> visitedBackwardPages
    ) {
        if (visitedForwardPages.putIfAbsent(page.getTitle(), page) != null) {
            throw new RuntimeException("Already visited");
        }

        var backwardPage = visitedBackwardPages.get(page.getTitle());
        if (backwardPage != null) {
            return new PairPages(page, backwardPage);
        }

        if (curDepth == maxDepth) {
            throw new RuntimeException("Depth reached");
        }

        var links = wikiRepository.getLinksByTitle(page.getTitle());

        try (var scope = new StructuredTaskScope.ShutdownOnSuccess<PairPages>()) {
            links.forEach((link) -> {
                scope.fork(() -> processPageForward(
                        new ForwardPage(link, page),
                        curDepth + 1,
                        maxDepth,
                        visitedForwardPages,
                        visitedBackwardPages
                    )
                );
            });

            scope.join();

            return scope.result();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private PairPages processPageBackward(
        BackwardPage page,
        int curDepth,
        int maxDepth,
        ConcurrentMap<String, ForwardPage> visitedForwardPages,
        ConcurrentMap<String, BackwardPage> visitedBackwardPages
    ) {
        if (visitedBackwardPages.putIfAbsent(page.getTitle(), page) != null) {
            throw new RuntimeException("Already visited");
        }

        var forwardPage = visitedForwardPages.get(page.getTitle());
        if (forwardPage != null) {
            return new PairPages(forwardPage, page);
        }

        if (curDepth == maxDepth) {
            throw new RuntimeException("Depth reached");
        }

        var backlinks = wikiRepository.getBacklinksByTitle(page.getTitle());

        try (var scope = new StructuredTaskScope.ShutdownOnSuccess<PairPages>()) {
            backlinks.forEach((link) -> {
                scope.fork(() -> processPageBackward(
                    new BackwardPage(link, page),
                    curDepth + 1,
                    maxDepth,
                    visitedForwardPages,
                    visitedBackwardPages
                ));
            });

            scope.join();

            return scope.result();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
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
