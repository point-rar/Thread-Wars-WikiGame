package rar.kotlin.model

import kotlinx.serialization.Serializable

@Serializable
data class PageLinks(
    val links: List<Link>? = emptyList()
)
