package point.rar.common.wiki.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class QueryLinks(
    val pages: Map<String, PageLinks> = emptyMap()
)
