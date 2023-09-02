package point.rar.model

import kotlinx.serialization.Serializable

@Serializable
data class QueryLinks(
    val pages: Map<String, PageLinks> = emptyMap()
)
