package rar.java;

import rar.java.wiki.data.repository.WikiRepositoryImpl;
import rar.java.wiki.data.source.WikiRemoteDataSource;
import rar.java.wiki.domain.repository.WikiRepository;
import rar.java.wiki.remote.WikiRemoteDataSourceImpl;
import rar.java.repository.*;

public class Main {
    public static void main(String[] args) {
        WikiRemoteDataSource wikiRemoteDataSource = new WikiRemoteDataSourceImpl();
        WikiRepository wikiRepository = new WikiRepositoryImpl(wikiRemoteDataSource);

        long startTime = System.currentTimeMillis();

        WikiGame wikiGame = new LoomImpl(wikiRepository);
//        var path = wikiGame.play("Алгебра", "Ятаган", 6);
//        var path = wikiGame.play("!!!", "Теория гомологий", 6);
        var path = wikiGame.play("Бакуган", "Библия", 6);

        long endTime = System.currentTimeMillis();

        System.out.println(path);
        System.out.println("Total execution time: " + (endTime - startTime) + "ms");
    }
}