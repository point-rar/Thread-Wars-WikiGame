package coroutines.wiki.data.source

interface WikiRemoteDataSource {
    suspend fun getLinksByTitle(title: String): List<String>

    suspend fun getBacklinksByTitle(title: String): List<String>
}