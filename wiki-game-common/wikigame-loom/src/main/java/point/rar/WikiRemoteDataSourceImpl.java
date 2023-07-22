package point.rar;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.utils.URIBuilder;
import point.rar.wiki.data.source.WikiRemoteDataSource;
import point.rar.wiki.domain.model.Link;
import point.rar.wiki.domain.model.WikiBacklinksResponse;
import point.rar.wiki.domain.model.WikiLinksResponse;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public class WikiRemoteDataSourceImpl {

    private static final String URL = "https://ru.wikipedia.org/w/api.php";
    private final HttpClient client = HttpClient.newHttpClient();

    private final ObjectMapper objectMapper = new ObjectMapper();

    public WikiRemoteDataSourceImpl() {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    List<String> getLinksByTitle(String title) throws URISyntaxException, IOException, InterruptedException {
        URI uri = new URIBuilder(URL)
                .addParameter("action", "query")
                .addParameter("titles", title)
                .addParameter("prop", "links")
                .addParameter("pllimit", "max")
                .addParameter("format", "json")
                .addParameter("plnamespace", "0")
                .build();

        HttpRequest req = HttpRequest.newBuilder(uri).GET().build();

        String body = client.send(req, HttpResponse.BodyHandlers.ofString()).body();

        WikiLinksResponse response = objectMapper.readValue(body, WikiLinksResponse.class);

        return parseLinks(response);
    }

    List<String> getBacklinksByTitle(String title) throws URISyntaxException, IOException, InterruptedException {
        URI uri = new URIBuilder(URL)
                .addParameter("action", "query")
                .addParameter("bltitle", title)
                .addParameter("list", "backlinks")
                .addParameter("bllimit", "max")
                .addParameter("format", "json")
                .addParameter("blnamespace", "0")
                .build();

        HttpRequest req = HttpRequest.newBuilder(uri).GET().build();

        String body = client.send(req, HttpResponse.BodyHandlers.ofString()).body();

        WikiBacklinksResponse response = objectMapper.readValue(body, WikiBacklinksResponse.class);

        return response.getQuery().getBacklinks().stream().map(Link::getTitle).toList();
    }

    private static List<String> parseLinks(WikiLinksResponse response) {
        return response.getQuery().getPages().entrySet().iterator().next().getValue().getLinks()
                .stream()
                .map(Link::getTitle)
                .toList();
    }
}
