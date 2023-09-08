package rar.kotlin.model

import kotlinx.serialization.Serializable

@Serializable
data class QueryLinks(
    val pages: Map<String, PageLinks> = emptyMap()
)
