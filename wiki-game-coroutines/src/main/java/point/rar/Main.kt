package point.rar

import point.rar.wiki.data.source.WikiRemoteDataSource
import point.rar.wiki.remote.WikiRemoteDataSourceImpl

suspend fun main(args: Array<String>) {
    val wikiRemoteDataSource: WikiRemoteDataSource = WikiRemoteDataSourceImpl()

    println(
        wikiRemoteDataSource.getLinksByTitle("Coroutine")
    )
}