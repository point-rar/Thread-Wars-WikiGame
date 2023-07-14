package point.rar

import point.rar.game.repository.WikiGame
import point.rar.game.repository.WikiGameDumbImpl

fun main(args: Array<String>) {
    val wikiGame: WikiGame = WikiGameDumbImpl()

    val start = System.nanoTime()
    val path = wikiGame.play("Бакуган", "Библия", maxDepth = 6)
    println((System.nanoTime() - start) / 1_000_000)
    println(path)
}