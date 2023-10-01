package rar.java.wiki.data.repository;

import rar.java.wiki.data.source.WikiRemoteDataSource;
import rar.java.wiki.domain.repository.WikiRepository;

import java.util.List;

public class WikiRepositoryImpl implements WikiRepository {

    private final WikiRemoteDataSource wikiRemoteDataSource;

    public WikiRepositoryImpl(WikiRemoteDataSource wikiRemoteDataSource) {
        this.wikiRemoteDataSource = wikiRemoteDataSource;
    }


    @Override
    public List<String> getLinksByTitle(String title) {
        return wikiRemoteDataSource.getLinksByTitle(title);
    }

    @Override
    public List<String> getBacklinksByTitle(String title) {
        return wikiRemoteDataSource.getBacklinksByTitle(title);
    }
}
