package point.rar.coroutines.repository

interface WikiGame {
    fun play(startPageTitle: String, endPageTitle: String, maxDepth: Int = 11): List<String>
}