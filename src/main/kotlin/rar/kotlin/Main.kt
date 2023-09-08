package rar.kotlin

import coroutines.repository.WikiGameCoroImpl
import rar.java.repository.WikiGame
import repository.WikiGameAlgoImpl

fun main(args: Array<String>) {
    val wikiGame: WikiGame = WikiGameCoroImpl()

    val start = System.nanoTime()
    val path = wikiGame.play("Бакуган", "Библия", 6)
    val timeSec = (System.nanoTime() - start) / (1_000_000_000f)
    println("$timeSec s.")
    println(path)
}

