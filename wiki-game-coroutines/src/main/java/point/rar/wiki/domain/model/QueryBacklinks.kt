package point.rar.wiki.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class QueryBacklinks(
    val backlinks: List<Link>,
)
