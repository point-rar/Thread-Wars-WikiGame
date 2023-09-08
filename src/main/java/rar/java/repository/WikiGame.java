package rar.java.repository;

import java.util.List;

public interface WikiGame {
    List<String> play(String startPageTitle, String endPageTitle, int maxDepth);
}
