package rar.kotlin.repository

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import rar.kotlin.model.Page
import rar.java.repository.WikiGame
import rar.kotlin.wiki.WikiRemoteDataSource
import rar.kotlin.wiki.WikiRemoteDataSourceImpl
import java.util.concurrent.ConcurrentHashMap

class WikiGameCoroImpl : WikiGame {
    private val wikiRemoteDataSource: WikiRemoteDataSource = WikiRemoteDataSourceImpl()

    override fun play(startPageTitle: String, endPageTitle: String, maxDepth: Int): List<String> = runBlocking {
        val visitedPages: MutableMap<String, Boolean> = ConcurrentHashMap()

        val startPage = Page(startPageTitle, null)

        val resultPage = processPage(startPage, endPageTitle, 0, maxDepth, visitedPages)

        val path = mutableListOf<String>()

        var curPg: Page? = resultPage
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
    ): Page {
        if (visitedPages.putIfAbsent(page.title, true) != null) {
            throw RuntimeException("Already visited")
        }

        if (page.title == endPageTitle) {
            return page
        }

        if (curDepth == maxDepth) {
            throw RuntimeException("Depth reached")
        }

        val links = wikiRemoteDataSource.getLinksByTitle(page.title)

        val pageChannel = Channel<Page>()

        val scope = CoroutineScope(SupervisorJob() + CoroutineExceptionHandler { _, _ -> })
        links.forEach { link ->
            scope.launch {
                val pageResult = processPage(
                    Page(link, page),
                    endPageTitle,
                    curDepth + 1,
                    maxDepth,
                    visitedPages,
                )

                pageChannel.send(pageResult)
            }
        }

        val resultPage = pageChannel.receive()

        scope.cancel()

        return resultPage
    }
}