package point.rar.model

import kotlinx.serialization.Serializable

@Serializable
data class PageLinks(
    val links: List<Link>? = emptyList()
)
