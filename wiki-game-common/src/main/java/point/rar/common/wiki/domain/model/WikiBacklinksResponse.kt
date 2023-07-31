package point.rar.common.wiki.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class WikiBacklinksResponse(
    val query: QueryBacklinks,
)
