package coroutines.repository

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import rar.kotlin.model.Page
import rar.java.repository.WikiGame
import rar.kotlin.wiki.WikiRemoteDataSource
import rar.kotlin.wiki.WikiRemoteDataSourceImpl
import java.lang.RuntimeException
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap

class WikiGameCoroImpl : WikiGame {
    private val wikiRemoteDataSource: WikiRemoteDataSource = WikiRemoteDataSourceImpl()

    override fun play(startPageTitle: String, endPageTitle: String, maxDepth: Int): List<String> = runBlocking {
        val visitedPages: MutableMap<String, Boolean> = ConcurrentHashMap()
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

        return@runBlocking path.reversed()
    }

    private suspend fun processPage(
        page: Page,
        endPageTitle: String,
        curDepth: Int,
        maxDepth: Int,
        visitedPages: MutableMap<String, Boolean>,
        coroutineScope: CoroutineScope
    ): Optional<Page> {
        if (visitedPages.putIfAbsent(page.title, true) != null) {
            return Optional.empty()
        }

        if (page.title == endPageTitle) {
            return Optional.of(page)
        }

        if (curDepth == maxDepth) {
            return Optional.empty()
        }

//        println("Started for ${page.title}, depth = $curDepth")
        val links = wikiRemoteDataSource.getLinksByTitle(page.title)

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