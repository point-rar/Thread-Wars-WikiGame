package point.rar.wiki;

import java.util.List;

public interface WikiRemoteDataSource {
    List<String> getLinksByTitle(String title);
    List<String> getBacklinksByTitle(String title);
}
