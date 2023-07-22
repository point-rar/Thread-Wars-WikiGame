package point.rar;

import point.rar.wiki.domain.model.Page;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class WikiGameLoomImpl implements WikiGame {

    @Override
    public List<String> play(String startPageTitle, String endPageTitle, int maxDepth) {
        Map<String, Integer> visitedPages = new ConcurrentHashMap<>();
    }

    Optional<Page> processPage(
       Page page,
       String endPageTitle,
       Integer curDepth,
       Integer maxDepth,
       Map<String, Integer> visitedPages
    ) {}
}
