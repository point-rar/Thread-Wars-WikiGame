package rar.java;

import rar.java.wiki.data.repository.WikiRepositoryImpl;
import rar.java.wiki.data.source.WikiMySqlDataSourceImpl;
import rar.java.wiki.data.source.WikiDataSource;
import rar.java.wiki.domain.repository.WikiRepository;
import rar.java.repository.*;

public class Main {
    public static void main(String[] args) {
        WikiDataSource wikiDataSource = new WikiMySqlDataSourceImpl();
        WikiRepository wikiRepository = new WikiRepositoryImpl(wikiDataSource);

        WikiGame wikiGame = new ReactorAlgoWikiGameImpl(wikiRepository);

        long startTime = System.currentTimeMillis();
        var path = wikiGame.play("Охотники_за_привидениями", "Пуджа", 6);
        long endTime = System.currentTimeMillis();

        System.out.println(path);
        System.out.println("Total execution time: " + (endTime - startTime) + "ms");
    }
}