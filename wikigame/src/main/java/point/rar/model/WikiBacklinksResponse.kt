package point.rar.model

import kotlinx.serialization.Serializable

@Serializable
data class WikiBacklinksResponse(
        val query: QueryBacklinks,
)
