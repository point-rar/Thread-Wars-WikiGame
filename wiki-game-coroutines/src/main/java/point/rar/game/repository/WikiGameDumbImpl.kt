package point.rar.game.repository

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import point.rar.wiki.data.source.WikiRemoteDataSource
import point.rar.wiki.domain.model.Page
import point.rar.wiki.remote.WikiRemoteDataSourceImpl
import java.lang.RuntimeException
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.coroutineContext

class WikiGameDumbImpl : WikiGame {
    companion object {
        private const val REQUEST_SENT = 1
        private const val RESPONSE_RECEIVED = 2
    }

    private val wikiRemoteDataSource: WikiRemoteDataSource = WikiRemoteDataSourceImpl()

    override fun play(startPageTitle: String, endPageTitle: String, maxDepth: Int): Result<List<String>> = runBlocking {
        val visitedPages: MutableMap<String, Int> = ConcurrentHashMap()
        val ctx = newFixedThreadPoolContext(4, "fixed-thread-context")
        val scope = CoroutineScope(ctx)

        val startPage = Page(startPageTitle, null)

        val res = processPage(startPage, endPageTitle, 0, maxDepth, visitedPages, scope)

        ctx.cancelChildren()

        val endPage = res.getOrNull() ?: return@runBlocking Result.failure(res.exceptionOrNull()!!)
        val path = mutableListOf<String>()

        var curPg: Page? = endPage
        do {
            path.add(curPg!!.title)
            curPg = curPg.parentPage
        } while (curPg != null)

        val pagesWithResponseCount = visitedPages.entries.count { it.value == RESPONSE_RECEIVED }
        println("Received responses from $pagesWithResponseCount pages")

        return@runBlocking Result.success(path.reversed())
    }

    private suspend fun processPage(
        page: Page,
        endPageTitle: String,
        curDepth: Int,
        maxDepth: Int,
        visitedPages: MutableMap<String, Int>,
        coroutineScope: CoroutineScope
    ): Result<Page> {
        if (visitedPages.contains(page.title)) {
            return Result.success(page)
        }
        visitedPages[page.title] = REQUEST_SENT

        if (page.title == endPageTitle) {
            return Result.success(page)
        }

        if (curDepth == maxDepth) {
            return Result.failure(RuntimeException("Depth reached"))
        }

//        println("Started for ${page.title}, depth = $curDepth")
        val links = wikiRemoteDataSource.getLinksByTitle(page.title)

        visitedPages[page.title] = RESPONSE_RECEIVED

        val ch = Channel<Result<Page>>()

        links.forEach {
            // Creates new scope because we don't want to wait for all the coroutines to complete
            coroutineScope.launch {
                val coroRes = processPage(
                    Page(it, page),
                    endPageTitle,
                    curDepth + 1,
                    maxDepth,
                    visitedPages,
                    coroutineScope
                )

                ch.send(coroRes)
            }
        }

        for (i in 1..links.size) {
            val chRes = ch.receive()
            if (chRes.isSuccess) {
                return chRes
            }
        }

        return Result.failure(RuntimeException("Depth reached"))
    }
}