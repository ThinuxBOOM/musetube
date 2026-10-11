package com.musetube.app.searchEngine

fun interface SearchableFieldExtractor<T> {
    fun getSearchableFields(item: T): Array<String>
}