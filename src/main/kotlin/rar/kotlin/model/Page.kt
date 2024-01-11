package rar.kotlin.model

data class Page(val title: String, val parentPage: Page?) : Comparable<Page> {
    override fun compareTo(other: Page): Int {
        return title.compareTo(other.title)
    }
}
