package rar.kotlin.model

import kotlinx.serialization.Serializable

@Serializable
data class QueryBacklinks(
    val backlinks: List<Link>,
)
