package rar.java.wiki.domain.repository;

import java.util.List;

public interface WikiRepository {
    List<String> getLinksByTitle(String title);
    List<String> getBacklinksByTitle(String title);
}
