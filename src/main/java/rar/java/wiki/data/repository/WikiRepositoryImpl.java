package rar.java.wiki.data.repository;

import rar.java.wiki.data.source.WikiDataSource;
import rar.java.wiki.domain.repository.WikiRepository;

import java.util.List;

public class WikiRepositoryImpl implements WikiRepository {

    private final WikiDataSource wikiDataSource;

    public WikiRepositoryImpl(WikiDataSource wikiDataSource) {
        this.wikiDataSource = wikiDataSource;
    }


    @Override
    public List<String> getLinksByTitle(String title) {
        return wikiDataSource.getLinksByTitle(title);
    }

    @Override
    public List<String> getBacklinksByTitle(String title) {
        return wikiDataSource.getBacklinksByTitle(title);
    }
}
