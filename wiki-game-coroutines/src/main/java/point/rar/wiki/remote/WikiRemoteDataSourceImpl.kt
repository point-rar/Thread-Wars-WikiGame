package point.rar.wiki.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import point.rar.wiki.data.source.WikiRemoteDataSource
import point.rar.wiki.domain.model.WikiBacklinksResponse
import point.rar.wiki.domain.model.WikiLinksResponse

class WikiRemoteDataSourceImpl : WikiRemoteDataSource {
    companion object Parameter {
        val URL = "https://en.wikipedia.org/w/api.php"
    }


    private val client: HttpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }
    }

    override suspend fun getLinksByTitle(title: String): List<String> {
        val request = client.get(URL) {
            parameter("action", "query")
            parameter("titles", title)
            parameter("prop", "links")
            parameter("pllimit", "max")
            parameter("format", "json")
            parameter("plnamespace", 0)
        }

        val wikiLinksResponse: WikiLinksResponse = request.body()

        return wikiLinksResponse
            .query
            .pages
            .values
            .first()
            .links
            .map { it.title }
    }

    override suspend fun getBacklinksByTitle(title: String): List<String> {
        val request = client.get(URL) {
            parameter("action", "query")
            parameter("bltitle", title)
            parameter("list", "backlinks")
            parameter("bllimit", "max")
            parameter("format", "json")
            parameter("blnamespace", 0)
        }

        val wikiBacklinksResponse: WikiBacklinksResponse = request.body()

        return wikiBacklinksResponse
            .query
            .backlinks
            .map { it.title }
    }
}