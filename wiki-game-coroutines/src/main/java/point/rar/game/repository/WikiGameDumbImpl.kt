package point.rar.game.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import point.rar.wiki.data.source.WikiRemoteDataSource
import point.rar.wiki.domain.model.Page
import point.rar.wiki.remote.WikiRemoteDataSourceImpl
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.coroutineContext

class WikiGameDumbImpl : WikiGame {
    private val wikiRemoteDataSource: WikiRemoteDataSource = WikiRemoteDataSourceImpl()

    override fun play(startPageTitle: String, endPageTitle: String, maxDepth: Int): Result<List<String>> = runBlocking {
        val visitedPages: Set<String> = ConcurrentHashMap.newKeySet()
        val resChannel = Channel<Result<Page>>()
        val scope = CoroutineScope(coroutineContext)

        val startPage = Page(startPageTitle, null)
        scope.launch {
            processPage(startPage, endPageTitle, 0, maxDepth, visitedPages, resChannel, scope)
        }

        val resPage = resChannel.receive()
        coroutineContext.cancelChildren()

        val endPage = resPage.getOrNull() ?: return@runBlocking Result.failure(resPage.exceptionOrNull()!!)
        val path = mutableListOf<String>()

        var curPg = endPage
        while (curPg.parentPage != null) {
            path.add(curPg.title)
            curPg = curPg.parentPage!!
        }
        path.add(startPage.title)

        return@runBlocking Result.success(path.reversed())
    }

    private suspend fun processPage(
        page: Page,
        endPageTitle: String,
        curDepth: Int,
        maxDepth: Int,
        visitedPages: Set<String>,
        resChannel: Channel<Result<Page>>,
        coroutineScope: CoroutineScope
    ) {
        if (visitedPages.contains(page.title)) {
            return
        }

        if (page.title == endPageTitle) {
            resChannel.send(Result.success(page))
            return
        }

        if (curDepth == maxDepth) {
            resChannel.send(Result.failure(RuntimeException("Depth is reached")))
            return
        }

        println("Started for ${page.title}, depth = $curDepth")
        val links = wikiRemoteDataSource.getLinksByTitle(page.title)
        links.forEach {
            // Creates new scope because we don't want to wait for all the coroutines to complete
            coroutineScope.launch {
                processPage(Page(it, page), endPageTitle, curDepth + 1, maxDepth, visitedPages, resChannel, coroutineScope)
            }
        }

    }
}