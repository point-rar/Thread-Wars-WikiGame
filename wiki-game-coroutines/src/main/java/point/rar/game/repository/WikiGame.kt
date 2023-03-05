package point.rar.game.repository

interface WikiGame {
    fun play(startPageTitle: String, endPageTitle: String, maxDepth: Int = 11): Result<List<String>>
}