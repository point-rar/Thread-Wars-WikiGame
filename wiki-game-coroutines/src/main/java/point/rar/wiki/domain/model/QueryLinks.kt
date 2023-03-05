package point.rar.wiki.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class QueryLinks(
    val pages: Map<String, PageLinks>
)
