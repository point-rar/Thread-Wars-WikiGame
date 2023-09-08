package rar.kotlin.wiki

interface WikiRemoteDataSource {
    suspend fun getLinksByTitle(title: String): List<String>

    suspend fun getBacklinksByTitle(title: String): List<String>
}