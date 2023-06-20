package point.rar.game.repository

import io.github.resilience4j.kotlin.ratelimiter.executeSuspendFunction
import io.github.resilience4j.ratelimiter.RateLimiterConfig
import io.github.resilience4j.ratelimiter.RateLimiterRegistry
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import point.rar.wiki.data.source.WikiRemoteDataSource
import point.rar.wiki.domain.model.Page
import point.rar.wiki.remote.WikiRemoteDataSourceImpl
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.coroutineContext

class WikiGameDumbImpl : WikiGame {
    companion object {
        private const val REQUEST_SENT = 1
        private const val RESPONSE_RECEIVED = 2
    }


    private val rateLimiterConfig = RateLimiterConfig
            .custom()
            .limitForPeriod(10)
            .limitRefreshPeriod(Duration.ofMillis(60))
            .timeoutDuration(Duration.ofDays(10000))
            .build()

    private val rateLimiterRegistry = RateLimiterRegistry.of(rateLimiterConfig)
    private val rateLimiter = rateLimiterRegistry.rateLimiter("rate limiter")

    private val wikiRemoteDataSource: WikiRemoteDataSource = WikiRemoteDataSourceImpl()

    override fun play(startPageTitle: String, endPageTitle: String, maxDepth: Int): Result<List<String>> = runBlocking {
        val visitedPages: MutableMap<String, Int> = ConcurrentHashMap()
        val resChannel = Channel<Result<Page>>()
        val ctx = newFixedThreadPoolContext(4, "fixed-thread-context")
        val scope = CoroutineScope(ctx)

        val startPage = Page(startPageTitle, null)
        scope.launch {
            processPage(startPage, endPageTitle, 0, maxDepth, visitedPages, resChannel, scope)
        }

        val resPage = resChannel.receive()
        ctx.cancelChildren()

        val endPage = resPage.getOrNull() ?: return@runBlocking Result.failure(resPage.exceptionOrNull()!!)
        val path = mutableListOf<String>()

        var curPg: Page? = endPage
        do {
            path.add(curPg!!.title)
            curPg = curPg!!.parentPage
        } while (curPg != null)

        val pagesWithResponseCount = visitedPages.entries.filter { it.value == RESPONSE_RECEIVED }.count()
        println("Received responses from $pagesWithResponseCount pages")

        return@runBlocking Result.success(path.reversed())
    }

    private suspend fun processPage(
        page: Page,
        endPageTitle: String,
        curDepth: Int,
        maxDepth: Int,
        visitedPages: MutableMap<String, Int>,
        resChannel: Channel<Result<Page>>,
        coroutineScope: CoroutineScope
    ) {
        if (visitedPages.contains(page.title)) {
            return
        }
        visitedPages[page.title] = REQUEST_SENT

        if (page.title == endPageTitle) {
            resChannel.send(Result.success(page))
            return
        }

        if (curDepth == maxDepth) {
//            resChannel.send(Result.failure(RuntimeException("Depth is reached")))
            return
        }

//        println("Started for ${page.title}, depth = $curDepth")
        val links = wikiRemoteDataSource.getLinksByTitle(page.title)

        visitedPages[page.title] = RESPONSE_RECEIVED

        links.forEach {
            // Creates new scope because we don't want to wait for all the coroutines to complete
            rateLimiter.executeSuspendFunction {
                coroutineScope.launch {
                    processPage(
                            Page(it, page),
                            endPageTitle,
                            curDepth + 1,
                            maxDepth,
                            visitedPages,
                            resChannel,
                            coroutineScope
                    )
                }
            }
        }

    }
}