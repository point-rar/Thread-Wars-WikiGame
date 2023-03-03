package point.rar.wiki.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class PageLinks(
    val links: List<Link>? = null
)
