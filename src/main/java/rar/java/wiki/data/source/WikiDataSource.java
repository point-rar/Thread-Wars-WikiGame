package rar.java.wiki.data.source;

import java.util.List;

public interface WikiDataSource {
    List<String> getLinksByTitle(String title);
    List<String> getBacklinksByTitle(String title);
}
