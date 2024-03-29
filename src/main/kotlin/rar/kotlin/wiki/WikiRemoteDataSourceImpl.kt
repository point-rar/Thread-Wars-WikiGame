package rar.kotlin.wiki

import io.github.resilience4j.kotlin.ratelimiter.executeSuspendFunction
import io.github.resilience4j.ratelimiter.RateLimiterConfig
import io.github.resilience4j.ratelimiter.RateLimiterRegistry
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import rar.kotlin.model.WikiBacklinksResponse
import rar.kotlin.model.WikiLinksResponse
import java.time.Duration

class WikiRemoteDataSourceImpl : WikiRemoteDataSource {
    companion object Parameter {
        const val URL = "https://ru.wikipedia.org/w/api.php"
    }

    private val rateLimiterConfig = RateLimiterConfig
        .custom()
        .limitForPeriod(1)
        .limitRefreshPeriod(Duration.ofMillis(40))
        .timeoutDuration(Duration.ofDays(10000))
        .build()

    private val rateLimiterRegistry = RateLimiterRegistry.of(rateLimiterConfig)
    private val rateLimiter = rateLimiterRegistry.rateLimiter("rate limiter")

    private val client: HttpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })

        }
    }

    override suspend fun getLinksByTitle(title: String): List<String> {
        val response = rateLimiter.executeSuspendFunction {
            client.get(URL) {
                parameter("action", "query")
                parameter("titles", title)
                parameter("prop", "links")
                parameter("pllimit", "max")
                parameter("format", "json")
                parameter("plnamespace", 0)
            }
        }

        val wikiLinksResponse: WikiLinksResponse = response.body()

        val links = wikiLinksResponse
            .query
            .pages
            .values
            .first()
            .links
            ?.map { it.title } ?: emptyList()

        return links
    }

    override suspend fun getBacklinksByTitle(title: String): List<String> {
        val response = rateLimiter.executeSuspendFunction {
            client.get(URL) {
                parameter("action", "query")
                parameter("bltitle", title)
                parameter("list", "backlinks")
                parameter("bllimit", "max")
                parameter("format", "json")
                parameter("blnamespace", 0)
            }
        }

        val wikiBacklinksResponse: WikiBacklinksResponse = response.body()

        return wikiBacklinksResponse
            .query
            .backlinks
            .map { it.title }
    }
}