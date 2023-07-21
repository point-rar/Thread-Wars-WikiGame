package point.rar.game.repository

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import point.rar.wiki.data.source.WikiRemoteDataSource
import point.rar.wiki.domain.model.Page
import point.rar.wiki.remote.WikiRemoteDataSourceImpl
import java.lang.RuntimeException
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap

class WikiGameDumbImpl : WikiGame {
    companion object {
        private const val REQUEST_SENT = 1
        private const val RESPONSE_RECEIVED = 2
    }

    private val wikiRemoteDataSource: WikiRemoteDataSource = WikiRemoteDataSourceImpl()

    override fun play(startPageTitle: String, endPageTitle: String, maxDepth: Int): List<String> = runBlocking {
        val visitedPages: MutableMap<String, Int> = ConcurrentHashMap()
        val ctx = newFixedThreadPoolContext(4, "fixed-thread-context")
        val scope = CoroutineScope(ctx)

        val startPage = Page(startPageTitle, null)

        val res = processPage(startPage, endPageTitle, 0, maxDepth, visitedPages, scope)
        if (res.isEmpty) {
            throw RuntimeException("Depth reached")
        }

        ctx.cancelChildren()

        val endPage = res.get()
        val path = mutableListOf<String>()

        var curPg: Page? = endPage
        do {
            path.add(curPg!!.title)
            curPg = curPg.parentPage
        } while (curPg != null)

        val pagesWithResponseCount = visitedPages.entries.count { it.value == RESPONSE_RECEIVED }
        println("Received responses from $pagesWithResponseCount pages")

        return@runBlocking path.reversed()
    }

    private suspend fun processPage(
        page: Page,
        endPageTitle: String,
        curDepth: Int,
        maxDepth: Int,
        visitedPages: MutableMap<String, Int>,
        coroutineScope: CoroutineScope
    ): Optional<Page> {
        if (visitedPages.contains(page.title)) {
            return Optional.empty()
        }
        visitedPages[page.title] = REQUEST_SENT

        if (page.title == endPageTitle) {
            return Optional.of(page)
        }

        if (curDepth == maxDepth) {
            return Optional.empty()
        }

//        println("Started for ${page.title}, depth = $curDepth")
        val links = wikiRemoteDataSource.getLinksByTitle(page.title)

        visitedPages[page.title] = RESPONSE_RECEIVED

        val ch = Channel<Optional<Page>>()

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
            if (chRes.isPresent) {
                return chRes
            }
        }

        return Optional.empty()
    }
}