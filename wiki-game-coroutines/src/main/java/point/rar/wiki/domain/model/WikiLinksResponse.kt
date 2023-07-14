package point.rar.wiki.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class WikiLinksResponse(
    val query: QueryLinks
)