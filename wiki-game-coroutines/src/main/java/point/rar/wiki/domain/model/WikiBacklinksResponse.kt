package point.rar.wiki.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class WikiBacklinksResponse(
    val query: QueryBacklinks,
)