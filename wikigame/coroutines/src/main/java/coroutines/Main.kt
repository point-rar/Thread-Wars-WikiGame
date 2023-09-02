package coroutines

import coroutines.repository.WikiGame
import coroutines.repository.WikiGameDumbImpl

fun main(args: Array<String>) {
    val wikiGame: WikiGame = WikiGameDumbImpl()

    val start = System.nanoTime()
    val path = wikiGame.play("Бакуган", "Библия", maxDepth = 6)
    val timeSec = (System.nanoTime() - start) / (1_000_000_000f)
    println("$timeSec s.")
    println(path)
}