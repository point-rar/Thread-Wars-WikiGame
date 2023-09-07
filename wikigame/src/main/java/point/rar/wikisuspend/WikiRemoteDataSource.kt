package point.rar.wikisuspend

interface WikiRemoteDataSource {
    suspend fun getLinksByTitle(title: String): List<String>

    suspend fun getBacklinksByTitle(title: String): List<String>
}