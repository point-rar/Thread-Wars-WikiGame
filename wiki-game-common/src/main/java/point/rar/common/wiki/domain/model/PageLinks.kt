package point.rar.common.wiki.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class PageLinks(
    val links: List<Link>? = emptyList()
)
