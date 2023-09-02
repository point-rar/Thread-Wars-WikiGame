package point.rar.model

import kotlinx.serialization.Serializable

@Serializable
data class WikiLinksResponse(
    val query: QueryLinks = QueryLinks(emptyMap())
)