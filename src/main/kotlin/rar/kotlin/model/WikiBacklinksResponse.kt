package rar.kotlin.model

import kotlinx.serialization.Serializable

@Serializable
data class WikiBacklinksResponse(
    val query: QueryBacklinks = QueryBacklinks(emptyList()),
)
