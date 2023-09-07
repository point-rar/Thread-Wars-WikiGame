package model

data class BackwardPage(val title: String, val childPage: BackwardPage?)
