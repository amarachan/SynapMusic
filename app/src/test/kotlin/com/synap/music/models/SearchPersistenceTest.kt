package com.synap.music.models

import org.junit.Assert.assertEquals
import org.junit.Test

class SearchPersistenceTest {

    @Test
    fun testSearchQueryModel() {
        val queryText = "test query"
        val searchQuery = SearchQuery(id = 1, query = queryText)
        
        assertEquals(1L, searchQuery.id)
        assertEquals(queryText, searchQuery.query)
    }
}
