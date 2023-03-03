package point.rar

import point.rar.game.repository.WikiGame
import point.rar.game.repository.WikiGameDumbImpl

fun main(args: Array<String>) {
    val wikiGame: WikiGame = WikiGameDumbImpl()

    val path = wikiGame.play("Java (programming language)", "Philosophy", maxDepth = 4)
    println(path)
}